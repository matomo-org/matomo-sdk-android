/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.util.Pair;

import org.junit.Test;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.tools.UrlHelper;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class EventTest {
    @Test
    public void testhashCode() {
        Event event = new Event(0, "");
        assertEquals(0, event.hashCode());
    }

    @Test
    public void testEncoding_escaping() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES.toString(), "{\"1\":[\"2& ?\",\"3@#\"]}");
        Event event = new Event(data);
        assertEquals("?_cvar=%7B%221%22%3A%5B%222%26%20%3F%22%2C%223%40%23%22%5D%7D", event.getEncodedQuery());
    }


    @Test
    public void testBncoding_empty() throws Exception {
        Map<String, String> data = new HashMap<>();
        Event event = new Event(data);
        assertEquals("", event.getEncodedQuery());
    }

    @Test
    public void testEncondingSingles() throws Exception {
        for (QueryParams param : QueryParams.values()) {
            String testVal = UUID.randomUUID().toString();
            TrackMe trackMe = new TrackMe();
            trackMe.set(param, testVal);
            assertEquals("?" + param.toString() + "=" + testVal, new Event(trackMe.toMap()).getEncodedQuery());
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