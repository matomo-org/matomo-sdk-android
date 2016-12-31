package org.piwik.sdk;

import android.app.Application;
import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.TestActivity;
import org.piwik.sdk.tools.UrlHelper;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.piwik.sdk.dispatcher.DispatcherTest.getFlattenedQueries;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class TrackerTest extends DefaultTestCase {

    @Test
    public void testLastScreenUrl() throws Exception {
        Tracker tracker = createTracker();
        assertNull(tracker.getLastEvent());

        tracker.track(new TrackMe());
        assertNotNull(tracker.getLastEvent());
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(tracker.getApplicationBaseURL() + "/", queryParams.get(QueryParams.URL_PATH));

        tracker.track(new TrackMe().set(QueryParams.URL_PATH, "http://some.thing.com/foo/bar"));
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals("http://some.thing.com/foo/bar", queryParams.get(QueryParams.URL_PATH));

        tracker.track(new TrackMe().set(QueryParams.URL_PATH, "http://some.other/thing"));
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals("http://some.other/thing", queryParams.get(QueryParams.URL_PATH));

        tracker.track(new TrackMe());
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals("http://some.other/thing", queryParams.get(QueryParams.URL_PATH));
    }

    @Test
    public void testPiwikAutoBindActivities() throws Exception {
        Application app = Robolectric.application;
        Piwik piwik = Piwik.getInstance(app);
        piwik.setDryRun(true);
        piwik.setOptOut(true);
        Tracker tracker = createTracker();
        //auto attach tracking screen view
        TrackHelper.track().screens(app).with(tracker);

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
    public void testSetDispatchTimeout() throws Exception {
        Tracker tracker = createTracker();
        tracker.setDispatchTimeout(1337);

        assertEquals(1337, tracker.getDispatcher().getConnectionTimeOut());
        assertEquals(1337, tracker.getDispatchTimeout());
    }

    @Test
    public void testGetOfflineCacheAge_defaultValue() throws Exception {
        Tracker tracker = createTracker();
        assertEquals(24 * 60 * 60 * 1000, tracker.getOfflineCacheAge());
    }

    @Test
    public void testSetOfflineCacheAge() throws Exception {
        Tracker tracker = createTracker();
        tracker.setOfflineCacheAge(80085);
        assertEquals(80085, tracker.getOfflineCacheAge());
    }

    @Test
    public void testGetOfflineCacheSize_defaultValue() throws Exception {
        Tracker tracker = createTracker();
        assertEquals(4 * 1024 * 1024, tracker.getOfflineCacheSize());
    }

    @Test
    public void testSetOfflineCacheSize() throws Exception {
        Tracker tracker = createTracker();
        tracker.setOfflineCacheSize(16 * 1000 * 1000);
        assertEquals(16 * 1000 * 1000, tracker.getOfflineCacheSize());
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
    public void testSetApplicationDomain() throws Exception {
        Tracker tracker = createTracker();
        tracker.setApplicationDomain("my-domain.com");
        TrackHelper.track().screen("test/test").title("Test title").with(tracker);

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
        assertTrue(tracker.getLastEvent().contains("_id="));
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
        assertTrue(tracker.getLastEvent().contains("res=480x800"));
    }

    @Test
    public void testSetVisitCustomVariable() throws Exception {
        Tracker tracker = createTracker();
        tracker.setVisitCustomVariable(1, "2& ?", "3@#");
        TrackHelper.track().screen("").with(tracker);

        String event = tracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals(queryParams.get("_cvar"), "{'1':['2& ?','3@#']}".replaceAll("'", "\""));
        // check url encoding
        assertTrue(event.contains("_cvar=%7B%221%22%3A%5B%222%26%20%3F%22%2C%223%40%23%22%5D%7D"));
    }

    @Test
    public void testSetNewSession() throws Exception {
        Tracker tracker = createTracker();
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        assertTrue(tracker.getLastEvent().contains("new_visit=1"));

        TrackHelper.track().screen("").with(tracker);
        assertFalse(tracker.getLastEvent().contains("new_visit=1"));

        TrackHelper.track().screen("").with(tracker);
        assertFalse(tracker.getLastEvent().contains("new_visit=1"));

        tracker.startNewSession();
        TrackHelper.track().screen("").with(tracker);
        assertTrue(tracker.getLastEvent().contains("new_visit=1"));
    }

    @Test
    public void testSetNewSessionRaceCondition() throws Exception {
        for (int retry = 0; retry < 5; retry++) {
            getPiwik().setOptOut(false);
            getPiwik().setDryRun(true);
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
                        TrackHelper.track().screen("Test").with(tracker);
                    }
                }).start();
            }
            Thread.sleep(500);
            List<String> flattenedQueries = getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
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

        TrackHelper.track().screen("test").with(tracker);
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
        TrackHelper.track().screen("test").with(tracker);
        assertTrue(tracker.getLastEvent().contains("new_visit=1"));
        Thread.sleep(1, 0);
        TrackHelper.track().screen("test").with(tracker);
        assertTrue(tracker.getLastEvent().contains("new_visit=1"));
        tracker.setSessionTimeout(60000);
        TrackHelper.track().screen("test").with(tracker);
        assertFalse(tracker.getLastEvent().contains("new_visit=1"));
    }

    private void checkEvent(QueryHashMap<String, String> queryParams, String name, Float value) {
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), name);
        assertEquals(String.valueOf(queryParams.get(QueryParams.EVENT_VALUE)), String.valueOf(value));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackerEquals() throws Exception {
        Tracker tracker = createTracker();
        Tracker tracker2 = Piwik.getInstance(Robolectric.application).newTracker("http://localhost", 100);
        Tracker tracker3 = Piwik.getInstance(Robolectric.application).newTracker("http://example.com", 1);
        assertNotNull(tracker);
        assertFalse(tracker.equals(tracker2));
        assertTrue(tracker.equals(tracker3));
    }

    @Test
    public void testTrackerHashCode() throws Exception {
        Tracker tracker = createTracker();
        assertEquals(tracker.hashCode(), 31 + tracker.getAPIUrl().hashCode());
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

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        Thread.sleep(10);
        // make sure we are tracking in seconds
        assertTrue(Math.abs((System.currentTimeMillis() / 1000) - Long.parseLong(queryParams.get(QueryParams.FIRST_VISIT_TIMESTAMP))) < 2);

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker1);
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

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(1, Integer.parseInt(queryParams.get(QueryParams.TOTAL_NUMBER_OF_VISITS)));

        tracker = createTracker();
        assertEquals(1, piwik.getSharedPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
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
                        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
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
    public void testSessionStartRaceCondition() throws Exception {
        final Tracker tracker = createTracker();
        for (int i = 0; i < 1000; i++) {
            final CountDownLatch countDownLatch = new CountDownLatch(10);
            for (int j = 0; j < 10; j++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(new Random().nextInt(2 - 0) + 0);
                            TrackMe trackMe = new TrackMe()
                                    .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_VALUE, 1);
                            tracker.track(trackMe);

                            countDownLatch.countDown();
                        } catch (Exception e) {
                            e.printStackTrace();
                            assertFalse(true);
                        }
                    }
                }).start();
            }
            countDownLatch.await();
            List<String> output = getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
            for (String out : output) {
                if (output.indexOf(out) == 0) {
                    assertTrue(out.contains("lang"));
                    assertTrue(out.contains("_idts"));
                    assertTrue(out.contains("new_visit"));
                } else {
                    assertFalse(out.contains("lang"));
                    assertFalse(out.contains("_idts"));
                    assertFalse(out.contains("new_visit"));
                }
            }
        }
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
                        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
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
                TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
                String previousVisit = tracker.getDefaultTrackMe().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP);
                if (previousVisit != null)
                    previousVisitTimes.add(Long.parseLong(previousVisit));
                Thread.sleep(1010);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        assertFalse(previousVisitTimes.contains(0L));
        Long lastTime = 0L;
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
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
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
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        queryParams = parseEventUrl(tracker.getLastEvent());
        // Transmitted timestamp is the one from the first visit visit
        assertEquals(previousVisit, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        Thread.sleep(1000);
        tracker = createTracker();
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        queryParams = parseEventUrl(tracker.getLastEvent());
        // Now the timestamp changed as this is the 3rd visit.
        assertNotEquals(previousVisit, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
        Thread.sleep(1000);

        previousVisit = piwik.getSharedPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);

        tracker = createTracker();
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        queryParams = parseEventUrl(tracker.getLastEvent());
        // Just make sure the timestamp in the 4th visit is from the 3rd visit
        assertEquals(previousVisit, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        // Test setting a custom timestamp
        TrackMe custom = new TrackMe();
        custom.set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, 1000L);
        tracker.track(custom);
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(1000L, Long.parseLong(queryParams.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
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
        QueryHashMap<String, String> values = new QueryHashMap<>();

        List<Pair<String, String>> params = UrlHelper.parse(new URI("http://localhost/" + url), "UTF-8");

        for (Pair<String, String> param : params)
            values.put(param.first, param.second);

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
