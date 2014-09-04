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
    public void testPiwikApplicationGetTracker() throws Exception {
        PiwikApplication app = new TestPiwikApplication();
        assertEquals(app.getTracker(), app.getTracker());
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
        dummyTracker.trackScreenView("/test/test");
        Map<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertTrue(queryParams.get(Tracker.QueryParams.URL_PATH).endsWith("/test/test"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackScreenWithTitleView() throws Exception {
        dummyTracker.trackScreenView("test/test", "Test title");
        Map<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertTrue(queryParams.get(Tracker.QueryParams.URL_PATH).endsWith("/test/test"));
        assertEquals(queryParams.get(Tracker.QueryParams.ACTION_NAME), "Test title");
        validateDefaultQuery(queryParams);
    }

    private void checkEvent(Map<String, String> queryParams, String name, String value) {
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_CATEGORY), "category");
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_ACTION), "test action");
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_NAME), name);
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_VALUE), value);
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackEvent() throws Exception {
        dummyTracker.trackEvent("category", "test action");
        checkEvent(parseEventUrl(dummyTracker.getLastEvent()), null, null);
    }

    @Test
    public void testTrackEventName() throws Exception {
        String name = "test name2";
        dummyTracker.trackEvent("category", "test action", name);
        checkEvent(parseEventUrl(dummyTracker.getLastEvent()), name, null);
    }

    @Test
    public void testTrackEventNameAndValue() throws Exception {
        String name = "test name3";
        dummyTracker.trackEvent("category", "test action", name, 1);
        checkEvent(parseEventUrl(dummyTracker.getLastEvent()), name, "1");
    }

    @Test
    public void testTrackGoal() throws Exception {
        dummyTracker.trackGoal(1);
        Map<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertNull(queryParams.get(Tracker.QueryParams.REVENUE));
        assertEquals(queryParams.get(Tracker.QueryParams.GOAL_ID), "1");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalRevenue() throws Exception {
        dummyTracker.trackGoal(1, 100);
        Map<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertEquals(queryParams.get(Tracker.QueryParams.GOAL_ID), "1");
        assertEquals(queryParams.get(Tracker.QueryParams.REVENUE), "100");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalInvalidId() throws Exception {
        dummyTracker.trackGoal(-1, 100);
        assertNull(dummyTracker.getLastEvent());
    }

    private boolean checkNewAppDownload(Map<String, String> queryParams) {
        assertTrue(queryParams.get(Tracker.QueryParams.DOWNLOAD).length() > 0);
        assertTrue(queryParams.get(Tracker.QueryParams.URL_PATH).length() > 0);
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_CATEGORY), "Application");
        assertEquals(queryParams.get(Tracker.QueryParams.EVENT_ACTION), "downloaded");
        assertEquals(queryParams.get(Tracker.QueryParams.ACTION_NAME), "application/downloaded");
        validateDefaultQuery(queryParams);
        return true;
    }

    @Test
    public void testTrackAppDownload() throws Exception {
        dummyTracker.trackAppDownload();
        checkNewAppDownload(parseEventUrl(dummyTracker.getLastEvent()));

        dummyTracker.clearLastEvent();

        // track only once
        dummyTracker.trackAppDownload();
        assertNull(dummyTracker.getLastEvent());

    }

    @Test
    public void testTrackNewAppDownload() throws Exception {
        dummyTracker.trackNewAppDownload();
        checkNewAppDownload(parseEventUrl(dummyTracker.getLastEvent()));

        dummyTracker.clearLastEvent();

        dummyTracker.trackNewAppDownload();
        checkNewAppDownload(parseEventUrl(dummyTracker.getLastEvent()));
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
        String[] paths = new String[]{null, "", "/",};

        for (String path : paths) {
            dummyTracker.trackScreenView(path);
            assertEquals(dummyTracker.getParamUlr(), "http://org.piwik.sdk.test/");
        }
    }

    @Test
    public void testSetAPIUrl() throws Exception {
        try {
            dummyTracker.setAPIUrl(null);
        } catch (MalformedURLException e) {
            assertTrue(e.getMessage().contains("provide the Piwik Tracker URL!"));
        }

        String[] urls = new String[]{
                "https://demo.org/piwik/piwik.php",
                "https://demo.org/piwik/",
                "https://demo.org/piwik",
        };

        for (String url : urls) {
            dummyTracker.setAPIUrl(url);
            assertEquals(dummyTracker.getAPIUrl(), "https://demo.org/piwik/piwik.php");
        }

        dummyTracker.setAPIUrl("http://demo.org/piwik-proxy.php");
        assertEquals(dummyTracker.getAPIUrl(), "http://demo.org/piwik-proxy.php");
    }

    @Test
    public void testMd5() throws Exception {
        assertEquals(Tracker.md5("test"), "098f6bcd4621d373cade4e832627b4f6");
        assertNull(Tracker.md5(null));
    }
}