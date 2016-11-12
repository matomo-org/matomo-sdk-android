package org.piwik.sdk.dispatcher;


import android.support.annotation.NonNull;

import org.piwik.sdk.Piwik;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class EventCache {
    private static final String TAG = Piwik.LOGGER_PREFIX + "EventCache";
    private final LinkedBlockingDeque<String> mDispatchQueue = new LinkedBlockingDeque<>();
    private final EventDiskCache mCache;
    private boolean mLastFailed = false;

    public EventCache(EventDiskCache cache) {
        mCache = cache;
    }

    public void add(String query) {
        mDispatchQueue.add(query);
    }

    public void drain(List<String> drainedEvents) {
        if (!mLastFailed && !mCache.isEmpty()) {
            drainedEvents.addAll(mCache.uncache());
        } else {
            mDispatchQueue.drainTo(drainedEvents);
        }
    }

    public void clearFailedFlag() {
        mLastFailed = false;
    }

    public void postpone(@NonNull List<String> failedEvents) {
        mCache.cache(failedEvents);
        mLastFailed = true;
    }

    public boolean isEmpty() {
        return mDispatchQueue.isEmpty() && mCache.isEmpty();
    }
}
