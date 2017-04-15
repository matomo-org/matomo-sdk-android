package org.piwik.sdk.dispatcher;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class EventDiskCacheTest {
    @Mock Piwik mPiwik;
    @Mock Tracker mTracker;
    @Mock Context mContext;
    private EventDiskCache mDiskCache;
    private File mHostFolder;
    private File mBaseCacheDir;
    private File mCacheFolder;

    @Before
    public void setup() throws MalformedURLException {
        MockitoAnnotations.initMocks(this);
        when(mTracker.getPiwik()).thenReturn(mPiwik);
        when(mPiwik.getContext()).thenReturn(mContext);
        mBaseCacheDir = new File("baseCacheDir");
        when(mContext.getCacheDir()).thenReturn(mBaseCacheDir);

        URL apiUrl = new URL("http://testhost/piwik.php");
        when(mTracker.getAPIUrl()).thenReturn(apiUrl);

        when(mTracker.getOfflineCacheAge()).thenReturn(0L);

        mCacheFolder = new File(mBaseCacheDir, "piwik_cache");
        mHostFolder = new File(mCacheFolder, "testhost");

        mDiskCache = new EventDiskCache(mTracker);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        for (File file : mBaseCacheDir.listFiles()[0].listFiles()[0].listFiles()) {
            file.delete();
        }
        mHostFolder.delete();
        mCacheFolder.delete();
        mBaseCacheDir.delete();
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(mDiskCache.isEmpty());
        mDiskCache.cache(Collections.singletonList(new Event("test")));
        assertFalse(mDiskCache.isEmpty());
    }

    @Test
    public void testCachePath() throws Exception {
        mDiskCache.cache(Collections.singletonList(new Event(1000, "test")));
        File cacheFolder = new File(mBaseCacheDir, "piwik_cache");
        File hostFolder = new File(cacheFolder, "testhost");
        assertTrue(hostFolder.exists());
        assertEquals(1, hostFolder.listFiles().length);
    }

    @Test
    public void testCacheFileName() throws Exception {
        mDiskCache.cache(Arrays.asList(new Event(1234567890, "test"), new Event(987654321, "test2")));
        File cacheFile = new File(mHostFolder, "events_987654321");
        assertTrue(cacheFile.exists());
        mDiskCache.uncache();
        assertFalse(cacheFile.exists());
    }

    @Test
    public void testCaching() throws Exception {
        Event event1 = new Event(1, "test");
        Event event2 = new Event(2, "test");
        mDiskCache.cache(Arrays.asList(event1, event2));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(2, events.size());
        assertEquals(event1, events.get(0));
        assertEquals(event2, events.get(1));
    }

    @Test
    public void testCaching_empty() throws Exception {
        mDiskCache.cache(Collections.<Event>emptyList());
    }

    @Test
    public void testOrder() throws Exception {
        Event event1 = new Event(1, "test");
        Event event2 = new Event(2, "test");
        mDiskCache.cache(Arrays.asList(event1, event2));
        Event event3 = new Event(3, "test");
        Event event4 = new Event(4, "test");
        mDiskCache.cache(Arrays.asList(event3, event4));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(4, events.size());
        assertEquals(event1, events.get(0));
        assertEquals(event2, events.get(1));
        assertEquals(event3, events.get(2));
        assertEquals(event4, events.get(3));
    }

    @Test
    public void testMaxAge_positive_allStale() throws Exception {
        when(mTracker.getOfflineCacheAge()).thenReturn(10 * 1000L);
        mDiskCache = new EventDiskCache(mTracker);
        Event event1 = new Event(1, "test");
        Event event2 = new Event(2, "test");
        mDiskCache.cache(Arrays.asList(event1, event2));
        assertEquals(0, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(0, events.size());
    }

    @Test
    public void testMaxAge_positive_singleContainer() throws Exception {
        when(mTracker.getOfflineCacheAge()).thenReturn(10 * 1000L);
        mDiskCache = new EventDiskCache(mTracker);
        Event event1 = new Event(System.currentTimeMillis() - 60 * 1000, "test");
        Event event2 = new Event(System.currentTimeMillis(), "test");
        Event event3 = new Event(2 * System.currentTimeMillis(), "test");
        mDiskCache.cache(Arrays.asList(event1, event2, event3));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(2, events.size());
        assertEquals(event2, events.get(0));
        assertEquals(event3, events.get(1));
    }

    @Test
    public void testMaxAge_positive_multipleContainer() throws Exception {
        when(mTracker.getOfflineCacheAge()).thenReturn(10 * 1000L);
        mDiskCache = new EventDiskCache(mTracker);
        Event event1 = new Event(System.currentTimeMillis() - 20 * 1000, "test");
        Event event2 = new Event(System.currentTimeMillis() - 15 * 1000, "test");
        mDiskCache.cache(Arrays.asList(event1, event2));
        Event event3 = new Event(System.currentTimeMillis() - 5 * 1000, "test");
        Event event4 = new Event(System.currentTimeMillis() - 2 * 1000, "test");
        mDiskCache.cache(Arrays.asList(event3, event4));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(2, events.size());
        assertEquals(event3, events.get(0));
        assertEquals(event4, events.get(1));
    }

    @Test
    public void testMaxAge_unlimited() throws Exception {
        when(mTracker.getOfflineCacheAge()).thenReturn(0L);
        mDiskCache = new EventDiskCache(mTracker);
        Event event1 = new Event(-System.currentTimeMillis(), "test1");
        Event event2 = new Event(0, "test2");
        Event event3 = new Event(System.currentTimeMillis(), "test3");
        Event event4 = new Event(2 * System.currentTimeMillis(), "test3");
        mDiskCache.cache(Arrays.asList(event1, event2, event3, event4));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(4, events.size());
        assertEquals(event1, events.get(0));
        assertEquals(event2, events.get(1));
        assertEquals(event3, events.get(2));
        assertEquals(event4, events.get(3));
    }

    @Test
    public void testMaxAge_negative_cachingDisabled() throws Exception {
        when(mTracker.getOfflineCacheAge()).thenReturn(-1L);
        mDiskCache = new EventDiskCache(mTracker);
        Event event0 = new Event(-System.currentTimeMillis(), "test");
        Event event1 = new Event(0, "test");
        Event event2 = new Event(System.currentTimeMillis(), "test");
        Event event3 = new Event(2 * System.currentTimeMillis(), "test");
        mDiskCache.cache(Arrays.asList(event0, event1, event2, event3));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(0, events.size());
    }

    @Test
    public void testClearDataOnceEvenIfDisabled() throws Exception {
        Event event1 = new Event(0, "test");
        Event event2 = new Event(System.currentTimeMillis(), "test");
        mDiskCache.cache(Arrays.asList(event1, event2));
        assertFalse(mDiskCache.isEmpty());
        mDiskCache = new EventDiskCache(mTracker);
        assertFalse(mDiskCache.isEmpty());
        when(mTracker.getOfflineCacheAge()).thenReturn(-1L);
        mDiskCache = new EventDiskCache(mTracker);
        assertTrue(mDiskCache.isEmpty());
    }

    @Test
    public void testMaxSize_limited() throws Exception {
        when(mTracker.getOfflineCacheSize()).thenReturn(500 * 1024L);
        mDiskCache = new EventDiskCache(mTracker);
        for (int j = 0; j < 4; j++) {
            List<Event> events = new ArrayList<>();
            for (int k = 0; k < 4000; k++) {
                events.add(new Event(System.nanoTime(), UUID.randomUUID().toString()));
            }
            // About 206KB
            mDiskCache.cache(events);
        }

        assertEquals(3, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(8000, events.size());
    }

    @Test
    public void testMaxSize_disabled() throws Exception {
        when(mTracker.getOfflineCacheSize()).thenReturn(0L);
        mDiskCache = new EventDiskCache(mTracker);
        for (int j = 0; j < 10; j++) {
            List<Event> events = new ArrayList<>();
            for (int k = 0; k < 1000; k++) {
                events.add(new Event(System.nanoTime(), UUID.randomUUID().toString()));
            }
            mDiskCache.cache(events);
        }

        assertEquals(10, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(10000, events.size());
    }

    @Test
    public void stressTest_singles() throws Exception {
        final Semaphore sem = new Semaphore(0);
        for (int i = 0; i < 8; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        Event event1 = new Event(System.nanoTime(), UUID.randomUUID().toString());
                        mDiskCache.cache(Collections.singletonList(event1));
                    }
                    sem.release(1);
                }
            }).start();
        }
        sem.acquire(8);
        assertEquals(800, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(800, events.size());
        assertEquals(0, mHostFolder.listFiles().length);
    }

    @Test
    public void stressTest_multi() throws Exception {
        final Semaphore sem = new Semaphore(0);
        for (int i = 0; i < 4; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        List<Event> events = new ArrayList<>();
                        for (int k = 0; k < 1000; k++) {
                            events.add(new Event(System.nanoTime(), UUID.randomUUID().toString()));
                        }
                        mDiskCache.cache(events);
                    }
                    sem.release(1);
                }
            }).start();
        }
        sem.acquire(4);
        assertEquals(40, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(40000, events.size());
        assertEquals(0, mHostFolder.listFiles().length);
    }
}
