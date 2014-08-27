package org.piwik.sdk;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

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

    /**
     * page iterator
     * @return iterator
     */
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return currentPage < pages;
            }

            @Override
            public Integer next() {
                return currentPage++;
            }

            @Override
            public void remove() {
                // TODO Auto-generated method stub
            }
        };
    }

    public URL getApiUrl() {
        return apiUrl;
    }

    /**
     * {
     *  "requests": ["?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
     *               "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"],
     *  "token_auth": "33dc3f2536d3025974cccb4b4d2d98f4"
     * }
     *
     * @return json object
     */
    public JSONObject getJSONBody(Integer page) {
        if(!(page >= 0 || page < pages)){
            return null;
        }

        int fromIndex = page * eventsPerPage;
        int toIndex = fromIndex + eventsPerPage;
        toIndex = Math.min(toIndex, events.size());

        JSONObject params = new JSONObject();
        try {
            params.put("requests", new JSONArray(events.subList(fromIndex, toIndex)));

            if (authToken != null) {
                params.put("token_auth", authToken);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return params;
    }
}
