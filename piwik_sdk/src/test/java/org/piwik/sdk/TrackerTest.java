package org.piwik.sdk;

import android.app.Application;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class TrackerTest {
    final static String testAPIUrl = "http://example.com";

    public Tracker createTracker() throws MalformedURLException {
        return Piwik.getInstance(Robolectric.application).newTracker(testAPIUrl, 1);
    }

    @Before
    public void setup() {
        Piwik.getInstance(Robolectric.application).setDryRun(true);
        Piwik.getInstance(Robolectric.application).setAppOptOut(true);
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
        Tracker tracker = createTracker();
        tracker.setDispatchInterval(1);
        assertEquals(tracker.getDispatchInterval(), 1);

    }

    @Test
    public void testGetDispatchIntervalMillis() throws Exception {
        Tracker tracker = createTracker();
        tracker.setDispatchInterval(1);
        assertEquals(tracker.getDispatchIntervalMillis(), 1000);
    }

    @Test
    public void testDispatchingFlow() throws Exception {
        Tracker tracker = createTracker();
        tracker.dispatchingStarted();
        assertTrue(tracker.isDispatching());
        tracker.dispatchingCompleted(1);
        assertFalse(tracker.isDispatching());
    }

    @Test
    public void testSet() throws Exception {
        Tracker tracker = createTracker();
        tracker.set(QueryParams.HOURS, "0")
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


        assertEquals(tracker.getQuery(), "?new_visit=1&h=0");
    }

    @Test
    public void testSetURL() throws Exception {
        Tracker tracker = createTracker();
        tracker.setApplicationDomain("test.com");
        assertEquals(tracker.getApplicationDomain(), "test.com");
        assertEquals(tracker.getApplicationBaseURL(), "http://test.com");
        assertEquals(tracker.getParamURL(), "http://test.com/");

        tracker.set(QueryParams.URL_PATH, "me");
        assertEquals(tracker.getParamURL(), "http://test.com/me");

        // override protocol
        tracker.set(QueryParams.URL_PATH, "https://my.com/secure");
        assertEquals(tracker.getParamURL(), "https://my.com/secure");
    }

    @Test
    public void testSetApplicationDomain() throws Exception {
        Tracker tracker = createTracker();
        tracker
                .setApplicationDomain("my-domain.com")
                .trackScreenView("test/test", "Test title");
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        validateDefaultQuery(queryParams);
        assertTrue(queryParams.get(QueryParams.URL_PATH).equals("http://my-domain.com/test/test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTooShortVistorId() throws MalformedURLException {
        Tracker tracker = createTracker();
        String tooShortVisitorId = "0123456789ab";
        tracker.setVisitorId(tooShortVisitorId);
        assertNotEquals(tooShortVisitorId, tracker.getVisitorId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTooLongVistorId() throws MalformedURLException {
        Tracker tracker = createTracker();
        String tooLongVisitorId = "0123456789abcdefghi";
        tracker.setVisitorId(tooLongVisitorId);
        assertNotEquals(tooLongVisitorId, tracker.getVisitorId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVistorIdWithInvalidCharacters() throws MalformedURLException {
        Tracker tracker = createTracker();
        String invalidCharacterVisitorId = "01234-6789-ghief";
        tracker.setVisitorId(invalidCharacterVisitorId);
        assertNotEquals(invalidCharacterVisitorId, tracker.getVisitorId());
    }

    @Test
    public void testSetVistorId() throws Exception {
        Tracker tracker = createTracker();
        String visitorId = "0123456789abcdef";
        tracker.setVisitorId(visitorId);
        assertEquals(visitorId, tracker.getVisitorId());
        tracker.beforeTracking();
        assertTrue(tracker.getQuery().contains("_id=" + visitorId));
    }

    @Test
    public void testSetUserId() throws Exception {
        Tracker tracker = createTracker();
        tracker.setUserId("test");
        assertEquals(tracker.getUserId(), "test");

        tracker.clearUserId();
        assertNull(tracker.getUserId());

        tracker.setUserId("");
        assertNull(tracker.getUserId());

        tracker.setUserId(null);
        assertNull(tracker.getUserId());

        tracker.setUserId("X98F6bcd4621d373");
        assertEquals(tracker.getUserId(), "X98F6bcd4621d373");
    }

    @Test
    public void testSetUserIdLong() throws Exception {
        Tracker tracker = createTracker();
        tracker.setUserId(123456);
        assertEquals(tracker.getUserId(), "123456");
    }

    @Test
    public void testGetResolution() throws Exception {
        Tracker tracker = createTracker();
        tracker.setResolution(100, 200);
        assertEquals(tracker.getQuery(), "?res=100x200&new_visit=1");
    }

    @Test
    public void testSetUserCustomVariable() throws Exception {
        Tracker tracker = createTracker();
        tracker.setUserCustomVariable(1, "2& ?", "3@#");
        tracker.trackScreenView("");

        String event = tracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals(queryParams.get("_cvar"), "{'1':['2& ?','3@#']}".replaceAll("'", "\""));
        // check url encoding
        assertTrue(event.contains("_cvar=%7B%221%22%3A%5B%222%26%20%3F%22%2C%223%40%23%22%5D%7D"));
    }

    @Test
    public void testSetScreenCustomVariable() throws Exception {
        Tracker tracker = createTracker();
        tracker.setScreenCustomVariable(1, "2", "3");
        tracker.trackScreenView("");

        String event = tracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals(queryParams.get("cvar"), "{'1':['2','3']}".replaceAll("'", "\""));

    }

    @Test
    public void testSetNewSession() throws Exception {
        Tracker tracker = createTracker();

        assertEquals(tracker.getQuery(), "?new_visit=1");

        tracker.trackScreenView("");
        assertEquals(tracker.getQuery(), "");

        tracker.trackScreenView("");
        assertEquals(tracker.getQuery(), "");

        tracker.setNewSession();
        assertEquals(tracker.getQuery(), "?new_visit=1");
    }

    @Test
    public void testSetSessionTimeout() throws Exception {
        Tracker tracker = createTracker();

        tracker.setSessionTimeout(10);
        assertFalse(tracker.isExpired());

        tracker.setSessionTimeout(0);
        Thread.sleep(1, 0);
        assertTrue(tracker.isExpired());

        tracker.setSessionTimeout(10);
        assertFalse(tracker.isExpired());

    }

    @Test
    public void testCheckSessionTimeout() throws Exception {
        Tracker tracker = createTracker();
        tracker.setSessionTimeout(0);

        assertEquals(tracker.getQuery(), "?new_visit=1");
        tracker.afterTracking();
        Thread.sleep(1, 0);
        tracker.checkSessionTimeout();

        assertEquals(tracker.getQuery(), "?new_visit=1");

    }

    @Test
    public void testTrackScreenView() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackScreenView("/test/test");
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertTrue(queryParams.get(QueryParams.URL_PATH).endsWith("/test/test"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackScreenWithTitleView() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackScreenView("test/test", "Test title");
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

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
        Tracker tracker = createTracker();
        tracker.trackEvent("category", "test action");
        checkEvent(parseEventUrl(tracker.getLastEvent()), null, null);
    }

    @Test
    public void testTrackEventName() throws Exception {
        Tracker tracker = createTracker();
        String name = "test name2";
        tracker.trackEvent("category", "test action", name);
        checkEvent(parseEventUrl(tracker.getLastEvent()), name, null);
    }

    @Test
    public void testTrackEventNameAndValue() throws Exception {
        Tracker tracker = createTracker();
        String name = "test name3";
        tracker.trackEvent("category", "test action", name, 1);
        checkEvent(parseEventUrl(tracker.getLastEvent()), name, "1");
    }

    @Test
    public void testTrackGoal() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackGoal(1);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertNull(queryParams.get(QueryParams.REVENUE));
        assertEquals(queryParams.get(QueryParams.GOAL_ID), "1");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalRevenue() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackGoal(1, 100);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.GOAL_ID), "1");
        assertEquals(queryParams.get(QueryParams.REVENUE), "100");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalInvalidId() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackGoal(-1, 100);
        assertNull(tracker.getLastEvent());
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
        Tracker tracker = createTracker();
        tracker.trackAppDownload();
        checkNewAppDownload(parseEventUrl(tracker.getLastEvent()));

        tracker.clearLastEvent();

        // track only once
        tracker.trackAppDownload();
        assertNull(tracker.getLastEvent());

    }

    @Test
    public void testTrackNewAppDownload() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackNewAppDownload();
        checkNewAppDownload(parseEventUrl(tracker.getLastEvent()));

        tracker.clearLastEvent();

        tracker.trackNewAppDownload();
        checkNewAppDownload(parseEventUrl(tracker.getLastEvent()));
    }

    @Test
    public void testTrackContentImpression() throws Exception {
        Tracker tracker = createTracker();
        String name = "test name2";
        tracker.trackContentImpression(name, "test", "test2");
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.CONTENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(queryParams.get(QueryParams.CONTENT_TARGET), "test2");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackContentInteraction() throws Exception {
        Tracker tracker = createTracker();
        String interaction = "interaction";
        String name = "test name2";
        tracker.trackContentInteraction(interaction, name, "test", "test2");

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.CONTENT_INTERACTION), interaction);
        assertEquals(queryParams.get(QueryParams.CONTENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(queryParams.get(QueryParams.CONTENT_TARGET), "test2");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackException() throws Exception {
        Tracker tracker = createTracker();
        Exception catchedException;
        try {
            throw new Exception("Test");
        } catch (Exception e) {
            catchedException = e;
        }
        assertNotNull(catchedException);
        tracker.trackException(catchedException, "<Null> exception", false);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Exception");
        StackTraceElement traceElement = catchedException.getStackTrace()[0];
        assertNotNull(traceElement);
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "org.piwik.sdk.TrackerTest" + "/" + "testTrackException" + ":" + traceElement.getLineNumber());
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), "<Null> exception");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testGetParamUlr() throws Exception {
        Tracker tracker = createTracker();
        String[] paths = new String[]{null, "", "/",};

        for (String path : paths) {
            tracker.trackScreenView(path);
            assertEquals(tracker.getParamURL(), "http://org.piwik.sdk.test/");
        }
    }

    @Test
    public void testSetAPIUrl() throws Exception {
        Tracker tracker = createTracker();
        try {
            tracker.setAPIUrl(null);
        } catch (MalformedURLException e) {
            assertTrue(e.getMessage().contains("provide the Piwik Tracker URL!"));
        }

        String[] urls = new String[]{
                "https://demo.org/piwik/piwik.php",
                "https://demo.org/piwik/",
                "https://demo.org/piwik",
        };

        for (String url : urls) {
            tracker.setAPIUrl(url);
            assertEquals(tracker.getAPIUrl(), "https://demo.org/piwik/piwik.php");
        }

        tracker.setAPIUrl("http://demo.org/piwik-proxy.php");
        assertEquals(tracker.getAPIUrl(), "http://demo.org/piwik-proxy.php");
    }

    @Test
    public void testSetUserAgent() throws MalformedURLException {
        Tracker tracker = createTracker();
        String defaultUserAgent = "aUserAgent";
        String customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0";
        System.setProperty("http.agent", "aUserAgent");

        assertEquals(tracker.getUserAgent(), defaultUserAgent);

        tracker.setUserAgent(customUserAgent);
        assertEquals(tracker.getUserAgent(), customUserAgent);

        tracker.setUserAgent(null);
        assertEquals(tracker.getUserAgent(), defaultUserAgent);
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

}