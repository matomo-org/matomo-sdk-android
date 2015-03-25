package org.piwik.sdk;

import android.app.Application;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
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
    public void testPiwikAutoBindActivities() throws Exception {
        Application app = Robolectric.application;
        Piwik piwik = Piwik.getInstance(app);
        piwik.setDryRun(true);
        piwik.setAppOptOut(true);
        Tracker tracker = piwik.newTracker(testAPIUrl, 1);
        //auto attach tracking screen view
        QuickTrack.bindToApp(app, tracker);

        // emulate default trackScreenView
        Robolectric.buildActivity(TestActivity.class).create().start().resume().visible().get();

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        validateDefaultQuery(queryParams);
        assertEquals(queryParams.get(QueryParams.ACTION_NAME), TestActivity.getTestTitle());
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
        dummyTracker.set(QueryParams.HOURS, "0")
                .set(QueryParams.MINUTES, (Integer) null)
                .set(QueryParams.SECONDS, (String) null)
                .set(QueryParams.FIRST_VISIT_TIMESTAMP, (String) null)
                .set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, (String) null)
                .set(QueryParams.TOTAL_NUMBER_OF_VISITS, (String) null)
                .set(QueryParams.GOAL_ID, (String) null)
                .set(QueryParams.LATITUDE, (String) null)
                .set(QueryParams.LONGITUDE, (String) null)
                .set(QueryParams.SEARCH_KEYWORD, (String) null)
                .set(QueryParams.SEARCH_CATEGORY, (String) null)
                .set(QueryParams.SEARCH_NUMBER_OF_HITS, (String) null)
                .set(QueryParams.REFERRER, (String) null)
                .set(QueryParams.CAMPAIGN_NAME, (String) null)
                .set(QueryParams.CAMPAIGN_KEYWORD, (String) null);

        assertEquals(dummyTracker.getQuery(), "?h=0");
    }

    @Test
    public void testSetURL() throws Exception {
        dummyTracker.setApplicationDomain("test.com");
        assertEquals(dummyTracker.getApplicationDomain(), "test.com");
        assertEquals(dummyTracker.getApplicationBaseURL(), "http://test.com");
        assertEquals(dummyTracker.getParamURL(), "http://test.com/");

        dummyTracker.set(QueryParams.URL_PATH, "me");
        assertEquals(dummyTracker.getParamURL(), "http://test.com/me");

        // override protocol
        dummyTracker.set(QueryParams.URL_PATH, "https://my.com/secure");
        assertEquals(dummyTracker.getParamURL(), "https://my.com/secure");
    }

    @Test
    public void testSetApplicationDomain() throws Exception {
        dummyTracker
                .setApplicationDomain("my-domain.com")
                .trackScreenView("test/test", "Test title");
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        validateDefaultQuery(queryParams);
        assertTrue(queryParams.get(QueryParams.URL_PATH).equals("http://my-domain.com/test/test"));
    }

    @Test(expected=IllegalArgumentException.class) 
    public void testSetTooShortVistorId() {
        String tooShortVisitorId = "0123456789ab";
        dummyTracker.setVisitorId(tooShortVisitorId);
        assertNotEquals(tooShortVisitorId, dummyTracker.getVisitorId());
    }

    @Test(expected=IllegalArgumentException.class) 
    public void testSetTooLongVistorId() {
        String tooLongVisitorId = "0123456789abcdefghi";
        dummyTracker.setVisitorId(tooLongVisitorId);
        assertNotEquals(tooLongVisitorId, dummyTracker.getVisitorId());
    }

    @Test(expected=IllegalArgumentException.class) 
    public void testSetVistorIdWithInvalidCharacters() {
        String invalidCharacterVisitorId = "01234-6789-ghief";
        dummyTracker.setVisitorId(invalidCharacterVisitorId);
        assertNotEquals(invalidCharacterVisitorId, dummyTracker.getVisitorId());
    }

    @Test
    public void testSetVistorId() throws Exception {
        String visitorId = "0123456789abcdef";
        dummyTracker.setVisitorId(visitorId);
        assertEquals(visitorId, dummyTracker.getVisitorId());
        dummyTracker.beforeTracking();
        assertTrue(dummyTracker.getQuery().contains("_id=" + visitorId));
    }

    @Test
    public void testSetUserId() throws Exception {
        dummyTracker.setUserId("test");
        assertEquals(dummyTracker.getUserId(), "test");

        dummyTracker.clearUserId();
        assertNull(dummyTracker.getUserId());

        dummyTracker.setUserId("");
        assertNull(dummyTracker.getUserId());

        dummyTracker.setUserId(null);
        assertNull(dummyTracker.getUserId());

        dummyTracker.setUserId("X98F6bcd4621d373");
        assertEquals(dummyTracker.getUserId(), "X98F6bcd4621d373");
    }

    @Test
    public void testSetUserIdLong() throws Exception {
        dummyTracker.setUserId(123456);
        assertEquals(dummyTracker.getUserId(), "123456");
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
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertTrue(queryParams.get(QueryParams.URL_PATH).endsWith("/test/test"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackScreenWithTitleView() throws Exception {
        dummyTracker.trackScreenView("test/test", "Test title");
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertTrue(queryParams.get(QueryParams.URL_PATH).endsWith("/test/test"));
        assertEquals(queryParams.get(QueryParams.ACTION_NAME), "Test title");
        validateDefaultQuery(queryParams);
    }

    private void checkEvent(QueryHashMap<String, String> queryParams, String name, String value) {
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.EVENT_VALUE), value);
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
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertNull(queryParams.get(QueryParams.REVENUE));
        assertEquals(queryParams.get(QueryParams.GOAL_ID), "1");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalRevenue() throws Exception {
        dummyTracker.trackGoal(1, 100);
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.GOAL_ID), "1");
        assertEquals(queryParams.get(QueryParams.REVENUE), "100");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalInvalidId() throws Exception {
        dummyTracker.trackGoal(-1, 100);
        assertNull(dummyTracker.getLastEvent());
    }

    private boolean checkNewAppDownload(QueryHashMap<String, String> queryParams) {
        assertTrue(queryParams.get(QueryParams.DOWNLOAD).length() > 0);
        assertTrue(queryParams.get(QueryParams.URL_PATH).length() > 0);
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Application");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "downloaded");
        assertEquals(queryParams.get(QueryParams.ACTION_NAME), "application/downloaded");
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
    public void testTrackContentImpression() throws Exception {
        String name = "test name2";
        dummyTracker.trackContentImpression(name, "test", "test2");
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.CONTENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(queryParams.get(QueryParams.CONTENT_TARGET), "test2");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackContentInteraction() throws Exception {
        String interaction = "interaction";
        String name = "test name2";
        dummyTracker.trackContentInteraction(interaction, name, "test", "test2");

        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.CONTENT_INTERACTION), interaction);
        assertEquals(queryParams.get(QueryParams.CONTENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(queryParams.get(QueryParams.CONTENT_TARGET), "test2");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackException() throws Exception {
        dummyTracker.trackException("ClassName:10+2 2", "<Null> exception", false);
        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Exception");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "ClassName:10+2 2");
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), "<Null> exception");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackUncaughtExceptionHandler() throws Exception {

        try {
            //noinspection NumericOverflow
            int i = 1 / 0;
            assertNotEquals(i, 0);
        } catch (Exception e) {
            dummyTracker.customUEH.uncaughtException(Thread.currentThread(), e);
        }

        QueryHashMap<String, String> queryParams = parseEventUrl(dummyTracker.getLastEvent());

        validateDefaultQuery(queryParams);
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Exception");
        assertTrue(queryParams.get(QueryParams.EVENT_ACTION)
                .startsWith("org.piwik.sdk.TrackerTest/testTrackUncaughtExceptionHandler"));
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), "/ by zero");
        assertEquals(queryParams.get(QueryParams.EVENT_VALUE), "1");
    }

    @Test
    public void testGetParamUlr() throws Exception {
        String[] paths = new String[]{null, "", "/",};

        for (String path : paths) {
            dummyTracker.trackScreenView(path);
            assertEquals(dummyTracker.getParamURL(), "http://org.piwik.sdk.test/");
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
    public void testSetUserAgent() {
        String defaultUserAgent = "aUserAgent";
        String customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0";
        System.setProperty("http.agent", "aUserAgent");

        assertEquals(dummyTracker.getUserAgent(), defaultUserAgent);
        
        dummyTracker.setUserAgent(customUserAgent);
        assertEquals(dummyTracker.getUserAgent(), customUserAgent);

        dummyTracker.setUserAgent(null);
        assertEquals(dummyTracker.getUserAgent(), defaultUserAgent);
    }

}