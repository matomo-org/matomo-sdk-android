package org.piwik.sdk;

import android.app.Application;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.ecommerce.EcommerceItems;
import org.piwik.sdk.plugins.CustomDimensions;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
        Piwik.getInstance(Robolectric.application).getSharedPreferences().edit().clear().apply();
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
    public void testGetSiteId() throws Exception {
        assertEquals(createTracker().getSiteId(), 1);
    }

    @Test
    public void testGetPiwik() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getPiwik(), Piwik.getInstance(piwikApplication));
    }

    @Test
    public void testSet() throws Exception {
        TrackMe trackMe = new TrackMe().set(QueryParams.HOURS, "0")
                .set(QueryParams.MINUTES, null)
                .set(QueryParams.SECONDS, null)
                .set(QueryParams.FIRST_VISIT_TIMESTAMP, null)
                .set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, null)
                .set(QueryParams.TOTAL_NUMBER_OF_VISITS, null)
                .set(QueryParams.GOAL_ID, null)
                .set(QueryParams.LATITUDE, null)
                .set(QueryParams.LONGITUDE, null)
                .set(QueryParams.SEARCH_KEYWORD, null)
                .set(QueryParams.SEARCH_CATEGORY, null)
                .set(QueryParams.SEARCH_NUMBER_OF_HITS, null)
                .set(QueryParams.REFERRER, null)
                .set(QueryParams.CAMPAIGN_NAME, null)
                .set(QueryParams.CAMPAIGN_KEYWORD, null);
        assertEquals("?h=0", trackMe.build());
    }

    @Test
    public void testSetURL() throws Exception {
        Tracker tracker = createTracker();
        tracker.setApplicationDomain("test.com");
        assertEquals(tracker.getApplicationDomain(), "test.com");
        assertEquals(tracker.getApplicationBaseURL(), "http://test.com");
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        assertEquals("http://test.com/", trackMe.get(QueryParams.URL_PATH));

        trackMe.set(QueryParams.URL_PATH, "me");
        tracker.track(trackMe);
        assertEquals("http://test.com/me", trackMe.get(QueryParams.URL_PATH));

        // override protocol
        trackMe.set(QueryParams.URL_PATH, "https://my.com/secure");
        tracker.track(trackMe);
        assertEquals("https://my.com/secure", trackMe.get(QueryParams.URL_PATH));
    }

    @Test
    public void testOutlink() throws Exception {
        Tracker tracker = createTracker();
        assertNull(tracker.getLastEvent());

        tracker.trackOutlink(new URL("file://mount/sdcard/something"));
        assertNull(tracker.getLastEvent());

        URL valid = new URL("https://foo.bar");
        tracker.trackOutlink(valid);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.URL_PATH));

        valid = new URL("https://foo.bar");
        tracker.trackOutlink(valid);
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.URL_PATH));

        valid = new URL("ftp://foo.bar");
        tracker.trackOutlink(valid);
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.URL_PATH));
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
        tracker.track(trackMe);
        assertTrue(trackMe.build().contains("_id=" + visitorId));
    }

    @Test
    public void testSetUserId() throws Exception {
        Tracker tracker = createTracker();
        assertNotNull(tracker.getDefaultTrackMe().get(QueryParams.USER_ID));

        tracker.setUserId("test");
        assertEquals(tracker.getUserId(), "test");

        tracker.setUserId("");
        assertEquals(tracker.getUserId(), "test");

        tracker.setUserId(null);
        assertNull(tracker.getUserId());

        String uuid = UUID.randomUUID().toString();
        tracker.setUserId(uuid);
        assertEquals(uuid, tracker.getUserId());
        assertEquals(uuid, createTracker().getUserId());
    }

    @Test
    public void testGetResolution() throws Exception {
        Tracker tracker = createTracker();
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        assertTrue(trackMe.build().contains("res=480x800"));
    }

    @Test
    public void testSetVisitCustomVariable() throws Exception {
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
        tracker.track(trackMe);
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
    public void testSetNewSessionRaceCondition() throws Exception {
        for (int retry = 0; retry < 5; retry++) {
            getPiwik().setOptOut(false);
            getPiwik().setDryRun(true);
            getPiwik().setDebug(true);
            final Tracker tracker = createTracker();
            tracker.setDispatchInterval(0);
            int count = 20;
            for (int i = 0; i < count; i++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        tracker.trackScreenView("Test");
                    }
                }).start();
            }
            Thread.sleep(500);
            List<String> flattenedQueries = TestDispatcher.getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
            assertEquals(count, flattenedQueries.size());
            int found = 0;
            for (String query : flattenedQueries) {
                if (query.contains("new_visit=1"))
                    found++;
            }
            assertEquals(1, found);
        }
    }

    @Test
    public void testSetSessionTimeout() throws Exception {
        Tracker tracker = createTracker();

        tracker.setSessionTimeout(10000);
        tracker.trackScreenView("test");
        assertFalse(tracker.tryNewSession());

        tracker.setSessionTimeout(0);
        Thread.sleep(1, 0);
        assertTrue(tracker.tryNewSession());

        tracker.setSessionTimeout(10000);
        assertFalse(tracker.tryNewSession());
        assertEquals(tracker.getSessionTimeout(), 10000);
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
        tracker.trackScreenView("/test/test", "title");
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

    private void checkEvent(QueryHashMap<String, String> queryParams, String name, Float value) {
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), name);
        assertEquals(String.valueOf(queryParams.get(QueryParams.EVENT_VALUE)), String.valueOf(value));
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
        tracker.trackEvent("category", "test action", name, 1f);
        checkEvent(parseEventUrl(tracker.getLastEvent()), name, 1f);
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
        tracker.trackGoal(1, 100f);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals("1", queryParams.get(QueryParams.GOAL_ID));
        assertTrue(100f == Float.valueOf(queryParams.get(QueryParams.REVENUE)));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackerEquals() throws Exception {
        Tracker tracker = createTracker();
        Tracker tracker2 = Piwik.getInstance(Robolectric.application).newTracker("http://localhost", 100);
        assertFalse(tracker.equals(null));
        assertFalse(tracker.equals(new String()));
        assertFalse(tracker.equals(tracker2));
    }

    @Test
    public void testTrackerHashCode() throws Exception {
        Tracker tracker = createTracker();
        assertEquals(tracker.hashCode(), 31 + tracker.getAPIUrl().hashCode());
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
        assertEquals(TestPiwikApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

        tracker.clearLastEvent();

        tracker.trackNewAppDownload(Robolectric.application, Tracker.ExtraIdentifier.INSTALLER_PACKAGENAME);
        queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(TestPiwikApplication.PACKAGENAME, m.group(1));
        assertEquals(TestPiwikApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(TestPiwikApplication.INSTALLER_PACKAGENAME, m.group(3));
        assertEquals(TestPiwikApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

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
        assertEquals("unknown", queryParams.get(QueryParams.REFERRER));
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
    public void testTrackEcommerceCartUpdate() throws Exception {
        Tracker tracker = createTracker();
        EcommerceItems items = new EcommerceItems();
        items.addItem("fake_sku", "fake_product", "fake_category", 200, 2);
        items.addItem("fake_sku_2", "fake_product_2", "fake_category_2", 400, 3);
        tracker.trackEcommerceCartUpdate(50000, items);

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.GOAL_ID), "0");
        assertEquals(queryParams.get(QueryParams.REVENUE), "500.00");

        String ecommerceItemsJson = queryParams.get(QueryParams.ECOMMERCE_ITEMS);

        new JSONArray(ecommerceItemsJson); // will throw exception if not valid json

        assertTrue(ecommerceItemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(ecommerceItemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackEcommerceOrder() throws Exception {
        Tracker tracker = createTracker();
        EcommerceItems items = new EcommerceItems();
        items.addItem("fake_sku", "fake_product", "fake_category", 200, 2);
        items.addItem("fake_sku_2", "fake_product_2", "fake_category_2", 400, 3);
        tracker.trackEcommerceOrder("orderId", 10020, 7002, 2000, 1000, 0, items);

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(queryParams.get(QueryParams.GOAL_ID), "0");
        assertEquals(queryParams.get(QueryParams.ORDER_ID), "orderId");
        assertEquals(queryParams.get(QueryParams.REVENUE), "100.20");
        assertEquals(queryParams.get(QueryParams.SUBTOTAL), "70.02");
        assertEquals(queryParams.get(QueryParams.TAX), "20.00");
        assertEquals(queryParams.get(QueryParams.SHIPPING), "10.00");
        assertEquals(queryParams.get(QueryParams.DISCOUNT), "0.00");

        String ecommerceItemsJson = queryParams.get(QueryParams.ECOMMERCE_ITEMS);

        new JSONArray(ecommerceItemsJson); // will throw exception if not valid json

        assertTrue(ecommerceItemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(ecommerceItemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
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
            tracker.track(trackMe);
            assertEquals("http://org.piwik.sdk.test/", trackMe.get(QueryParams.URL_PATH));
        }
    }

    @Test
    public void testSetAPIUrl() throws Exception {
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
        String defaultUserAgent = "aUserAgent";
        String customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0";
        System.setProperty("http.agent", "aUserAgent");

        // Default system user agent
        Tracker tracker = createTracker();
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        assertEquals(defaultUserAgent, trackMe.get(QueryParams.USER_AGENT));

        // Custom developer specified useragent
        tracker = createTracker();
        trackMe = new TrackMe();
        trackMe.set(QueryParams.USER_AGENT, customUserAgent);
        tracker.track(trackMe);
        assertEquals(customUserAgent, trackMe.get(QueryParams.USER_AGENT));

        // Modifying default TrackMe, no USER_AGENT
        tracker = createTracker();
        trackMe = new TrackMe();
        tracker.getDefaultTrackMe().set(QueryParams.USER_AGENT, null);
        tracker.track(trackMe);
        assertEquals(null, trackMe.get(QueryParams.USER_AGENT));
    }

    @Test
    public void testFirstVisitTimeStamp() throws Exception {
        Piwik piwik = getPiwik();
        assertEquals(-1, piwik.getSharedPreferences().getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1));

        Tracker tracker = createTracker();
        Tracker tracker1 = createTracker();

        tracker.trackEvent("TestCategory", "TestAction");
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        Thread.sleep(10);
        // make sure we are tracking in seconds
        assertTrue(Math.abs((System.currentTimeMillis() / 1000) - Long.parseLong(queryParams.get(QueryParams.FIRST_VISIT_TIMESTAMP))) < 2);

        tracker1.trackEvent("TestCategory", "TestAction");
        QueryHashMap<String, String> queryParams1 = parseEventUrl(tracker1.getLastEvent());
        assertEquals(Long.parseLong(queryParams.get(QueryParams.FIRST_VISIT_TIMESTAMP)), Long.parseLong(queryParams1.get(QueryParams.FIRST_VISIT_TIMESTAMP)));
        assertEquals(piwik.getSharedPreferences().getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1), Long.parseLong(queryParams.get(QueryParams.FIRST_VISIT_TIMESTAMP)));
    }

    @Test
    public void testTotalVisitCount() throws Exception {
        Piwik piwik = getPiwik();
        Tracker tracker = createTracker();
        assertEquals(-1, piwik.getSharedPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));

        tracker.trackEvent("TestCategory", "TestAction");
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(1, Integer.parseInt(queryParams.get(QueryParams.TOTAL_NUMBER_OF_VISITS)));

        tracker = createTracker();
        assertEquals(1, piwik.getSharedPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));
        tracker.trackEvent("TestCategory", "TestAction");
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(2, Integer.parseInt(queryParams.get(QueryParams.TOTAL_NUMBER_OF_VISITS)));
        assertEquals(2, piwik.getSharedPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
    }

    @Test
    public void testVisitCountMultipleThreads() throws Exception {
        int threadCount = 1000;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Tracker tracker = createTracker();
                        Thread.sleep(new Random().nextInt(20 - 0) + 0);
                        tracker.trackEvent("TestCategory", "TestAction");
                        countDownLatch.countDown();
                    } catch (MalformedURLException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        countDownLatch.await();
        assertEquals(threadCount, getPiwik().getSharedPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, 0));
    }

    @Test
    public void testFirstVisitMultipleThreads() throws Exception {
        int threadCount = 100;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        final List<Long> firstVisitTimes = Collections.synchronizedList(new ArrayList<Long>());
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Tracker tracker = createTracker();
                        Thread.sleep(new Random().nextInt(20 - 0) + 0);
                        tracker.trackEvent("TestCategory", "TestAction");
                        long firstVisit = Long.valueOf(tracker.getDefaultTrackMe().get(QueryParams.FIRST_VISIT_TIMESTAMP));
                        firstVisitTimes.add(firstVisit);
                        countDownLatch.countDown();
                    } catch (MalformedURLException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        countDownLatch.await();
        for (Long firstVisit : firstVisitTimes)
            assertEquals(firstVisitTimes.get(0), firstVisit);
    }

    @Test
    public void testPreviousVisits() throws Exception {
        final List<Long> previousVisitTimes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            try {
                Tracker tracker = createTracker();
                tracker.trackEvent("TestCategory", "TestAction");
                String previousVisit = tracker.getDefaultTrackMe().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP);
                if (previousVisit != null)
                    previousVisitTimes.add(Long.parseLong(previousVisit));
                Thread.sleep(1010);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        assertFalse(previousVisitTimes.contains(0l));
        Long lastTime = 0l;
        for (Long time : previousVisitTimes) {
            assertTrue(lastTime < time);
            lastTime = time;
        }
    }

    @Test
    public void testPreviousVisit() throws Exception {
        Piwik piwik = getPiwik();
        // No timestamp yet
        assertEquals(-1, piwik.getSharedPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1));

        Tracker tracker = createTracker();
        tracker.trackEvent("TestCategory", "TestAction");
        long _startTime = System.currentTimeMillis() / 1000;
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        // There was no previous visit
        assertNull(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP));
        Thread.sleep(1000);

        // After the first visit we now have a timestamp for the previous visit
        long previousVisit = piwik.getSharedPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
        assertTrue(previousVisit - _startTime < 2000);
        assertNotEquals(-1, previousVisit);

        tracker = createTracker();
        tracker.trackEvent("TestCategory", "TestAction");
        queryParams = parseEventUrl(tracker.getLastEvent());
        // Transmitted timestamp is the one from the first visit visit
        assertEquals(previousVisit, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        Thread.sleep(1000);
        tracker = createTracker();
        tracker.trackEvent("TestCategory", "TestAction");
        queryParams = parseEventUrl(tracker.getLastEvent());
        // Now the timestamp changed as this is the 3rd visit.
        assertNotEquals(previousVisit, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
        Thread.sleep(1000);

        previousVisit = piwik.getSharedPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);

        tracker = createTracker();
        tracker.trackEvent("TestCategory", "TestAction");
        queryParams = parseEventUrl(tracker.getLastEvent());
        // Just make sure the timestamp in the 4th visit is from the 3rd visit
        assertEquals(previousVisit, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        // Test setting a custom timestamp
        TrackMe custom = new TrackMe();
        custom.set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, 1000l);
        tracker.track(custom);
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(1000l, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
    }

    @Test
    public void testSetCustomDimensions() throws Exception {
        CustomDimensions customDimensions = new CustomDimensions();
        customDimensions.set(0, "foo");
        customDimensions.set(1, "foo");
        customDimensions.set(2, "bar");
        customDimensions.set(3, "empty").set(3, null);
        customDimensions.set(4, "");

        QueryHashMap<String, String> queryParams = parseEventUrl(customDimensions.build());

        assertEquals(queryParams.get("dimension1"), "foo");
        assertEquals(queryParams.get("dimension2"), "bar");
        assertFalse(queryParams.containsKey("dimension0"));
        assertFalse(queryParams.containsKey("dimension3"));
        assertFalse(queryParams.containsKey("dimension4"));
    }

    @Test
    public void testSetCustomDimensionsMaxLength() throws Exception {
        CustomDimensions customDimensions = new CustomDimensions();
        customDimensions.set(1, new String(new char[1000]));

        QueryHashMap<String, String> queryParams = parseEventUrl(customDimensions.build());
        assertEquals(queryParams.get("dimension1").length(), 255);
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
        QueryHashMap<String, String> values = new QueryHashMap<>();

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
