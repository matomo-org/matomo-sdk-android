/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher

import org.matomo.sdk.Matomo.Companion.tag
import org.matomo.sdk.TrackMe
import org.matomo.sdk.tools.Connectivity
import timber.log.Timber
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile
import kotlin.math.min


/**
 * Responsible for transmitting packets to a server
 */
class DefaultDispatcher(
    private val eventCache: EventCache,
    private val connectivity: Connectivity,
    private val packetFactory: PacketFactory,
    private val packetSender: PacketSender,
    callback: ((Exception) -> Unit)?// = null
) : Dispatcher {

    private val threadControl = Any()
    private val sleepToken = Semaphore(0)

    @Volatile
    private var timeOut = Dispatcher.DEFAULT_CONNECTION_TIMEOUT

    @Volatile
    private var dispatchInterval = Dispatcher.DEFAULT_DISPATCH_INTERVAL

    @Volatile
    private var retryCounter = 0

    @Volatile
    private var forcedBlocking = false

    private var dispatchGzipped = false

    @Volatile
    private var dispatchMode = DispatchMode.ALWAYS

    @Volatile
    private var running = false

    @Volatile
    private var dispatchThread: Thread? = null
    private var mDryRunTarget: MutableList<Packet>? = null

    init {
        packetSender.setGzipData(dispatchGzipped)
        packetSender.setTimeout(timeOut.toLong())
    }

    /**
     * Connection timeout in milliseconds
     *
     * @return timeout in milliseconds
     */
    override fun getConnectionTimeOut(): Int {
        return timeOut
    }

    /**
     * Timeout when trying to establish connection and when trying to read a response.
     * Values take effect on next dispatch.
     *
     * @param timeOutIn timeout in milliseconds
     */
    override fun setConnectionTimeOut(timeOutIn: Int) {
        timeOut = timeOutIn
        packetSender.setTimeout(timeOut.toLong())
    }

    /**
     * Packets are collected and dispatched in batches, this intervals sets the pause between batches.
     *
     * @param dispatchIntervalIn in milliseconds
     */
    override fun setDispatchInterval(dispatchIntervalIn: Long) {
        dispatchInterval = dispatchIntervalIn
        if (dispatchInterval != -1L)
            launch()
    }

    override fun getDispatchInterval(): Long {
        return dispatchInterval
    }

    /**
     * Packets are collected and dispatched in batches. This boolean sets if post must be
     * gzipped or not. Use of gzip needs mod_deflate/Apache ou lua_zlib/NGINX
     *
     * @param dispatchGzippedIn boolean
     */
    override fun setDispatchGzipped(dispatchGzippedIn: Boolean) {
        dispatchGzipped = dispatchGzippedIn
        packetSender.setGzipData(dispatchGzipped)
    }

    override fun getDispatchGzipped(): Boolean {
        return dispatchGzipped
    }

    override fun setDispatchMode(dispatchModeIn: DispatchMode) {
        this.dispatchMode = dispatchModeIn
    }

    override fun getDispatchMode(): DispatchMode {
        return dispatchMode
    }

    private fun launch(): Boolean {
        synchronized(threadControl) {
            if (!running) {
                running = true
                val thread = Thread(loop)
                thread.priority = Thread.MIN_PRIORITY
                thread.name = "Matomo-default-dispatcher"
                dispatchThread = thread
                thread.start()
                return true
            }
        }
        return false
    }

    /**
     * Starts the dispatcher for one cycle if it is currently not working.
     * If the dispatcher is working it will skip the dispatch interval once.
     */
    override fun forceDispatch(): Boolean {
        if (!launch()) {
            retryCounter = 0
            sleepToken.release()
            return false
        }
        return true
    }

    override fun forceDispatchBlocking() {
        synchronized(threadControl) {
            // force thread to exit after it completes its dispatch loop
            forcedBlocking = true
        }

        if (forceDispatch()) {
            sleepToken.release()
        }

        val dispatchThreadLocal = dispatchThread

        if (dispatchThreadLocal != null) {
            try {
                dispatchThreadLocal.join()
            } catch (e: InterruptedException) {
                Timber.tag(TAG).d("Interrupted while waiting for dispatch thread to complete")
            }
        }

        synchronized(threadControl) {
            // re-enable default behavior
            forcedBlocking = false
        }
    }

    override fun clear() {
        eventCache.clear()
        // Try to exit the loop as the queue is empty
        if (running) forceDispatch()
    }

    override fun submit(trackMe: TrackMe) {
        eventCache.add(Event(trackMe.toMap()))
        if (dispatchInterval != -1L) launch()
    }

    private val loop: Runnable = Runnable {
        retryCounter = 0
        while (running) {
            try {
                var sleepTime = dispatchInterval
                if (retryCounter > 1)
                    sleepTime += min(
                        (retryCounter * dispatchInterval).toDouble(),
                        (5 * dispatchInterval).toDouble()
                    ).toLong()

                // Either we wait the interval or forceDispatch() granted us one free pass
                sleepToken.tryAcquire(sleepTime, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Timber.tag(TAG).e(e)
            }
            if (eventCache.updateState(isOnline)) {
                var count = 0
                val drainedEvents: List<Event> = ArrayList()
                eventCache.drainTo(drainedEvents)
                Timber.tag(TAG).d("Drained %s events.", drainedEvents.size)
                for (packet in packetFactory.buildPackets(drainedEvents)) {
                    var success: Boolean
                    var resultException: Exception? = null
                    if (mDryRunTarget != null) {
                        Timber.tag(TAG).d("DryRun, stored HttpRequest, now %d.", mDryRunTarget!!.size)
                        success = mDryRunTarget!!.add(packet)
                    } else {
                        resultException = packetSender.send(packet)
                        success = resultException == null
                    }

                    if (success) {
                        count += packet.eventCount
                        retryCounter = 0
                    } else {
                        // On network failure, requeue all un-sent events, but use isOnline to determine if events should be cached in
                        // memory or disk
                        Timber.tag(TAG).d("Failure while trying to send packet")
                        retryCounter++
                        callback?.let { resultException?.let { exception -> it(exception) } }
                        break
                    }

                    // Re-check network connectivity to early exit if we drop offline.  This speeds up how quickly the setOffline method will
                    // take effect
                    if (!isOnline) {
                        Timber.tag(TAG).d("Disconnected during dispatch loop")
                        break
                    }
                }

                Timber.tag(TAG).d("Dispatched %d events.", count)
                if (count < drainedEvents.size) {
                    Timber.tag(TAG).d("Unable to send all events, re-queueing %d events", drainedEvents.size - count)
                    // Requeue events to the event cache that weren't processed (either PacketSender failure or we are now offline).  Once the
                    // events are re-queued we update the event cache state to write the re-queued events to disk or to leave them in memory
                    // depending on the connectivity state of the device.
                    eventCache.requeue(drainedEvents.subList(count, drainedEvents.size))
                    eventCache.updateState(isOnline)
                }
            }

            synchronized(threadControl) {
                // We may be done or this was a forced dispatch.  If we are in a blocking force dispatch we need to exit immediately to ensure
                // the blocking doesn't take too long.
                if (forcedBlocking || eventCache.isEmpty || dispatchInterval < 0) {
                    running = false
                }
            }
        }
    }

    private val isOnline: Boolean
        get() {
            if (!connectivity.isConnected) return false

            return when (dispatchMode) {
                DispatchMode.EXCEPTION -> false
                DispatchMode.ALWAYS -> true
                DispatchMode.WIFI_ONLY -> connectivity.type == Connectivity.Type.WIFI
            }
        }

    override fun setDryRunTarget(dryRunTarget: MutableList<Packet>) {
        mDryRunTarget = dryRunTarget
    }

    override fun getDryRunTarget(): List<Packet> {
        return mDryRunTarget!!
    }

    companion object {
        private val TAG = tag(DefaultDispatcher::class.java)
    }
}
