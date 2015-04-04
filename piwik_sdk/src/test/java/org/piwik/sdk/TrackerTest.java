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
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class TrackerTest {

    public Tracker createTracker() throws MalformedURLException {
        TestPiwikApplication app = (TestPiwikApplication) Robolectric.application;
        return Piwik.getInstance(Robolectric.application).newTracker(app.getTrackerUrl(), app.getSiteId());
    }

    public Piwik getPiwik() {
        return Piwik.getInstance(Robolectric.application);
    }

    @Before
    public void setup() {
        Piwik.getInstance(Robolectric.application).setDryRun(true);
        Piwik.getInstance(Robolectric.application).setOptOut(true);
    }

    @Test
    public void testPiwikAutoBindActivities() throws Exception {
        Application app = Robolectric.application;
        Piwik piwik = Piwik.getInstance(app);
        piwik.setDryRun(true);
        piwik.setOptOut(true);
        Tracker tracker = createTracker();
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
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getTracker(), piwikApplication.getTracker());
    }

    @Test
    public void testPiwikApplicationNewTracker() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getTracker(), piwikApplication.getTracker());
        assertEquals(piwikApplication.getTracker(), piwikApplication.newTracker());
        Tracker manual = Piwik.getInstance(Robolectric.application).newTracker(piwikApplication.getTrackerUrl(), piwikApplication.getSiteId());
        Tracker simplified = piwikApplication.newTracker();
        assertEquals(manual.getAPIUrl(), simplified.getAPIUrl());
        assertEquals(manual.getSiteId(), simplified.getSiteId());
    }

    @Test
    public void testPiwikApplicationgetPiwik() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getPiwik(), Piwik.getInstance(piwikApplication));
    }

    @Test
    public void testEmptyQueueDispatch() throws Exception {
        assertFalse(createTracker().dispatch());
    }

    @Test
    public void testSetDispatchInterval() throws Exception {
        Tracker tracker = createTracker();
        tracker.setDispatchInterval(1);
        assertEquals(tracker.getDispatchInterval(), 1);
    }

    @Test
    public void testSet() throws Exception {
        TrackMe trackMe = new TrackMe().set(QueryParams.HOURS, "0")
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
        assertEquals("?h=0", trackMe.build());
    }

    @Test
    public void testSetURL() throws Exception {
        Tracker tracker = createTracker();
        tracker.setApplicationDomain("test.com");
        assertEquals(tracker.getApplicationDomain(), "test.com");
        assertEquals(tracker.getApplicationBaseURL(), "http://test.com");
        TrackMe trackMe = new TrackMe();
        tracker.doInjections(trackMe);
        assertEquals("http://test.com/", trackMe.get(QueryParams.URL_PATH));

        trackMe.set(QueryParams.URL_PATH, "me");
        tracker.doInjections(trackMe);
        assertEquals("http://test.com/me", trackMe.get(QueryParams.URL_PATH));

        // override protocol
        trackMe.set(QueryParams.URL_PATH, "https://my.com/secure");
        tracker.doInjections(trackMe);
        assertEquals("https://my.com/secure", trackMe.get(QueryParams.URL_PATH));
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
        TrackMe trackMe = new TrackMe();
        tracker.doInjections(trackMe);
        assertTrue(trackMe.build().contains("_id=" + visitorId));
    }

    @Test
    public void testSetUserId() throws Exception {
        Tracker tracker = createTracker();
        tracker.setUserId("test");
        assertEquals(tracker.getUserId(), "test");

        tracker.setUserId("");
        assertEquals(tracker.getUserId(), "test");

        tracker.setUserId(null);
        assertNotEquals("test", tracker.getUserId());
        assertNotNull(tracker.getUserId());

        String uuid = UUID.randomUUID().toString();
        tracker.setUserId(uuid);
        assertEquals(uuid, tracker.getUserId());

        assertEquals(uuid, createTracker().getUserId());
    }

    @Test
    public void testGetResolution() throws Exception {
        Tracker tracker = createTracker();
        TrackMe trackMe = new TrackMe();
        tracker.doInjections(trackMe);
        assertTrue(trackMe.build().contains("res=480x800"));
    }

    @Test
    public void testSetUserCustomVariable() throws Exception {
        Tracker tracker = createTracker();
        tracker.setVisitCustomVariable(1, "2& ?", "3@#");
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
        TrackMe trackMe = new TrackMe();
        trackMe.setScreenCustomVariable(1, "2", "3");
        tracker.trackScreenView(trackMe, "", null);

        String event = tracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals("{'1':['2','3']}".replaceAll("'", "\""), queryParams.get("cvar"));
    }

    @Test
    public void testSetNewSession() throws Exception {
        Tracker tracker = createTracker();
        TrackMe trackMe = new TrackMe();
        tracker.doInjections(trackMe);
        assertTrue(trackMe.build().contains("new_visit=1"));

        tracker.trackScreenView("");
        assertFalse(tracker.getLastEvent().contains("new_visit=1"));

        tracker.trackScreenView("");
        assertFalse(tracker.getLastEvent().contains("new_visit=1"));

        tracker.startNewSession();
        tracker.trackScreenView("");
        assertTrue(trackMe.build().contains("new_visit=1"));
    }

    @Test
    public void testSetSessionTimeout() throws Exception {
        Tracker tracker = createTracker();

        tracker.setSessionTimeout(10000);
        tracker.trackScreenView("test");
        assertFalse(tracker.isSessionExpired());

        tracker.setSessionTimeout(0);
        Thread.sleep(1, 0);
        assertTrue(tracker.isSessionExpired());

        tracker.setSessionTimeout(10000);
        assertFalse(tracker.isSessionExpired());

    }

    @Test
    public void testCheckSessionTimeout() throws Exception {
        Tracker tracker = createTracker();
        tracker.setSessionTimeout(0);
        tracker.trackScreenView("test");
        assertTrue(tracker.getLastEvent().contains("new_visit=1"));
        Thread.sleep(1, 0);
        tracker.trackScreenView("test");
        assertTrue(tracker.getLastEvent().contains("new_visit=1"));
        tracker.setSessionTimeout(60000);
        tracker.trackScreenView("test");
        assertFalse(tracker.getLastEvent().contains("new_visit=1"));
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

    private final Pattern REGEX_DOWNLOADTRACK = Pattern.compile("(?:https?:\\/\\/)([\\w.]+)(?::)([\\d]+)(?:\\/)([\\W\\w]+)");

    @Test
    public void testTrackNewAppDownload() throws Exception {
        Tracker tracker = createTracker();
        tracker.trackNewAppDownload(Robolectric.application, Tracker.ExtraIdentifier.APK_CHECKSUM);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        Matcher m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(TestPiwikApplication.PACKAGENAME, m.group(1));
        assertEquals(TestPiwikApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(TestPiwikApplication.FAKE_APK_DATA_MD5, m.group(3));

        tracker.clearLastEvent();

        tracker.trackNewAppDownload(Robolectric.application, Tracker.ExtraIdentifier.INSTALLER_PACKAGENAME);
        queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(TestPiwikApplication.PACKAGENAME, m.group(1));
        assertEquals(TestPiwikApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(TestPiwikApplication.INSTALLER_PACKAGENAME, m.group(3));

        tracker.clearLastEvent();

        FullEnvPackageManager pm = (FullEnvPackageManager) Robolectric.packageManager;
        pm.getInstallerMap().clear();
        tracker.trackNewAppDownload(Robolectric.application, Tracker.ExtraIdentifier.INSTALLER_PACKAGENAME);
        queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(TestPiwikApplication.PACKAGENAME, m.group(1));
        assertEquals(TestPiwikApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals("unknown", m.group(3));
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
    public void testUrlPathCorrection() throws Exception {
        Tracker tracker = createTracker();
        String[] paths = new String[]{null, "", "/",};
        for (String path : paths) {
            TrackMe trackMe = new TrackMe();
            trackMe.set(QueryParams.URL_PATH, path);
            tracker.doInjections(trackMe);
            assertEquals("http://org.piwik.sdk.test/", trackMe.get(QueryParams.URL_PATH));
        }
    }

    @Test
    public void testSetAPIUrl() throws Exception {
        try {
            getPiwik().newTracker(null, 1);
            assert false;
        } catch (MalformedURLException e) {
            assertTrue(e.getMessage().contains("provide the Piwik Tracker URL!"));
        }

        String[] urls = new String[]{
                "https://demo.org/piwik/piwik.php",
                "https://demo.org/piwik/",
                "https://demo.org/piwik",
        };

        for (String url : urls) {
            assertEquals(getPiwik().newTracker(url, 1).getAPIUrl().toString(), "https://demo.org/piwik/piwik.php");
        }

        assertEquals(getPiwik().newTracker("http://demo.org/piwik-proxy.php", 1).getAPIUrl(), new URL("http://demo.org/piwik-proxy.php"));
    }

    @Test
    public void testSetUserAgent() throws MalformedURLException {
        Tracker tracker = createTracker();
        String defaultUserAgent = "aUserAgent";
        String customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0";
        System.setProperty("http.agent", "aUserAgent");

        TrackMe trackMe = new TrackMe();
        tracker.doInjections(trackMe);
        assertEquals(trackMe.get(QueryParams.USER_AGENT), defaultUserAgent);

        trackMe.set(QueryParams.USER_AGENT, customUserAgent);
        tracker.doInjections(trackMe);
        assertEquals(trackMe.get(QueryParams.USER_AGENT), customUserAgent);

        trackMe.remove(QueryParams.USER_AGENT);
        tracker.doInjections(trackMe);
        assertEquals(trackMe.get(QueryParams.USER_AGENT), defaultUserAgent);

        tracker.getDefaultTrackMe().remove(QueryParams.USER_AGENT);
        tracker.doInjections(trackMe);
        assertEquals(trackMe.get(QueryParams.USER_AGENT), null);
    }

    private static class QueryHashMap<String, V> extends HashMap<String, V> {

        private QueryHashMap() {
            super(10);
        }

        public V get(QueryParams key) {
            return get(key.toString());
        }
    }

    @SuppressWarnings("deprecation")
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
        assertTrue(params.get(QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(QueryParams.RANDOM_NUMBER)) > 0);
    }
}