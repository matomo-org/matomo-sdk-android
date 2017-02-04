package org.piwik.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class TrackMeTest extends DefaultTestCase {
    @Test
    public void testSourcingFromOtherTrackMe() throws Exception {
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
    public void testSet() throws Exception {
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
    public void testTrySet() throws Exception {
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
    public void testSetAll() throws Exception {
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
