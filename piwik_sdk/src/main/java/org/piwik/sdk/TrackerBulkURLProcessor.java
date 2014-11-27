/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;


import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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
public class TrackerBulkURLProcessor extends AsyncTask<TrackerBulkURLWrapper, Integer, Integer> {

    private final int timeout;
    private boolean dryRun = false;
    private final Dispatchable<Integer> dispatchable;

    public TrackerBulkURLProcessor(final Dispatchable<Integer> tracker, int timeout) {
        dispatchable = tracker;
        this.timeout = timeout;
    }

    public TrackerBulkURLProcessor(final Dispatchable<Integer> tracker, int timeout, boolean dryRun) {
        this(tracker, timeout);
        this.dryRun = dryRun;
    }

    protected Integer doInBackground(TrackerBulkURLWrapper... wrappers) {
        int count = 0;

        for (TrackerBulkURLWrapper wrapper : wrappers) {
            Iterator<TrackerBulkURLWrapper.Page> pageIterator = wrapper.iterator();
            TrackerBulkURLWrapper.Page page;

            while (pageIterator.hasNext()) {
                page = pageIterator.next();

                // use doGET when only event on current page
                if (page.elementsCount() > 1) {
                    if (doPost(wrapper.getApiUrl(), wrapper.getEvents(page))) {
                        count += page.elementsCount();
                    }
                } else {
                    if (doGet(wrapper.getEventUrl(page))) {
                        count += 1;
                    }
                }
                if (isCancelled()) break;
            }
        }

        return count;
    }

    protected void onPostExecute(Integer count) {
        dispatchable.dispatchingCompleted(count);
    }

    public void processBulkURLs(URL apiUrl, List<String> events, String authToken) {
        dispatchable.dispatchingStarted();
        executeAsyncTask(this, new TrackerBulkURLWrapper(apiUrl, events, authToken));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

    public boolean doGet(String trackingEndPointUrl) {
        if (trackingEndPointUrl == null) {
            return false;
        }

        HttpGet get = new HttpGet(trackingEndPointUrl);

        return doRequest(get, null);
    }

    public boolean doPost(URL url, JSONObject json) {
        if (url == null || json == null) {
            return false;
        }

        String jsonBody = json.toString();

        try {
            HttpPost post = new HttpPost(url.toURI());
            StringEntity se = new StringEntity(jsonBody);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);

            return doRequest(post, jsonBody);
        } catch (URISyntaxException e) {
            Log.w(Tracker.LOGGER_TAG, String.format("URI Syntax Error %s", url.toString()), e);
        } catch (UnsupportedEncodingException e) {
            Log.w(Tracker.LOGGER_TAG, String.format("Unsupported Encoding %s", jsonBody), e);
        }

        return false;
    }

    private boolean doRequest(HttpRequestBase requestBase, String body) {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), timeout * 1000);
        HttpResponse response;

        if (dryRun) {
            Log.i(Tracker.LOGGER_TAG, "Request wasn't send due to dry run is on");
            logRequest(requestBase, body);
        } else {
            try {
                response = client.execute(requestBase);
                int statusCode = response.getStatusLine().getStatusCode();
                Log.d(Tracker.LOGGER_TAG, String.format("status code %s", statusCode));
                return statusCode == HttpStatus.SC_NO_CONTENT || statusCode == HttpStatus.SC_OK;

            } catch (ClientProtocolException e) {
                Log.w(Tracker.LOGGER_TAG, "Cannot send request", e);
            } catch (IOException e) {
                Log.w(Tracker.LOGGER_TAG, "Cannot send request", e);
            }

            logRequest(requestBase, body);
        }

        return false;
    }

    private void logRequest(HttpRequestBase requestBase, String body) {
        Log.i(Tracker.LOGGER_TAG, "\tURI: " + requestBase.getURI().toString());
        if (body != null) {
            Log.i(Tracker.LOGGER_TAG, "\tBODY: " + body);
        }
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
            Log.w(Tracker.LOGGER_TAG, String.format("Cannot encode %s", param), e);
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
}
