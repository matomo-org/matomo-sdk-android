package org.piwik.sdk.dispatcher;


import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventCacheTest {

    EventCache eventCache;

    @Mock EventDiskCache eventDiskCache;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(eventDiskCache.isEmpty()).thenReturn(true);
        eventCache = spy(new EventCache(eventDiskCache));
    }

    @Test
    public void testDrain_simple() throws Exception {
        assertTrue(eventCache.isEmpty());
        eventCache.add(new Event("test"));
        assertFalse(eventCache.isEmpty());
        List<Event> events = new ArrayList<>();
        eventCache.drainTo(events);
        assertEquals("test", events.get(0).getQuery());
        assertTrue(eventCache.isEmpty());
    }

    @Test
    public void testDrain_empty() throws Exception {
        List<Event> events = new ArrayList<>();
        eventCache.drainTo(events);
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_empty() throws Exception {
        List<Event> events = new ArrayList<>();
        eventCache.drainTo(events);
        verify(eventDiskCache, never()).uncache();
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_nonempty() throws Exception {
        List<Event> events = new ArrayList<>();
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(eventDiskCache.uncache()).thenReturn(Collections.singletonList(new Event("test")));
        eventCache.updateState(true);
        eventCache.drainTo(events);
        verify(eventDiskCache).uncache();
        assertFalse(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_first() throws Exception {
        eventCache.add(new Event("3"));
        List<Event> events = new ArrayList<>();
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(eventDiskCache.uncache()).thenReturn(Arrays.asList(new Event("1"), new Event("2")));
        eventCache.updateState(true);
        eventCache.drainTo(events);
        verify(eventDiskCache).uncache();
        assertFalse(events.isEmpty());
        assertEquals("1", events.get(0).getQuery());
        assertEquals("2", events.get(1).getQuery());
        assertEquals("3", events.get(2).getQuery());
    }

    @Test
    public void testUpdateState_online() throws Exception {
        verify(eventDiskCache, never()).uncache();
        eventCache.updateState(true);
        when(eventDiskCache.isEmpty()).thenReturn(false);
        eventCache.updateState(true);
        verify(eventDiskCache, times(2)).uncache();
    }

    @Test
    public void testUpdateState_offline() throws Exception {
        assertTrue(eventCache.isEmpty());
        eventCache.add(new Event("test"));
        assertFalse(eventCache.isEmpty());
        eventCache.updateState(false);
        verify(eventDiskCache).cache(ArgumentMatchers.<Event>anyList());

        eventCache.updateState(false);
        verify(eventDiskCache).cache(ArgumentMatchers.<Event>anyList());
        eventCache.add(new Event("test"));
        eventCache.updateState(false);
        verify(eventDiskCache, times(2)).cache(ArgumentMatchers.<Event>anyList());
    }

    @Test
    public void testUpdateState_offline_ordering() throws Exception {
        assertTrue(eventCache.isEmpty());
        eventCache.add(new Event("test2"));
        when(eventDiskCache.uncache()).thenReturn(Arrays.asList(new Event("test0"), new Event("test1")));
        when(eventDiskCache.isEmpty()).thenReturn(false);
        eventCache.updateState(true);

        List<Event> restoredEvents = new ArrayList<>();
        eventCache.drainTo(restoredEvents);

        assertEquals(3, restoredEvents.size());
        assertEquals("test0", restoredEvents.get(0).getQuery());
        assertEquals("test1", restoredEvents.get(1).getQuery());
        assertEquals("test2", restoredEvents.get(2).getQuery());
    }

}
