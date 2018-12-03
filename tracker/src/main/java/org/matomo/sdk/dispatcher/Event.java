package org.matomo.sdk.dispatcher;


import org.matomo.sdk.Matomo;

import java.net.URLEncoder;
import java.util.Map;

import timber.log.Timber;

public class Event {
    private static final String TAG = Matomo.tag(Event.class);
    private final long mTimestamp;
    private final String mQuery;

    public Event(Map<String, String> eventData) {
        this(urlEncodeUTF8(eventData));
    }

    public Event(String query) {
        this(System.currentTimeMillis(), query);
    }

    public Event(long timestamp, String query) {
        this.mTimestamp = timestamp;
        this.mQuery = query;
    }

    public long getTimeStamp() {
        return mTimestamp;
    }

    public String getEncodedQuery() {
        return mQuery;
    }

    @Override
    public String toString() {
        return getEncodedQuery();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        return mTimestamp == event.mTimestamp && mQuery.equals(event.mQuery);

    }

    @Override
    public int hashCode() {
        int result = (int) (mTimestamp ^ (mTimestamp >>> 32));
        result = 31 * result + mQuery.hashCode();
        return result;
    }

    /**
     * http://stackoverflow.com/q/4737841
     *
     * @param param raw data
     * @return encoded string
     */
    private static String urlEncodeUTF8(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8").replaceAll("\\+", "%20");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Cannot encode %s", param);
            return "";
        }
    }

    /**
     * URL encodes a key-value map
     */
    private static String urlEncodeUTF8(Map<String, String> map) {
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
