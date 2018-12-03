/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.dispatcher;

import org.matomo.sdk.Matomo;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.tools.Connectivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Responsible for transmitting packets to a server
 */
public class DefaultDispatcher implements Dispatcher {
    private static final String TAG = Matomo.tag(DefaultDispatcher.class);
    private final Object mThreadControl = new Object();
    private final EventCache mEventCache;
    private final Semaphore mSleepToken = new Semaphore(0);
    private final Connectivity mConnectivity;
    private final PacketFactory mPacketFactory;
    private final PacketSender mPacketSender;
    private volatile int mTimeOut = DEFAULT_CONNECTION_TIMEOUT;
    private volatile long mDispatchInterval = DEFAULT_DISPATCH_INTERVAL;
    private volatile int mRetryCounter = 0;

    private boolean mDispatchGzipped = false;
    private DispatchMode mDispatchMode = DispatchMode.ALWAYS;
    private volatile boolean mRunning = false;
    private List<Packet> mDryRunTarget = null;

    public DefaultDispatcher(EventCache eventCache, Connectivity connectivity, PacketFactory packetFactory, PacketSender packetSender) {
        mConnectivity = connectivity;
        mEventCache = eventCache;
        mPacketFactory = packetFactory;
        mPacketSender = packetSender;
        packetSender.setGzipData(mDispatchGzipped);
        packetSender.setTimeout(mTimeOut);
    }

    /**
     * Connection timeout in milliseconds
     *
     * @return timeout in milliseconds
     */
    @Override
    public int getConnectionTimeOut() {
        return mTimeOut;
    }

    /**
     * Timeout when trying to establish connection and when trying to read a response.
     * Values take effect on next dispatch.
     *
     * @param timeOut timeout in milliseconds
     */
    @Override
    public void setConnectionTimeOut(int timeOut) {
        mTimeOut = timeOut;
        mPacketSender.setTimeout(mTimeOut);
    }

    /**
     * Packets are collected and dispatched in batches, this intervals sets the pause between batches.
     *
     * @param dispatchInterval in milliseconds
     */
    @Override
    public void setDispatchInterval(long dispatchInterval) {
        mDispatchInterval = dispatchInterval;
        if (mDispatchInterval != -1) launch();
    }

    @Override
    public long getDispatchInterval() {
        return mDispatchInterval;
    }

    /**
     * Packets are collected and dispatched in batches. This boolean sets if post must be
     * gzipped or not. Use of gzip needs mod_deflate/Apache ou lua_zlib/NGINX
     *
     * @param dispatchGzipped boolean
     */
    @Override
    public void setDispatchGzipped(boolean dispatchGzipped) {
        mDispatchGzipped = dispatchGzipped;
        mPacketSender.setGzipData(mDispatchGzipped);
    }

    @Override
    public boolean getDispatchGzipped() {
        return mDispatchGzipped;
    }

    @Override
    public void setDispatchMode(DispatchMode dispatchMode) {
        this.mDispatchMode = dispatchMode;
    }

    @Override
    public DispatchMode getDispatchMode() {
        return mDispatchMode;
    }

    private boolean launch() {
        synchronized (mThreadControl) {
            if (!mRunning) {
                mRunning = true;
                Thread thread = new Thread(mLoop);
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.start();
                return true;
            }
        }
        return false;
    }

    /**
     * Starts the dispatcher for one cycle if it is currently not working.
     * If the dispatcher is working it will skip the dispatch interval once.
     */
    @Override
    public boolean forceDispatch() {
        if (!launch()) {
            mRetryCounter = 0;
            mSleepToken.release();
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        mEventCache.clear();
        // Try to exit the loop as the queue is empty
        if (mRunning) forceDispatch();
    }

    @Override
    public void submit(TrackMe trackMe) {
        mEventCache.add(new Event(trackMe.toMap()));
        if (mDispatchInterval != -1) launch();
    }

    private final Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            mRetryCounter = 0;
            while (mRunning) {
                try {
                    long sleepTime = mDispatchInterval;
                    if (mRetryCounter > 1) sleepTime += Math.min(mRetryCounter * mDispatchInterval, 5 * mDispatchInterval);

                    // Either we wait the interval or forceDispatch() granted us one free pass
                    mSleepToken.tryAcquire(sleepTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {Timber.tag(TAG).e(e); }
                if (mEventCache.updateState(isConnected())) {
                    int count = 0;
                    List<Event> drainedEvents = new ArrayList<>();
                    mEventCache.drainTo(drainedEvents);
                    Timber.tag(TAG).d("Drained %s events.", drainedEvents.size());
                    for (Packet packet : mPacketFactory.buildPackets(drainedEvents)) {
                        boolean success;

                        if (mDryRunTarget != null) {
                            Timber.tag(TAG).d("DryRun, stored HttpRequest, now %d.", mDryRunTarget.size());
                            success = mDryRunTarget.add(packet);
                        } else {
                            success = mPacketSender.send(packet);
                        }

                        if (success) {
                            count += packet.getEventCount();
                            mRetryCounter = 0;
                        } else {
                            Timber.tag(TAG).d("Unsuccesful assuming OFFLINE, requeuing events.");
                            mEventCache.updateState(false);
                            mEventCache.requeue(drainedEvents.subList(count, drainedEvents.size()));
                            mRetryCounter++;
                            break;
                        }
                    }
                    Timber.tag(TAG).d("Dispatched %d events.", count);
                }
                synchronized (mThreadControl) {
                    // We may be done or this was a forced dispatch
                    if (mEventCache.isEmpty() || mDispatchInterval < 0) {
                        mRunning = false;
                        break;
                    }
                }
            }
        }
    };

    private boolean isConnected() {
        if (!mConnectivity.isConnected()) return false;

        switch (mDispatchMode) {
            case ALWAYS:
                return true;
            case WIFI_ONLY:
                return mConnectivity.getType() == Connectivity.Type.WIFI;
            default:
                return false;
        }
    }

    @Override
    public void setDryRunTarget(List<Packet> dryRunTarget) {
        mDryRunTarget = dryRunTarget;
    }

    @Override
    public List<Packet> getDryRunTarget() {
        return mDryRunTarget;
    }
}
