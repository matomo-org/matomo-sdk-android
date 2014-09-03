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
        dummyTracker = createNewTracker();
    }

    private static Tracker createNewTracker() throws MalformedURLException {
        return optOutedPiwik.newTracker("http://example.com", 1);
    }

    @Before
    public void clearTracker() throws Exception {
        dummyTracker.afterTracking();
        optOutedPiwik.setDryRun(true);
        optOutedPiwik.setAppOptOut(true);
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
        dummyTracker.setDispatchInterval(1);
        assertEquals(dummyTracker.getDispatchInterval(), 1);

    }

    @Test
    public void testGetDispatchIntervalMillis() throws Exception {
        dummyTracker.setDispatchInterval(1);
        assertEquals(dummyTracker.getDispatchIntervalMillis(), 1000);
    }

    @Test
    public void testDispatchingFlow() throws Exception {
        dummyTracker.dispatchingStarted();
        assertTrue(dummyTracker.isDispatching());
        dummyTracker.dispatchingCompleted(1);
        assertFalse(dummyTracker.isDispatching());
    }

    @Test
    public void testSet() throws Exception {
        dummyTracker.set("a", "b")
                .set("b", (Integer) null)
                .set("c", (String) null);
        assertEquals(dummyTracker.getQuery(), "?a=b");
    }

    @Test
    public void testSetUserId() throws Exception {
        dummyTracker.setUserId("test");
        assertEquals(dummyTracker.getUserId(), "098f6bcd4621d373");

        dummyTracker.setUserId("098F6bcd4621d373");
        dummyTracker.setUserId(null);
        assertEquals(dummyTracker.getUserId(), "098F6bcd4621d373");

        dummyTracker.setUserId("X98F6bcd4621d373");
        assertNotEquals(dummyTracker.getUserId(), "X98F6bcd4621d373");
    }

    @Test
    public void testSetUserIdLong() throws Exception {
        dummyTracker.setUserId(123456);
        assertNotEquals(dummyTracker.getUserId(), "123456");
    }

    @Test
    public void testGetResolution() throws Exception {
        dummyTracker.setResolution(100, 200);
        assertEquals(dummyTracker.getQuery(), "?res=100x200");
    }

    @Test
    public void testSetUserCustomVariable() throws Exception {
        dummyTracker.setUserCustomVariable(1, "2& ?", "3@#");
        dummyTracker.trackScreenView("");

        String event = dummyTracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals(queryParams.get("_cvar"), "{'1':['2& ?','3@#']}".replaceAll("'", "\""));
        // check url encoding
        assertTrue(event.contains("_cvar=%7B%221%22%3A%5B%222%26%20%3F%22%2C%223%40%23%22%5D%7D"));
    }

    @Test
    public void testSetScreenCustomVariable() throws Exception {
        dummyTracker.setScreenCustomVariable(1, "2", "3");
        dummyTracker.trackScreenView("");

        String event = dummyTracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals(queryParams.get("cvar"), "{'1':['2','3']}".replaceAll("'", "\""));

    }

    @Test
    public void testSetNewSession() throws Exception {
        Tracker newTracker = createNewTracker();

        assertEquals(newTracker.getQuery(), "?new_visit=1");

        newTracker.trackScreenView("");
        assertEquals(newTracker.getQuery(), "");

        newTracker.trackScreenView("");
        assertEquals(newTracker.getQuery(), "");

        newTracker.setNewSession();
        assertEquals(newTracker.getQuery(), "?new_visit=1");
    }

    @Test
    public void testSetSessionTimeout() throws Exception {
        Tracker newTracker = createNewTracker();

        newTracker.setSessionTimeout(10);
        assertFalse(newTracker.isExpired());

        newTracker.setSessionTimeout(0);
        Thread.sleep(1, 0);
        assertTrue(newTracker.isExpired());

        newTracker.setSessionTimeout(10);
        assertFalse(newTracker.isExpired());

    }

    @Test
    public void testCheckSessionTimeout() throws Exception {
        Tracker newTracker = createNewTracker();
        newTracker.setSessionTimeout(0);

        assertEquals(newTracker.getQuery(), "?new_visit=1");
        newTracker.afterTracking();
        Thread.sleep(1, 0);
        newTracker.checkSessionTimeout();

        assertEquals(newTracker.getQuery(), "?new_visit=1");

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
        } catch (Exception e) {
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