/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class QuickTrackTest {
    static Tracker dummyTracker;
    static Piwik dummyPiwik;
    static TestPiwikApplication dummyApp;
    final static String testAPIUrl = "http://example.com";

    @BeforeClass
    public static void initDummyTracker() throws Exception {
        dummyApp = new TestPiwikApplication();
        dummyPiwik = Piwik.getInstance(dummyApp);
        dummyTracker = createNewTracker();
    }

    private static Tracker createNewTracker() throws MalformedURLException {
        return dummyPiwik.newTracker(testAPIUrl, 1);
    }

    @Before
    public void clearTracker() throws Exception {
        dummyApp.clearSharedPreferences();
        dummyPiwik.setDryRun(true);
        dummyPiwik.setAppOptOut(true);
        dummyTracker.afterTracking();
        dummyTracker.clearLastEvent();
        dummyTracker.setAPIUrl(testAPIUrl);
        dummyTracker.setApplicationDomain(null);
    }

    private static class QueryHashMap<String, V> extends HashMap<String, V> {

        private QueryHashMap() {
            super(10);
        }

        public V get(QueryParams key) {
            return get(key.toString());
        }
    }

    private static QueryHashMap<String, String> parseEventUrl(String url) throws Exception {
        QueryHashMap<String, String> values = new QueryHashMap<String, String>();

        List<NameValuePair> params = URLEncodedUtils.parse(new URI("http://localhost/" + url), "UTF-8");

        for (NameValuePair param : params) {
            values.put(param.getName(), param.getValue());
        }

        return values;
    }

    private static void validateDefaultQuery(QueryHashMap<String, String> params) {
        assertEquals(params.get(QueryParams.SITE_ID), "1");
        assertEquals(params.get(QueryParams.RECORD), "1");
        assertEquals(params.get(QueryParams.SEND_IMAGE), "0");
        assertEquals(params.get(QueryParams.VISITOR_ID).length(), 16);
        assertEquals(params.get(QueryParams.LANGUAGE), "en");
        assertTrue(params.get(QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(QueryParams.RANDOM_NUMBER)) > 0);
    }

    @Test
    public void testPiwikExceptionHandler() throws Exception {
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler);
        QuickTrack.trackUncaughtExceptions(dummyTracker);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler);
        try {
            int i = 1 / 0;
            assertNotEquals(i, 0);
        } catch (Exception e) {
            (Thread.getDefaultUncaughtExceptionHandler()).uncaughtException(Thread.currentThread(), e);
        }
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());
        validateDefaultQuery(queryParams);
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Exception");
        assertTrue(queryParams.get(QueryParams.EVENT_ACTION)
                .startsWith("org.piwik.sdk.QuickTrackTest/testPiwikExceptionHandler:"));
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), "/ by zero");
        assertEquals(queryParams.get(QueryParams.EVENT_VALUE), "1");
    }

}