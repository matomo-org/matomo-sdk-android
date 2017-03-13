/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.dispatcher;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.tools.Connectivity;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import timber.log.Timber;

/**
 * Responsible for transmitting packets to a server
 */
public class Dispatcher {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "Dispatcher";
    private final Object mThreadControl = new Object();
    private final EventCache mEventCache;
    private final Semaphore mSleepToken = new Semaphore(0);
    private final Connectivity mConnectivity;
    private final PacketFactory mPacketFactory;

    static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;  // 5s
    private volatile int mTimeOut = DEFAULT_CONNECTION_TIMEOUT;

    static final long DEFAULT_DISPATCH_INTERVAL = 120 * 1000; // 120s
    private volatile long mDispatchInterval = DEFAULT_DISPATCH_INTERVAL;

    private boolean mDispatchGzipped = false;
    private DispatchMode mDispatchMode = DispatchMode.ALWAYS;
    private volatile boolean mRunning = false;
    private List<Packet> mDryRunTarget = null;

    public Dispatcher(EventCache eventCache, Connectivity connectivity, PacketFactory packetFactory) {
        mConnectivity = connectivity;
        mEventCache = eventCache;
        mPacketFactory = packetFactory;
    }

    /**
     * Connection timeout in milliseconds
     *
     * @return timeout in milliseconds
     */
    public int getConnectionTimeOut() {
        return mTimeOut;
    }

    /**
     * Timeout when trying to establish connection and when trying to read a response.
     * Values take effect on next dispatch.
     *
     * @param timeOut timeout in milliseconds
     */
    public void setConnectionTimeOut(int timeOut) {
        mTimeOut = timeOut;
    }

    /**
     * Packets are collected and dispatched in batches, this intervals sets the pause between batches.
     *
     * @param dispatchInterval in milliseconds
     */
    public void setDispatchInterval(long dispatchInterval) {
        mDispatchInterval = dispatchInterval;
        if (mDispatchInterval != -1) launch();
    }

    public long getDispatchInterval() {
        return mDispatchInterval;
    }

    /**
     * Packets are collected and dispatched in batches. This boolean sets if post must be
     * gzipped or not. Use of gzip needs mod_deflate/Apache ou lua_zlib/NGINX
     *
     * @param dispatchGzipped boolean
     */
    public void setDispatchGzipped(boolean dispatchGzipped) {
        mDispatchGzipped = dispatchGzipped;
    }

    public boolean getDispatchGzipped() {
        return mDispatchGzipped;
    }

    public void setDispatchMode(DispatchMode dispatchMode) {
        this.mDispatchMode = dispatchMode;
    }

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
    public boolean forceDispatch() {
        if (!launch()) {
            mSleepToken.release();
            return false;
        }
        return true;
    }

    public void clear() {
        mEventCache.clear();
        // Try to exit the loop as the queue is empty
        if (mRunning) forceDispatch();
    }

    public void submit(TrackMe trackMe) {
        mEventCache.add(new Event(trackMe.toMap()));
        if (mDispatchInterval != -1) launch();
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            while (mRunning) {
                try {
                    // Either we wait the interval or forceDispatch() granted us one free pass
                    mSleepToken.tryAcquire(mDispatchInterval, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {Timber.tag(LOGGER_TAG).e(e); }

                if (mEventCache.updateState(isConnected())) {
                    int count = 0;
                    List<Event> drainedEvents = new ArrayList<>();
                    mEventCache.drainTo(drainedEvents);
                    Timber.tag(LOGGER_TAG).d("Drained %s events.", drainedEvents.size());
                    for (Packet packet : mPacketFactory.buildPackets(drainedEvents)) {
                        boolean success = false;
                        try {
                            success = dispatch(packet);
                        } catch (IOException e) {
                            // While rapidly dispatching, it's possible that we are connected, but can't resolve hostnames yet
                            // java.net.UnknownHostException: Unable to resolve host "...": No address associated with hostname
                            Timber.tag(LOGGER_TAG).d(e);
                        }
                        if (success) {
                            count += packet.getEventCount();
                        } else {
                            Timber.tag(LOGGER_TAG).d("Unsuccesful assuming OFFLINE, requeuing events.");
                            mEventCache.updateState(false);
                            mEventCache.requeue(drainedEvents.subList(count, drainedEvents.size()));
                            break;
                        }
                    }
                    Timber.tag(LOGGER_TAG).d("Dispatched %d events.", count);
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

    /**
     * @param packet to dispatch
     * @return true if dispatch was successful
     */
    @VisibleForTesting
    public boolean dispatch(@NonNull Packet packet) throws IOException {
        if (mDryRunTarget != null) {
            mDryRunTarget.add(packet);
            Timber.tag(LOGGER_TAG).d("DryRun, stored HttpRequest, now %s.", mDryRunTarget.size());
            return true;
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) packet.openConnection();
            urlConnection.setConnectTimeout(mTimeOut);
            urlConnection.setReadTimeout(mTimeOut);

            // IF there is json data we have to do a post
            if (packet.getPostData() != null) { // POST
                urlConnection.setDoOutput(true); // Forces post
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("charset", "utf-8");

                final String toPost = packet.getPostData().toString();
                if (mDispatchGzipped) {
                    urlConnection.addRequestProperty("Content-Encoding", "gzip");
                    ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

                    GZIPOutputStream gzipStream = null;
                    try {
                        gzipStream = new GZIPOutputStream(byteArrayOS);
                        gzipStream.write(toPost.getBytes(Charset.forName("UTF8")));
                    } finally { if (gzipStream != null) gzipStream.close();}

                    urlConnection.getOutputStream().write(byteArrayOS.toByteArray());
                } else {
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                        writer.write(toPost);
                    } finally { if (writer != null) writer.close(); }
                }

            } else { // GET
                urlConnection.setDoOutput(false); // Defaults to false, but for readability
            }

            int statusCode = urlConnection.getResponseCode();
            Timber.tag(LOGGER_TAG).d("status code %s", statusCode);

            return checkResponseCode(statusCode);
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
    }

    private boolean checkResponseCode(int code) {
        return code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_OK;
    }

    public void setDryRunTarget(List<Packet> dryRunTarget) {
        mDryRunTarget = dryRunTarget;
    }

    public List<Packet> getDryRunTarget() {
        return mDryRunTarget;
    }
}
