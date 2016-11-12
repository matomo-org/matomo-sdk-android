package org.piwik.sdk.dispatcher;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventDiskCacheTest {

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
        eventCache.drain(events);
        verify(eventDiskCache).uncache();
        assertFalse(events.isEmpty());
    }

    @Test
    public void testDrain_diskCache_first() throws Exception {
        eventCache.add("1");
        List<String> events = new ArrayList<>();
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(eventDiskCache.uncache()).thenReturn(Collections.singletonList("2"));
        eventCache.drain(events);
        verify(eventDiskCache).uncache();
        assertFalse(events.isEmpty());
        assertEquals("2", events.get(0));
    }

    @Test
    public void testPostpone_drainflag() throws Exception {
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(eventDiskCache.uncache()).thenReturn(Collections.singletonList("2"));

        eventCache.postpone(Collections.<String>emptyList());
        eventCache.add("1");

        List<String> events = new ArrayList<>();
        eventCache.drain(events);
        verify(eventDiskCache, never()).uncache();
        assertEquals("1", events.get(0));

        eventCache.clearFailedFlag();
        events.clear();
        eventCache.drain(events);
        verify(eventDiskCache).uncache();
        assertEquals("2", events.get(0));
    }

    @Test
    public void testPostpone_caching() throws Exception {
        List<String> events = new ArrayList<>();
        eventCache.postpone(events);
        verify(eventDiskCache).cache(events);
    }

}
