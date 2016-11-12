package org.piwik.sdk.dispatcher;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import timber.log.Timber;

public class EventDiskCache {
    private static final String TAG = Piwik.LOGGER_PREFIX + "EventDiskCache";
    private static final String CACHE_DIR_NAME = "piwik_cache";
    // We can send old events without authtokens for up 4 hours, and the default dispatch interval is 2 minutes.
    private static final int FILE_LIMIT = 4 * 60 / 2;
    private static final String VERSION = "1";
    private final LinkedBlockingQueue<File> mEventContainer = new LinkedBlockingQueue<>();
    private final File mCacheDir;

    public EventDiskCache(Tracker tracker) {
        File baseDir = new File(tracker.getPiwik().getContext().getCacheDir(), CACHE_DIR_NAME);
        mCacheDir = new File(baseDir, tracker.getAPIUrl().getHost());
        File[] storedContainers = mCacheDir.listFiles();
        if (storedContainers != null) {
            Arrays.sort(storedContainers);
            mEventContainer.addAll(Arrays.asList(storedContainers));
        } else {
            if (!mCacheDir.mkdirs()) Timber.tag(TAG).e("Failed to make disk-cache dir %s", mCacheDir);
        }
    }

    public synchronized void cache(@NonNull List<String> toCache) {
        long startTime = System.currentTimeMillis();
        File container = writeEventFile(toCache);
        mEventContainer.add(container);
        if (mEventContainer.size() > FILE_LIMIT) {
            final File oldest = mEventContainer.poll();
            if (!oldest.delete()) Timber.tag(TAG).e("Failed to delete cache container %s", oldest.getPath());
        }
        long stopTime = System.currentTimeMillis();
        Timber.tag(TAG).d("Caching of %d events took %dms", toCache.size(), (stopTime - startTime));
    }

    @NonNull
    public synchronized List<String> uncache() {
        long startTime = System.currentTimeMillis();
        List<String> events = new ArrayList<>();
        while (!mEventContainer.isEmpty()) {
            File head = mEventContainer.poll();
            if (head != null) {
                events.addAll(readEventFile(head));
                if (!head.delete()) Timber.tag(TAG).e("Failed to delete cache container %s", head.getPath());
            }
        }
        long stopTime = System.currentTimeMillis();
        Timber.tag(TAG).d("Uncaching of %d events took %dms", events.size(), (stopTime - startTime));
        return events;
    }

    public synchronized boolean isEmpty() {
        return mEventContainer.isEmpty();
    }

    private List<String> readEventFile(@NonNull File file) {
        List<String> events = new ArrayList<>();
        if (!file.exists()) {
            return events;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String versionLine = bufferedReader.readLine();
            if (!VERSION.equals(versionLine)) return events;

            String timestampLine = bufferedReader.readLine();
            if (timestampLine == null) return events;
            try {
                long timestamp = Long.parseLong(timestampLine);
                // after 4 hours it needs an authtoken, which is not encouraged
                // https://developer.piwik.org/api-reference/tracking-api
                if (System.currentTimeMillis() - timestamp > 4 * 60 * 60 * 1000) {
                    return events;
                }
            } catch (NumberFormatException e) {
                Timber.tag(TAG).e(e, null);
                return events;
            }

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                events.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) { Timber.tag(TAG).e(e, null); }
            }
        }

        Timber.tag(TAG).d("Restored %d events from %s", events.size(), file.getPath());
        return events;
    }

    @Nullable
    private File writeEventFile(@NonNull List<String> events) {
        File newFile = new File(mCacheDir, "events_" + System.currentTimeMillis());
        FileWriter out = null;
        try {
            out = new FileWriter(newFile);
            out.append(VERSION).append("\n");
            out.append(String.valueOf(System.currentTimeMillis())).append("\n");
            for (String event : events) {
                out.append(event).append("\n");
            }
            Timber.tag(TAG).d("Saved %d events to %s", events.size(), newFile.getPath());
            return newFile;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException e) { Timber.tag(TAG).e(e, null); }
            }
        }
        return null;
    }

}
