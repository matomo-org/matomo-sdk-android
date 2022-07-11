package org.matomo.sdk.dispatcher;


import org.matomo.sdk.Matomo;
import org.matomo.sdk.Tracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class EventDiskCache {
    private static final String TAG = Matomo.tag(EventDiskCache.class);
    private static final String CACHE_DIR_NAME = "piwik_cache";
    private static final String VERSION = "1";
    private final LinkedBlockingQueue<File> mEventContainer = new LinkedBlockingQueue<>();
    private final File mCacheDir;
    private final long mMaxAge;
    private final long mMaxSize;
    private long mCurrentSize = 0;
    private boolean mDelayedClear = false;

    public EventDiskCache(Tracker tracker) {
        mMaxAge = tracker.getOfflineCacheAge();
        mMaxSize = tracker.getOfflineCacheSize();
        File baseDir = new File(tracker.getMatomo().getContext().getCacheDir(), CACHE_DIR_NAME);
        try {
            mCacheDir = new File(baseDir, new URL(tracker.getAPIUrl()).getHost());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        File[] storedContainers = mCacheDir.listFiles();
        if (storedContainers != null) {
            Arrays.sort(storedContainers);
            for (File container : storedContainers) {
                mCurrentSize += container.length();
                mEventContainer.add(container);
            }
        }
    }

    // Must be called from a synchronized method
    private void checkCacheLimits() {
        long startTime = System.currentTimeMillis();
        if (mMaxAge < 0) {
            Timber.tag(TAG).d("Caching is disabled.");
            while (!mEventContainer.isEmpty()) {
                File head = mEventContainer.poll();
                if (head.delete()) {
                    Timber.tag(TAG).e("Deleted cache container %s", head.getPath());
                }
            }
        } else if (mMaxAge > 0) {
            final Iterator<File> iterator = mEventContainer.iterator();
            while (iterator.hasNext()) {
                File head = iterator.next();
                long timestamp;
                try {
                    final String[] split = head.getName().split("_");
                    timestamp = Long.parseLong(split[1]);
                } catch (Exception e) {
                    Timber.tag(TAG).e(e);
                    timestamp = 0;
                }
                if (timestamp < (System.currentTimeMillis() - mMaxAge)) {
                    if (head.delete()) Timber.tag(TAG).e("Deleted cache container %s", head.getPath());
                    else Timber.tag(TAG).e("Failed to delete cache container %s", head.getPath());
                    iterator.remove();
                } else {
                    // List is sorted by age
                    break;
                }
            }
        }
        if (mMaxSize != 0) {
            final Iterator<File> iterator = mEventContainer.iterator();
            while (iterator.hasNext() && mCurrentSize > mMaxSize) {
                File head = iterator.next();
                mCurrentSize -= head.length();
                iterator.remove();
                if (head.delete()) Timber.tag(TAG).e("Deleted cache container %s", head.getPath());
                else Timber.tag(TAG).e("Failed to delete cache container %s", head.getPath());
            }
        }
        long stopTime = System.currentTimeMillis();
        Timber.tag(TAG).d("Cache check took %dms", (stopTime - startTime));
    }

    private boolean isCachingEnabled() {
        return mMaxAge >= 0;
    }

    public synchronized void cache(@NonNull List<Event> toCache) {
        if (!isCachingEnabled() || toCache.isEmpty()) return;

        checkCacheLimits();

        long startTime = System.currentTimeMillis();

        File container = writeEventFile(toCache);
        if (container != null) {
            mEventContainer.add(container);
            mCurrentSize += container.length();
        }
        long stopTime = System.currentTimeMillis();
        Timber.tag(TAG).d("Caching of %d events took %dms (%s)", toCache.size(), (stopTime - startTime), container);
    }

    @NonNull
    public synchronized List<Event> uncache() {
        List<Event> events = new ArrayList<>();
        if (!isCachingEnabled()) return events;

        long startTime = System.currentTimeMillis();
        while (!mEventContainer.isEmpty()) {
            File head = mEventContainer.poll();
            if (head != null) {
                events.addAll(readEventFile(head));
                if (!head.delete()) Timber.tag(TAG).e("Failed to delete cache container %s", head.getPath());
            }
        }

        checkCacheLimits();

        long stopTime = System.currentTimeMillis();
        Timber.tag(TAG).d("Uncaching of %d events took %dms", events.size(), (stopTime - startTime));
        return events;
    }

    public synchronized boolean isEmpty() {
        if (!mDelayedClear) {
            checkCacheLimits();
            mDelayedClear = true;
        }
        return mEventContainer.isEmpty();
    }

    private List<Event> readEventFile(@NonNull File file) {
        List<Event> events = new ArrayList<>();
        if (!file.exists()) return events;

        InputStream in = null;
        try {
            in = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String versionLine = bufferedReader.readLine();
            if (!VERSION.equals(versionLine)) return events;

            final long cutoff = System.currentTimeMillis() - mMaxAge;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                final int split = line.indexOf(" ");
                if (split == -1) continue;

                try {
                    long timestamp = Long.parseLong(line.substring(0, split));
                    if (mMaxAge > 0 && timestamp < cutoff) continue;

                    String query = line.substring(split + 1);
                    events.add(new Event(timestamp, query));
                } catch (Exception e) { Timber.tag(TAG).e(e); }
            }
        } catch (IOException e) {
            Timber.tag(TAG).e(e);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) { Timber.tag(TAG).e(e); }
            }
        }

        Timber.tag(TAG).d("Restored %d events from %s", events.size(), file.getPath());
        return events;
    }

    @Nullable
    private File writeEventFile(@NonNull List<Event> events) {
        if (events.isEmpty()) return null;

        if (!mCacheDir.exists() && !mCacheDir.mkdirs())
            Timber.tag(TAG).e("Failed to make disk-cache dir '%s'", mCacheDir);

        File newFile = new File(mCacheDir, "events_" + events.get(events.size() - 1).getTimeStamp());
        FileWriter out = null;
        boolean dataWritten = false;
        try {
            out = new FileWriter(newFile);
            out.append(VERSION).append("\n");

            final long cutoff = System.currentTimeMillis() - mMaxAge;
            for (Event event : events) {
                if (mMaxAge > 0 && event.getTimeStamp() < cutoff) continue;
                out.append(String.valueOf(event.getTimeStamp())).append(" ").append(event.getEncodedQuery()).append("\n");
                dataWritten = true;
            }
        } catch (IOException e) {
            Timber.tag(TAG).e(e);
            //noinspection ResultOfMethodCallIgnored
            newFile.delete();
            return null;
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException e) { Timber.tag(TAG).e(e); }
            }
        }

        Timber.tag(TAG).d("Saved %d events to %s", events.size(), newFile.getPath());

        // If just version data was written delete the file.
        if (dataWritten) return newFile;
        else {
            //noinspection ResultOfMethodCallIgnored
            newFile.delete();
            return null;
        }
    }

}
