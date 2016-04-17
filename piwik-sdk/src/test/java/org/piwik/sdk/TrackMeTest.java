package org.piwik.sdk;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.dispatcher.Dispatcher;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.tools.UrlHelper;
import org.robolectric.annotation.Config;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test
    public void testEncondingSingles() throws Exception {
        for (QueryParams param : QueryParams.values()) {
            String testVal = UUID.randomUUID().toString();
            TrackMe trackMe = new TrackMe();
            trackMe.set(param, testVal);
            assertEquals("?" + param.toString() + "=" + testVal, Dispatcher.urlEncodeUTF8(trackMe.toMap()));
        }
    }

    @Test
    public void testEncodingMultiples() throws Exception {
        TrackMe trackMe = new TrackMe();
        Map<String, String> testValues = new HashMap<>();
        for (QueryParams param : QueryParams.values()) {
            String testVal = UUID.randomUUID().toString();
            trackMe.set(param, testVal);
            testValues.put(param.toString(), testVal);
        }
        final Map<String, String> parsedParams = parseEncoding(Dispatcher.urlEncodeUTF8(trackMe.toMap()));
        for (Map.Entry<String, String> pair : parsedParams.entrySet()) {
            assertEquals(testValues.get(pair.getKey()), pair.getValue());
        }
    }

    private static Map<String, String> parseEncoding(String url) throws Exception {
        Map<String, String> values = new HashMap<>();
        List<Pair<String, String>> params = UrlHelper.parse(new URI("http://localhost/" + url), "UTF-8");
        for (Pair<String, String> param : params) values.put(param.first, param.second);
        return values;
    }

}
