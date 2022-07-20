/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.tools.UrlHelper;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class EventTest extends BaseTest {
    @Test
    public void testhashCode() {
        Event event = new Event(0, "");
        assertEquals(0, event.hashCode());
    }

    @Test
    public void testEncoding_escaping() {
        Map<String, String> data = new HashMap<>();
        data.put(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES.toString(), "{\"1\":[\"2& ?\",\"3@#\"]}");
        Event event = new Event(data);
        assertEquals("?_cvar=%7B%221%22%3A%5B%222%26%20%3F%22%2C%223%40%23%22%5D%7D", event.getEncodedQuery());
    }


    @Test
    public void testBncoding_empty() {
        Map<String, String> data = new HashMap<>();
        Event event = new Event(data);
        assertEquals("", event.getEncodedQuery());
    }

    @Test
    public void testEncondingSingles() {
        for (QueryParams param : QueryParams.values()) {
            String testVal = UUID.randomUUID().toString();
            TrackMe trackMe = new TrackMe();
            trackMe.set(param, testVal);
            assertEquals("?" + param + "=" + testVal, new Event(trackMe.toMap()).getEncodedQuery());
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
        final Map<String, String> parsedParams = parseEncoding(new Event(trackMe.toMap()).getEncodedQuery());
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
