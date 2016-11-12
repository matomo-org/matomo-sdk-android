package org.piwik.sdk.dispatcher;


import org.piwik.sdk.Piwik;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.LinkedBlockingDeque;

public class EventCache {
    private static final String TAG = Piwik.LOGGER_PREFIX + "EventCache";
    private final LinkedBlockingDeque<String> mQueue = new LinkedBlockingDeque<>();
    private final EventDiskCache mDiskCache;

    public EventCache(EventDiskCache cache) {
        mDiskCache = cache;
    }

    public void add(String query) {
        mQueue.add(query);
    }

    public void drain(List<String> drainedEvents) {
        mQueue.drainTo(drainedEvents);
    }

    public boolean isEmpty() {
        return mQueue.isEmpty() && mDiskCache.isEmpty();
    }

    public void updateState(boolean online) {
        if (online && !mDiskCache.isEmpty()) {
            final List<String> uncache = mDiskCache.uncache();
            ListIterator<String> it = uncache.listIterator(uncache.size());
            while (it.hasPrevious()) {
                mQueue.offerFirst(it.previous());
            }
        } else if (!online && !mQueue.isEmpty()) {
            List<String> toCache = new ArrayList<>();
            mQueue.drainTo(toCache);
            mDiskCache.cache(toCache);
        }
    }

    public void requeue(List<String> events) {
        for (String e : events) {
            mQueue.offerFirst(e);
        }
    }
}
