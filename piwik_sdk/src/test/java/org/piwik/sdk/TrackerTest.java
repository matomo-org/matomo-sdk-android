package org.piwik.sdk;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TrackerTest {
    static Tracker dummyTracker;
    static Piwik optOutedPiwik;

    @BeforeClass
    public static void initDummyTracker() throws Exception {
        optOutedPiwik = Piwik.getInstance(new TestPiwikApplication());
        optOutedPiwik.setDryRun(true);
        optOutedPiwik.setAppOptOut(true);
        dummyTracker = optOutedPiwik.newTracker("http://example.com", 1);
    }

    private static Map<String, String> parseEventUrl(String url) throws Exception {
        HashMap<String, String> values = new HashMap<String, String>(10);

        List<NameValuePair> params = URLEncodedUtils.parse(new URI("http://localhost/" + url), "UTF-8");

        for (NameValuePair param : params) {
            values.put(param.getName(), param.getValue());
        }

        return values;
    }

    private static void validateDefaultQuery(Map<String, String> params) {
        assertEquals(params.get(Tracker.QueryParams.SITE_ID), "1");
        assertEquals(params.get(Tracker.QueryParams.RECORD), "1");
        assertEquals(params.get(Tracker.QueryParams.VISITOR_ID).length(), 16);
        assertEquals(params.get(Tracker.QueryParams.LANGUAGE), "en");
        assertTrue(params.get(Tracker.QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(Tracker.QueryParams.RANDOM_NUMBER)) > 0);
    }

    @Test
    public void testEmptyQueueDispatch() throws Exception {
        assertFalse(Piwik.getInstance(new TestPiwikApplication()).newTracker("http://example.com", 1).dispatch());
    }

    @Test
    public void testSetDispatchInterval() throws Exception {

    }

    @Test
    public void testGetDispatchInterval() throws Exception {

    }

    @Test
    public void testGetDispatchIntervalMillis() throws Exception {

    }

    @Test
    public void testDispatchingCompleted() throws Exception {

    }

    @Test
    public void testDispatchingStarted() throws Exception {

    }

    @Test
    public void testIsDispatching() throws Exception {

    }

    @Test
    public void testSet() throws Exception {

    }

    @Test
    public void testSet1() throws Exception {

    }

    @Test
    public void testSetUserId() throws Exception {

    }

    @Test
    public void testSetUserId1() throws Exception {

    }

    @Test
    public void testGetResolution() throws Exception {

    }

    @Test
    public void testSetUserCustomVariable() throws Exception {

    }

    @Test
    public void testSetScreenCustomVariable() throws Exception {

    }

    @Test
    public void testSetNewSession() throws Exception {

    }

    @Test
    public void testSetSessionTimeout() throws Exception {

    }

    @Test
    public void testTrackScreenView() throws Exception {

    }

    @Test
    public void testTrackEvent() throws Exception {

    }

    @Test
    public void testTrackGoal() throws Exception {

    }

    @Test
    public void testTrackGoal1() throws Exception {

    }

    @Test
    public void testTrackAppDownload() throws Exception {

    }

    @Test
    public void testTrackException() throws Exception {
        dummyTracker.trackException("ClassName:10+2 2", "<Null> exception", false);
        Map<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_CATEGORY), "Exception");
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_ACTION), "ClassName:10+2 2");
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_NAME), "<Null> exception");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackUncaughtExceptionHandler() throws Exception {

        try {
            int _ = 1 / 0;
        }catch (Exception e){
            dummyTracker.customUEH.uncaughtException(Thread.currentThread(), e);
        }

        Map<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        validateDefaultQuery(queryParams);
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_CATEGORY), "Exception");
        assertTrue(queryParams.get(Tracker.QueryParams.EVENT_ACTION)
                .startsWith("org.piwik.sdk.TrackerTest/testTrackUncaughtExceptionHandler"));
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_NAME), "java.lang.ArithmeticException [/ by zero]");
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_VALUE), "0");
    }

    @Test
    public void testGetParamUlr() throws Exception {

    }

    @Test
    public void testSetAPIUrl() throws Exception {

    }

    @Test
    public void testMd5() throws Exception {

    }
}