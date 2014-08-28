package org.piwik.sdk;


import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
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
    private final Dispatchable<Integer> dispatchable;

    public TrackerBulkURLProcessor(final Dispatchable<Integer> tracker, int timeout) {
        dispatchable = tracker;
        this.timeout = timeout;
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

        return doRequest(get);
    }

    public boolean doPost(URL url, JSONObject json) {
        if (url == null || json == null) {
            return false;
        }

        try {
            HttpPost post = new HttpPost(url.toURI());
            StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);

            return doRequest(post);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean doRequest(HttpRequestBase requestBase) {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), timeout);
        HttpResponse response;

        try {
            response = client.execute(requestBase);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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

        return sb.toString();
    }
}
