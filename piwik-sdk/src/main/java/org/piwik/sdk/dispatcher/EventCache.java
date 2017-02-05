package org.piwik.sdk.dispatcher;


import org.piwik.sdk.Piwik;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.LinkedBlockingDeque;

import timber.log.Timber;

public class EventCache {
    private static final String TAG = Piwik.LOGGER_PREFIX + "EventCache";
    private final LinkedBlockingDeque<Event> mQueue = new LinkedBlockingDeque<>();
    private final EventDiskCache mDiskCache;

    public EventCache(EventDiskCache cache) {
        mDiskCache = cache;
    }

    public void add(Event event) {
        mQueue.add(event);
    }

    public void drainTo(List<Event> drainedEvents) {
        mQueue.drainTo(drainedEvents);
    }

    public void clear() {
        mDiskCache.uncache();
        mQueue.clear();
    }

    public boolean isEmpty() {
        return mQueue.isEmpty() && mDiskCache.isEmpty();
    }

    public void updateState(boolean online) {
        if (online) {
            final List<Event> uncache = mDiskCache.uncache();
            ListIterator<Event> it = uncache.listIterator(uncache.size());
            while (it.hasPrevious()) {
                // Anything from  disk cache is older then what the queue could currently contain.
                mQueue.offerFirst(it.previous());
            }
            Timber.tag(TAG).d("Switched state to ONLINE, uncached %d events from disk.", uncache.size());
        } else if (!mQueue.isEmpty()) {
            List<Event> toCache = new ArrayList<>();
            mQueue.drainTo(toCache);
            mDiskCache.cache(toCache);
            Timber.tag(TAG).d("Switched state to OFFLINE, caching %d events to disk.", toCache.size());
        }
    }

    public void requeue(List<Event> events) {
        for (Event e : events) {
            mQueue.offerFirst(e);
        }
    }

}
