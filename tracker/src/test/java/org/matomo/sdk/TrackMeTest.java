package org.matomo.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(MockitoJUnitRunner.class)
public class TrackMeTest extends BaseTest {
    @Test
    public void testSourcingFromOtherTrackMe() {
        TrackMe base = new TrackMe();
        for (QueryParams param : QueryParams.values()) {
            String testValue = UUID.randomUUID().toString();
            base.set(param, testValue);
        }

        TrackMe offSpring = new TrackMe(base);
        for (QueryParams param : QueryParams.values()) {
            assertEquals(base.get(param), offSpring.get(param));
        }
    }

    @Test
    public void testAdd_overwrite() {
        TrackMe a = new TrackMe();
        a.set(QueryParams.URL_PATH, "pathA");
        a.set(QueryParams.EVENT_NAME, "name");
        TrackMe b = new TrackMe();
        b.set(QueryParams.URL_PATH, "pathB");
        a.putAll(b);
        assertEquals("pathB", a.get(QueryParams.URL_PATH));
        assertEquals("pathB", b.get(QueryParams.URL_PATH));
        assertEquals("name", a.get(QueryParams.EVENT_NAME));

        b.putAll(a);
        assertEquals("pathB", a.get(QueryParams.URL_PATH));
        assertEquals("pathB", b.get(QueryParams.URL_PATH));
        assertEquals("name", a.get(QueryParams.EVENT_NAME));
        assertEquals("name", b.get(QueryParams.EVENT_NAME));

    }

    @Test
    public void testSet() {
        TrackMe trackMe = new TrackMe();
        trackMe.set(QueryParams.HOURS, "String");
        assertEquals("String", trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.set(QueryParams.HOURS, 1f);
        assertEquals(String.valueOf(1f), trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.set(QueryParams.HOURS, 1L);
        assertEquals(String.valueOf(1L), trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.set(QueryParams.HOURS, 1);
        assertEquals(String.valueOf(1), trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.set(QueryParams.HOURS, null);
        assertNull(trackMe.get(QueryParams.HOURS));
    }

    @Test
    public void testTrySet() {
        TrackMe trackMe = new TrackMe();
        trackMe.trySet(QueryParams.HOURS, "A");
        trackMe.trySet(QueryParams.HOURS, "B");
        assertEquals("A", trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.trySet(QueryParams.HOURS, 1f);
        trackMe.trySet(QueryParams.HOURS, 2f);
        assertEquals(String.valueOf(1f), trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.trySet(QueryParams.HOURS, 1L);
        trackMe.trySet(QueryParams.HOURS, 2L);
        assertEquals(String.valueOf(1L), trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.trySet(QueryParams.HOURS, 1);
        trackMe.trySet(QueryParams.HOURS, 2);
        assertEquals(String.valueOf(1), trackMe.get(QueryParams.HOURS));

        trackMe = new TrackMe();
        trackMe.trySet(QueryParams.HOURS, "A");
        trackMe.trySet(QueryParams.HOURS, null);
        assertNotNull(trackMe.get(QueryParams.HOURS));
    }

    @Test
    public void testSetAll() {
        TrackMe trackMe = new TrackMe();
        Map<QueryParams, String> testValues = new HashMap<>();
        for (QueryParams param : QueryParams.values()) {
            String testValue = UUID.randomUUID().toString();
            trackMe.set(param, testValue);
            testValues.put(param, testValue);
        }
        assertEquals(QueryParams.values().length, testValues.size());

        for (QueryParams param : QueryParams.values()) {
            assertTrue(trackMe.has(param));
            assertEquals(testValues.get(param), trackMe.get(param));
        }
        for (QueryParams param : QueryParams.values()) {
            trackMe.set(param, null);
            assertFalse(trackMe.has(param));
            assertNull(trackMe.get(param));
        }
    }
}
