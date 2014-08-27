package org.piwik.sdk;


import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * Sends json POST request to tracking url http://piwik.example.com/piwik.php with body
 *
   {
   "requests": [
      "?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
      "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"
   ],
   "token_auth": "33dc3f2536d3025974cccb4b4d2d98f4"
   }
 */
public class TrackerBulkURLProcessor extends AsyncTask<TrackerBulkURLWrapper, Integer, Integer> {

    private final int timeout;
    private final Dispatchable<Integer> dispatchable;

    public TrackerBulkURLProcessor(final Dispatchable<Integer> tracker, int timeout){
        dispatchable = tracker;
        this.timeout = timeout;
    }

    protected Integer doInBackground(TrackerBulkURLWrapper... wrappers) {
        int count = 0;

        for(TrackerBulkURLWrapper wrapper : wrappers){
            Iterator<Integer> pageIterator = wrapper.iterator();
            while (pageIterator.hasNext()){
                count += doPost(wrapper.getApiUrl(), wrapper.getJSONBody(pageIterator.next()));
                if (isCancelled()) break;
            }
        }

        return count;
    }

    protected void onPostExecute(Integer count) {
        this.dispatchable.dispatchingCompleted(count);
    }

    public void processBulkURLs(URL apiUrl, List<String> events, String authToken) {
        dispatchable.startDispatching();
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

    public int doPost(URL url, JSONObject json) {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), timeout);
        HttpResponse response;

        try {
            HttpPost post = new HttpPost(url.toURI());
            StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);
            response = client.execute(post);
            // TODO check response code
            return 1;
        } catch(Exception e) {
            e.printStackTrace();
            return 0;
        }

    }
}
