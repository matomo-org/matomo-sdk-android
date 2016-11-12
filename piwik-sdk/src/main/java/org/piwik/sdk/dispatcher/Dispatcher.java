/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.dispatcher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final Piwik mPiwik;
    private final ConnectivityManager mConnectivityManager;

    private List<Packet> mDryRunOutput = Collections.synchronizedList(new ArrayList<Packet>());
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;  // 5s
    private volatile int mTimeOut = DEFAULT_CONNECTION_TIMEOUT;
    private volatile boolean mRunning = false;

    public static final long DEFAULT_DISPATCH_INTERVAL = 120 * 1000; // 120s
    private volatile long mDispatchInterval = DEFAULT_DISPATCH_INTERVAL;
    private boolean mDispatchGzipped = false;
    private final PacketFactory packetFactory;

    public Dispatcher(Tracker tracker, EventCache eventCache) {
        mPiwik = tracker.getPiwik();
        mConnectivityManager = (ConnectivityManager) mPiwik.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        packetFactory = new PacketFactory(tracker.getAPIUrl(), tracker.getAuthToken());
        mEventCache = eventCache;
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

    private boolean launch() {
        synchronized (mThreadControl) {
            if (!mRunning) {
                mRunning = true;
                new Thread(mLoop).start();
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

    public void submit(String query) {
        mEventCache.add(query);
        if (mDispatchInterval != -1) launch();
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            while (mRunning) {
                try {
                    // Either we wait the interval or forceDispatch() granted us one free pass
                    mSleepToken.tryAcquire(mDispatchInterval, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean connected = isConnected();
                mEventCache.updateState(connected);
                if (connected) {
                    int count = 0;
                    List<String> drainedEvents = new ArrayList<>();
                    mEventCache.drain(drainedEvents);
                    Timber.tag(LOGGER_TAG).d("Drained %s events.", drainedEvents.size());
                    for (Packet packet : packetFactory.buildPackets(drainedEvents)) {
                        try {
                            if (dispatch(packet)) count += packet.getEventCount();
                        } catch (IOException e) {
                            // While rapidly dispatching, it's possible that we are connected, but can't resolve hostnames yet
                            // java.net.UnknownHostException: Unable to resolve host "...": No address associated with hostname
                            Timber.tag(LOGGER_TAG).w(e, "Dispatch failed, assuming OFFLINE, requeuing events.");
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
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * @param packet to dispatch
     * @return true if dispatch was successful
     */
    @VisibleForTesting
    public boolean dispatch(@NonNull Packet packet) throws IOException {
        if (mPiwik.isDryRun()) {
            mDryRunOutput.add(packet);
            Timber.tag(LOGGER_TAG).d("DryRun, stored HttpRequest, now %s.", mDryRunOutput.size());
            return true;
        }

        if (!mDryRunOutput.isEmpty()) mDryRunOutput.clear();

        HttpURLConnection urlConnection = (HttpURLConnection) packet.openConnection();
        urlConnection.setConnectTimeout(mTimeOut);
        urlConnection.setReadTimeout(mTimeOut);

        // IF there is json data we want to do a post
        if (packet.getPostData() != null) {
            // POST
            urlConnection.setDoOutput(true); // Forces post
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("charset", "utf-8");

            String toPost = packet.getPostData().toString();
            if (mDispatchGzipped) {
                urlConnection.addRequestProperty("Content-Encoding", "gzip");
                ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOS);
                gzipOutputStream.write(toPost.getBytes(Charset.forName("UTF8")));
                gzipOutputStream.close();
                urlConnection.getOutputStream().write(byteArrayOS.toByteArray());
            } else {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(toPost);
                writer.flush();
                writer.close();
            }

        } else {
            // GET
            urlConnection.setDoOutput(false); // Defaults to false, but for readability
        }

        int statusCode = urlConnection.getResponseCode();
        Timber.tag(LOGGER_TAG).d("status code %s", statusCode);
        return statusCode == HttpURLConnection.HTTP_NO_CONTENT || statusCode == HttpURLConnection.HTTP_OK;

    }

    /**
     * http://stackoverflow.com/q/4737841
     *
     * @param param raw data
     * @return encoded string
     */
    public static String urlEncodeUTF8(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            Timber.tag(LOGGER_TAG).e(e, "Cannot encode %s", param);
            return "";
        } catch (NullPointerException e) {
            return "";
        }
    }

    /**
     * URL encodes a key-value map
     */
    public static String urlEncodeUTF8(Map<String, String> map) {
        StringBuilder sb = new StringBuilder(100);
        sb.append('?');
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(urlEncodeUTF8(entry.getKey()));
            sb.append('=');
            sb.append(urlEncodeUTF8(entry.getValue()));
            sb.append('&');
        }

        return sb.substring(0, sb.length() - 1);
    }

    public List<Packet> getDryRunOutput() {
        return mDryRunOutput;
    }

}
