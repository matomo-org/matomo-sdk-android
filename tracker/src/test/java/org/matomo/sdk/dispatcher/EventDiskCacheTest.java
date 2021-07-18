package org.matomo.sdk.dispatcher;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.Matomo;
import org.matomo.sdk.Tracker;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventDiskCacheTest extends BaseTest {
    @Mock Matomo mMatomo;
    @Mock Tracker mTracker;
    @Mock Context mContext;
    private EventDiskCache mDiskCache;
    private File mHostFolder;
    private File mBaseCacheDir;
    private File mCacheFolder;

    @Before
    public void setup() throws Exception {
        super.setup();
        when(mTracker.getMatomo()).thenReturn(mMatomo);
        when(mMatomo.getContext()).thenReturn(mContext);
        mBaseCacheDir = new File("baseCacheDir");
        when(mContext.getCacheDir()).thenReturn(mBaseCacheDir);

        when(mTracker.getAPIUrl()).thenReturn("http://testhost/matomo.php");

        when(mTracker.getOfflineCacheAge()).thenReturn(0L);

        mCacheFolder = new File(mBaseCacheDir, "piwik_cache");
        mHostFolder = new File(mCacheFolder, "testhost");

        mDiskCache = new EventDiskCache(mTracker);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (mHostFolder.exists()) {
            for (File file : mHostFolder.listFiles())
                file.delete();
        }
        mHostFolder.delete();
        mCacheFolder.delete();
        mBaseCacheDir.delete();
    }

    @Test
    public void testIsEmpty() {
        assertTrue(mDiskCache.isEmpty());
        mDiskCache.cache(Collections.singletonList(new Event("test")));
        assertFalse(mDiskCache.isEmpty());
    }

    @Test
    public void testCachePath() {
        mDiskCache.cache(Collections.singletonList(new Event(1000, "test")));
        File cacheFolder = new File(mBaseCacheDir, "piwik_cache");
        File hostFolder = new File(cacheFolder, "testhost");
        assertTrue(hostFolder.exists());
        assertEquals(1, hostFolder.listFiles().length);
    }

    @Test
    public void testCacheFileName() {
        mDiskCache.cache(Arrays.asList(new Event(1234567890, "test"), new Event(987654321, "test2")));
        File cacheFile = new File(mHostFolder, "events_987654321");
        assertTrue(cacheFile.exists());
        mDiskCache.uncache();
        assertFalse(cacheFile.exists());
    }

    @Test
    public void testCaching() {
        Event event1 = new Event(1, "test");
        Event event2 = new Event(2, "test");
        mDiskCache.cache(Arrays.asList(event1, event2));
        final List<Event> events = mDiskCache.uncache();
        assertEquals(2, events.size());
        assertEquals(event1, events.get(0));
        assertEquals(event2, events.get(1));
    }

    @Test
    public void testCaching_empty() {
        mDiskCache.cache(Collections.emptyList());
    }

    @Test
    public void testOrder() {
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
    public void testMaxAge_positive_allStale() {
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
    public void testMaxAge_positive_singleContainer() {
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
    public void testMaxAge_positive_multipleContainer() {
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
    public void testMaxAge_unlimited() {
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
    public void testMaxAge_negative_cachingDisabled() {
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
    public void testClearDataOnceEvenIfDisabled() {
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
    public void testMaxSize_limited() {
        when(mTracker.getOfflineCacheSize()).thenReturn(1024L);
        mDiskCache = new EventDiskCache(mTracker);
        for (int j = 0; j < 4; j++) {
            List<Event> events = new ArrayList<>();
            for (int k = 0; k < 10; k++) {
                events.add(new Event(System.nanoTime(), "set:" + j + " " + UUID.randomUUID().toString()));
            }
            // About ~512Byte
            mDiskCache.cache(events);
        }

        assertEquals(2, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(20, events.size());

        for (Event e : events.subList(0, 10)) {
            assertTrue(e.getEncodedQuery().startsWith("set:2"));
        }
        for (Event e : events.subList(10, 20)) {
            assertTrue(e.getEncodedQuery().startsWith("set:3"));
        }
    }

    @Test
    public void testMaxSize_disabled() {
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
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    Event event1 = new Event(System.nanoTime(), UUID.randomUUID().toString());
                    mDiskCache.cache(Collections.singletonList(event1));
                }
                sem.release(1);
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
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    List<Event> events = new ArrayList<>();
                    for (int k = 0; k < 1000; k++) {
                        events.add(new Event(System.nanoTime(), UUID.randomUUID().toString()));
                    }
                    mDiskCache.cache(events);
                }
                sem.release(1);
            }).start();
        }
        sem.acquire(4);
        assertEquals(40, mHostFolder.listFiles().length);
        final List<Event> events = mDiskCache.uncache();
        assertEquals(40000, events.size());
        assertEquals(0, mHostFolder.listFiles().length);
    }

    @Test
    public void testOfflineMode_issue_271() {
        when(mTracker.getOfflineCacheSize()).thenReturn(4096L);
        mDiskCache = new EventDiskCache(mTracker);

        // Hit limit
        for (int i = 0; i < 2; i++) {
            List<Event> batch1 = new ArrayList<>();
            for (int k = 0; k < 100; k++) {
                batch1.add(new Event(System.nanoTime(), UUID.randomUUID().toString()));
            }
            mDiskCache.cache(batch1);
        }

        final List<Event> events1 = mDiskCache.uncache();
        assertEquals(100, events1.size());

        // Hit limit again
        List<Event> batch2 = new ArrayList<>();
        for (int k = 0; k < 100; k++) {
            batch2.add(new Event(System.nanoTime(), UUID.randomUUID().toString()));
        }
        mDiskCache.cache(batch2);

        final List<Event> events2 = mDiskCache.uncache();
        assertEquals(100, events2.size());
    }
}
