/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Iterator;
import java.util.List;


public class TrackerBulkURLWrapper {

    private static final int eventsPerPage = 20;
    private int currentPage = 0;
    private final int pages;
    private final URL apiUrl;
    private final String authToken;
    private final List<String> events;

    public TrackerBulkURLWrapper(final URL apiUrl, final List<String> events, final String authToken) {
        this.apiUrl = apiUrl;
        this.authToken = authToken;
        this.pages = (int) Math.ceil(events.size() * 1.0 / eventsPerPage);
        this.events = events;
    }

    protected static int getEventsPerPage(){
        return eventsPerPage;
    }

    /**
     * page iterator
     *
     * @return iterator
     */
    public Iterator<Page> iterator() {
        return new Iterator<Page>() {
            @Override
            public boolean hasNext() {
                return currentPage < pages;
            }

            @Override
            public Page next() {
                if (hasNext()) {
                    return new Page(currentPage++);
                }
                return null;
            }

            @Override
            public void remove() {
            }
        };
    }

    public URL getApiUrl() {
        return apiUrl;
    }

    /**
     * {
     * "requests": ["?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
     * "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"],
     * "token_auth": "33dc3f2536d3025974cccb4b4d2d98f4"
     * }
     *
     * @return json object
     */
    public JSONObject getEvents(Page page) {
        if (page == null || page.isEmpty()) {
            return null;
        }

        List<String> pageElements = events.subList(page.fromIndex, page.toIndex);

        if(pageElements.size() == 0){
            Log.w(Tracker.LOGGER_TAG, "Empty page");
            return null;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("requests", new JSONArray(pageElements));

            if (authToken != null) {
                params.put(QueryParams.AUTHENTICATION_TOKEN.toString(), authToken);
            }
        } catch (JSONException e) {
            Log.w(Tracker.LOGGER_TAG, "Cannot create json object", e);
            Log.i(Tracker.LOGGER_TAG, TextUtils.join(", ", pageElements));
            return null;
        }
        return params;
    }

    /**
     * @param page Page object
     * @return tracked url. For example
     *  "http://domain.com/piwik.php?idsite=1&url=http://a.org&action_name=Test bulk log Pageview&rec=1"
     */
    public String getEventUrl(Page page) {
        if (page == null || page.isEmpty()) {
            return null;
        }

        return getApiUrl().toString() + events.get(page.fromIndex);
    }

    public final class Page {

        protected final int fromIndex, toIndex;

        protected Page(int pageNumber) {
            if (!(pageNumber >= 0 || pageNumber < pages)) {
                fromIndex = toIndex = -1;
                return;
            }
            fromIndex = pageNumber * eventsPerPage;
            toIndex = Math.min(fromIndex + eventsPerPage, events.size());
        }

        public int elementsCount() {
            return toIndex - fromIndex;
        }

        public boolean isEmpty() {
            return fromIndex == -1 || elementsCount() == 0;
        }
    }

}
