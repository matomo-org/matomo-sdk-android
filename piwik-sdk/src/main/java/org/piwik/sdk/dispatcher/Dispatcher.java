/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.dispatcher;

import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;
import org.piwik.sdk.Piwik;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Sends json POST request to tracking url http://piwik.example.com/piwik.php with body
 * <p/>
 * {
 * "requests": [
 * "?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
 * "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"
 * ],
 * "token_auth": "33dc3f2536d3025974cccb4b4d2d98f4"
 * }
 */
@SuppressWarnings("deprecation")
public class Dispatcher {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "Dispatcher";
    private final BlockingQueue<String> mDispatchQueue = new LinkedBlockingQueue<>();
    private final Object mThreadControl = new Object();
    private final Semaphore mSleepToken = new Semaphore(0);
    private final Piwik mPiwik;
    private final URL mApiUrl;
    private final String mAuthToken;

    private List<Packet> mDryRunOutput = Collections.synchronizedList(new ArrayList<Packet>());
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;  // 5s
    private volatile int mTimeOut = DEFAULT_CONNECTION_TIMEOUT;
    private volatile boolean mRunning = false;

    public static final long DEFAULT_DISPATCH_INTERVAL = 120 * 1000; // 120s
    private volatile long mDispatchInterval = DEFAULT_DISPATCH_INTERVAL;

    public Dispatcher(Piwik piwik, URL apiUrl, String authToken) {
        mPiwik = piwik;
        mApiUrl = apiUrl;
        mAuthToken = authToken;
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
        if (mDispatchInterval != -1)
            launch();
    }

    public long getDispatchInterval() {
        return mDispatchInterval;
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
        mDispatchQueue.add(query);
        if (mDispatchInterval != -1)
            launch();
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

                int count = 0;
                List<String> availableEvents = new ArrayList<>();
                mDispatchQueue.drainTo(availableEvents);
                Timber.tag(LOGGER_TAG).d("Drained %s events.", availableEvents.size());
                TrackerBulkURLWrapper wrapper = new TrackerBulkURLWrapper(mApiUrl, availableEvents, mAuthToken);
                Iterator<TrackerBulkURLWrapper.Page> pageIterator = wrapper.iterator();
                while (pageIterator.hasNext()) {
                    TrackerBulkURLWrapper.Page page = pageIterator.next();

                    // use doGET when only event on current page
                    if (page.elementsCount() > 1) {
                        JSONObject eventData = wrapper.getEvents(page);
                        if (eventData == null)
                            continue;
                        if (dispatch(new Packet(wrapper.getApiUrl(), eventData)))
                            count += page.elementsCount();
                    } else {
                        URL targetURL = wrapper.getEventUrl(page);
                        if (targetURL == null)
                            continue;
                        if (dispatch(new Packet(targetURL)))
                            count += 1;
                    }
                }
                Timber.tag(LOGGER_TAG).d("Dispatched %s events.", count);
                synchronized (mThreadControl) {
                    // We may be done or this was a forced dispatch
                    if (mDispatchQueue.isEmpty() || mDispatchInterval < 0) {
                        mRunning = false;
                        break;
                    }
                }
            }
        }
    };

    @VisibleForTesting
    public boolean dispatch(@NonNull Packet packet) {
        // Some error checking
        if (packet.getTargetURL() == null)
            return false;
        if (packet.getJSONObject() != null && packet.getJSONObject().length() == 0)
            return false;

        if (mPiwik.isDryRun()) {
            mDryRunOutput.add(packet);
            Timber.tag(LOGGER_TAG).d("DryRun, stored HttpRequest, now %s.", mDryRunOutput.size());
            return true;
        }

        if (!mDryRunOutput.isEmpty())
            mDryRunOutput.clear();

        try {
            HttpURLConnection urlConnection = (HttpURLConnection) packet.getTargetURL().openConnection();
            urlConnection.setConnectTimeout(mTimeOut);
            urlConnection.setReadTimeout(mTimeOut);

            // IF there is json data we want to do a post
            if (packet.getJSONObject() != null) {
                // POST
                urlConnection.setDoOutput(true); // Forces post
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("charset", "utf-8");

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(packet.getJSONObject().toString());
                writer.flush();
                writer.close();
            } else {
                // GET
                urlConnection.setDoOutput(false); // Defaults to false, but for readability
            }

            int statusCode = urlConnection.getResponseCode();
            Timber.tag(LOGGER_TAG).d("status code %s", statusCode);
            return statusCode == HttpURLConnection.HTTP_NO_CONTENT || statusCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            // Broad but an analytics app shouldn't impact it's host app.
            Timber.tag(LOGGER_TAG).w(e, "Cannot send request");
        }
        return false;
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
     * For bulk tracking purposes
     *
     * @param map query map
     * @return String "?idsite=1&url=http://example.org&action_name=Test bulk log view&rec=1"
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
