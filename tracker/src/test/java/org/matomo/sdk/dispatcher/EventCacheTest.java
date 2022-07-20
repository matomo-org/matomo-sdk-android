package org.matomo.sdk.dispatcher;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventCacheTest extends BaseTest {

    @Mock EventDiskCache mEventDiskCache;
    EventCache mEventCache;

    @Before
    public void setup() throws Exception {
        super.setup();
        when(mEventDiskCache.isEmpty()).thenReturn(true);
        mEventCache = spy(new EventCache(mEventDiskCache));
    }

    @Test
    public void testClear() {
        mEventCache.add(new Event("test"));
        mEventCache.clear();
        verify(mEventDiskCache).uncache();
        assertTrue(mEventCache.isEmpty());
    }

    @Test
    public void testDrain_simple() {
        assertTrue(mEventCache.isEmpty());
        mEventCache.add(new Event("test"));
        assertFalse(mEventCache.isEmpty());
        List<Event> events = new ArrayList<>();
        mEventCache.drainTo(events);
        assertEquals("test", events.get(0).getEncodedQuery());
        assertTrue(mEventCache.isEmpty());
    }

    @Test
    public void testDrain_empty() {
        List<Event> events = new ArrayList<>();
        mEventCache.drainTo(events);
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_empty() {
        List<Event> events = new ArrayList<>();
        mEventCache.drainTo(events);
        verify(mEventDiskCache, never()).uncache();
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_nonempty() {
        List<Event> events = new ArrayList<>();
        when(mEventDiskCache.uncache()).thenReturn(Collections.singletonList(new Event("test")));
        mEventCache.updateState(true);
        mEventCache.drainTo(events);
        verify(mEventDiskCache).uncache();
        assertFalse(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_first() {
        mEventCache.add(new Event("3"));
        List<Event> events = new ArrayList<>();
        when(mEventDiskCache.uncache()).thenReturn(Arrays.asList(new Event("1"), new Event("2")));
        mEventCache.updateState(true);
        mEventCache.drainTo(events);
        verify(mEventDiskCache).uncache();
        assertFalse(events.isEmpty());
        assertEquals("1", events.get(0).getEncodedQuery());
        assertEquals("2", events.get(1).getEncodedQuery());
        assertEquals("3", events.get(2).getEncodedQuery());
    }

    @Test
    public void testUpdateState_online() {
        verify(mEventDiskCache, never()).uncache();
        mEventCache.updateState(true);
        mEventCache.updateState(true);
        verify(mEventDiskCache, times(2)).uncache();
    }

    @Test
    public void testUpdateState_offline() {
        assertTrue(mEventCache.isEmpty());
        mEventCache.add(new Event("test"));
        assertFalse(mEventCache.isEmpty());
        mEventCache.updateState(false);
        verify(mEventDiskCache).cache(ArgumentMatchers.anyList());

        mEventCache.updateState(false);
        verify(mEventDiskCache).cache(ArgumentMatchers.anyList());
        mEventCache.add(new Event("test"));
        mEventCache.updateState(false);
        verify(mEventDiskCache, times(2)).cache(ArgumentMatchers.anyList());
    }

    @Test
    public void testUpdateState_offline_ordering() {
        assertTrue(mEventCache.isEmpty());
        mEventCache.add(new Event("test2"));
        when(mEventDiskCache.uncache()).thenReturn(Arrays.asList(new Event("test0"), new Event("test1")));
        mEventCache.updateState(true);

        List<Event> restoredEvents = new ArrayList<>();
        mEventCache.drainTo(restoredEvents);

        assertEquals(3, restoredEvents.size());
        assertEquals("test0", restoredEvents.get(0).getEncodedQuery());
        assertEquals("test1", restoredEvents.get(1).getEncodedQuery());
        assertEquals("test2", restoredEvents.get(2).getEncodedQuery());
    }

}
