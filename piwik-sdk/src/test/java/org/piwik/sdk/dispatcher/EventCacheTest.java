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
        eventCache.add("test");
        assertFalse(eventCache.isEmpty());
        List<String> events = new ArrayList<>();
        eventCache.drain(events);
        assertEquals("test", events.get(0));
        assertTrue(eventCache.isEmpty());
    }

    @Test
    public void testDrain_empty() throws Exception {
        List<String> events = new ArrayList<>();
        eventCache.drain(events);
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_empty() throws Exception {
        List<String> events = new ArrayList<>();
        eventCache.drain(events);
        verify(eventDiskCache, never()).uncache();
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_nonempty() throws Exception {
        List<String> events = new ArrayList<>();
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(eventDiskCache.uncache()).thenReturn(Collections.singletonList("test"));
        eventCache.updateState(true);
        eventCache.drain(events);
        verify(eventDiskCache).uncache();
        assertFalse(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_first() throws Exception {
        eventCache.add("3");
        List<String> events = new ArrayList<>();
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(eventDiskCache.uncache()).thenReturn(Arrays.asList("1", "2"));
        eventCache.updateState(true);
        eventCache.drain(events);
        verify(eventDiskCache).uncache();
        assertFalse(events.isEmpty());
        assertEquals("1", events.get(0));
        assertEquals("2", events.get(1));
        assertEquals("3", events.get(2));
    }

    @Test
    public void testUpdateState_online() throws Exception {
        when(eventDiskCache.isEmpty()).thenReturn(true);
        verify(eventDiskCache, never()).uncache();
        eventCache.updateState(true);
        verify(eventDiskCache).isEmpty();
        when(eventDiskCache.isEmpty()).thenReturn(false);
        eventCache.updateState(true);
        verify(eventDiskCache).uncache();
        verify(eventDiskCache, times(2)).isEmpty();
    }

    @Test
    public void testUpdateState_offline() throws Exception {
        assertTrue(eventCache.isEmpty());
        eventCache.add("test");
        assertFalse(eventCache.isEmpty());
        eventCache.updateState(false);
        verify(eventDiskCache).cache(ArgumentMatchers.<String>anyList());

        eventCache.updateState(false);
        verify(eventDiskCache).cache(ArgumentMatchers.<String>anyList());
        eventCache.add("test");
        eventCache.updateState(false);
        verify(eventDiskCache, times(2)).cache(ArgumentMatchers.<String>anyList());
    }

    @Test
    public void testIsEmpty() {
        when(eventDiskCache.isEmpty()).thenReturn(true);
        assertTrue(eventCache.isEmpty());
        when(eventDiskCache.isEmpty()).thenReturn(false);
        assertFalse(eventCache.isEmpty());

        when(eventDiskCache.isEmpty()).thenReturn(true);
        assertTrue(eventCache.isEmpty());
        eventCache.add("test");
        assertFalse(eventCache.isEmpty());

        when(eventDiskCache.isEmpty()).thenReturn(false);
        assertFalse(eventCache.isEmpty());
    }

}
