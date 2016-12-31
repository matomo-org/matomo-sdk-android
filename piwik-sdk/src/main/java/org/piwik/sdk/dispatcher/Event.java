package org.piwik.sdk.dispatcher;


public class Event {
    private final long mTimestamp;
    private final String mQuery;

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

    public String getQuery() {
        return mQuery;
    }

    @Override
    public String toString() {
        return getQuery();
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
}
