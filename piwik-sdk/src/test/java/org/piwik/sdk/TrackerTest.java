package org.piwik.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.piwik.sdk.dispatcher.DispatchMode;
import org.piwik.sdk.dispatcher.Dispatcher;
import org.piwik.sdk.dispatcher.DispatcherFactory;
import org.piwik.sdk.extra.TrackHelper;
import org.piwik.sdk.testhelper.TestPreferences;
import org.piwik.sdk.tools.DeviceHelper;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.piwik.sdk.QueryParams.FIRST_VISIT_TIMESTAMP;
import static org.piwik.sdk.QueryParams.SESSION_START;


@SuppressWarnings("PointlessArithmeticExpression")
public class TrackerTest {
    ArgumentCaptor<TrackMe> mCaptor = ArgumentCaptor.forClass(TrackMe.class);
    Tracker mTracker;
    @Mock Piwik mPiwik;
    @Mock Context mContext;
    @Mock Dispatcher mDispatcher;
    @Mock DispatcherFactory mDispatcherFactory;
    @Mock DeviceHelper mDeviceHelper;
    SharedPreferences mTrackerPreferences = new TestPreferences();
    SharedPreferences mPiwikPreferences = new TestPreferences();
    private final String mApiUrl = "http://example.com";
    private final int mSiteId = 11;
    private final String mName = "Default Tracker";
    private final TrackerConfig mTrackerConfig = new TrackerConfig(mApiUrl, mSiteId, mName);

    @Before
    public void setup() throws PackageManager.NameNotFoundException, MalformedURLException {
        MockitoAnnotations.initMocks(this);
        when(mPiwik.getContext()).thenReturn(mContext);
        when(mContext.getPackageName()).thenReturn("package");
        when(mPiwik.getTrackerPreferences(any(Tracker.class))).thenReturn(mTrackerPreferences);
        when(mPiwik.getPiwikPreferences()).thenReturn(mPiwikPreferences);
        when(mPiwik.getDispatcherFactory()).thenReturn(mDispatcherFactory);
        when(mPiwik.getApplicationDomain()).thenReturn("org.piwik.sdk.test");
        when(mDispatcherFactory.build(any(Tracker.class))).thenReturn(mDispatcher);
        when(mPiwik.getDeviceHelper()).thenReturn(mDeviceHelper);
        when(mDeviceHelper.getResolution()).thenReturn(new int[]{480, 800});
        when(mDeviceHelper.getUserAgent()).thenReturn("aUserAgent");
        when(mDeviceHelper.getUserLanguage()).thenReturn("en");

        mTracker = new Tracker(mPiwik, mTrackerConfig);
    }

    @Test
    public void testGetPreferences() {
        Tracker tracker1 = new Tracker(mPiwik, new TrackerConfig(mApiUrl, mSiteId, "Tracker1"));
        verify(mPiwik).getTrackerPreferences(tracker1);
        Tracker tracker2 = new Tracker(mPiwik, new TrackerConfig(mApiUrl, mSiteId, "Tracker2"));
        verify(mPiwik).getTrackerPreferences(tracker2);
    }

    @Test
    public void testLastScreenUrl() throws Exception {
        mTracker.track(new TrackMe());
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals(mTracker.getApplicationBaseURL() + "/", mCaptor.getValue().get(QueryParams.URL_PATH));

        mTracker.track(new TrackMe().set(QueryParams.URL_PATH, "http://some.thing.com/foo/bar"));
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals("http://some.thing.com/foo/bar", mCaptor.getValue().get(QueryParams.URL_PATH));

        mTracker.track(new TrackMe().set(QueryParams.URL_PATH, "http://some.other/thing"));
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        assertEquals("http://some.other/thing", mCaptor.getValue().get(QueryParams.URL_PATH));

        mTracker.track(new TrackMe());
        verify(mDispatcher, times(4)).submit(mCaptor.capture());
        assertEquals("http://some.other/thing", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testSetDispatchInterval() throws Exception {
        mTracker.setDispatchInterval(1);
        verify(mDispatcher).setDispatchInterval(1);
        mTracker.getDispatchInterval();
        verify(mDispatcher).getDispatchInterval();
    }

    @Test
    public void testSetDispatchTimeout() throws Exception {
        int timeout = 1337;
        mTracker.setDispatchTimeout(timeout);
        verify(mDispatcher).setConnectionTimeOut(timeout);
        mTracker.getDispatchTimeout();
        verify(mDispatcher).getConnectionTimeOut();
    }

    @Test
    public void testGetOfflineCacheAge_defaultValue() throws Exception {
        assertEquals(24 * 60 * 60 * 1000, mTracker.getOfflineCacheAge());
    }

    @Test
    public void testSetOfflineCacheAge() throws Exception {
        mTracker.setOfflineCacheAge(80085);
        assertEquals(80085, mTracker.getOfflineCacheAge());
    }

    @Test
    public void testGetOfflineCacheSize_defaultValue() throws Exception {
        assertEquals(4 * 1024 * 1024, mTracker.getOfflineCacheSize());
    }

    @Test
    public void testSetOfflineCacheSize() throws Exception {
        mTracker.setOfflineCacheSize(16 * 1000 * 1000);
        assertEquals(16 * 1000 * 1000, mTracker.getOfflineCacheSize());
    }

    @Test
    public void testSetDispatchMode() throws MalformedURLException {
        assertEquals(DispatchMode.ALWAYS, mTracker.getDispatchMode());
        verify(mDispatcher, times(1)).setDispatchMode(DispatchMode.ALWAYS);

        mTracker.setDispatchMode(DispatchMode.WIFI_ONLY);
        assertEquals(DispatchMode.WIFI_ONLY, mTracker.getDispatchMode());
        verify(mDispatcher, times(1)).setDispatchMode(DispatchMode.WIFI_ONLY);

        mTracker.getPreferences().edit().putString(Tracker.PREF_KEY_DISPATCHER_MODE, "lol").apply();
        assertEquals(DispatchMode.ALWAYS, mTracker.getDispatchMode());
        verify(mDispatcher, times(2)).setDispatchMode(DispatchMode.ALWAYS);

        mTracker.setDispatchMode(DispatchMode.WIFI_ONLY);
        assertEquals(DispatchMode.WIFI_ONLY, mTracker.getDispatchMode());
        verify(mDispatcher, times(2)).setDispatchMode(DispatchMode.WIFI_ONLY);
    }

    @Test
    public void testsetDispatchGzip() {
        mTracker.setDispatchGzipped(true);
        verify(mDispatcher).setDispatchGzipped(true);
    }

    @Test
    public void testOptOut_set() throws Exception {
        mTracker.setOptOut(true);
        verify(mDispatcher).clear();
        assertTrue(mTracker.isOptOut());
        mTracker.setOptOut(false);
        assertFalse(mTracker.isOptOut());
    }

    @Test
    public void testOptOut_init() throws Exception {
        mTrackerPreferences.edit().putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, false).apply();
        Tracker tracker = new Tracker(mPiwik, mTrackerConfig);
        assertFalse(tracker.isOptOut());
        mTrackerPreferences.edit().putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, true).apply();
        tracker = new Tracker(mPiwik, mTrackerConfig);
        assertTrue(tracker.isOptOut());
    }

    @Test
    public void testDispatch() {
        mTracker.dispatch();
        verify(mDispatcher).forceDispatch();
        mTracker.dispatch();
        verify(mDispatcher, times(2)).forceDispatch();
    }

    @Test
    public void testDispatch_optOut() {
        mTracker.setOptOut(true);
        mTracker.dispatch();
        verify(mDispatcher, never()).forceDispatch();
        mTracker.setOptOut(false);
        mTracker.dispatch();
        verify(mDispatcher).forceDispatch();
    }

    @Test
    public void testGetSiteId() throws Exception {
        assertEquals(mTracker.getSiteId(), 11);
    }

    @Test
    public void testGetPiwik() throws Exception {
        assertEquals(mPiwik, mTracker.getPiwik());
    }

    @Test
    public void testSetURL() throws Exception {
        mTracker.setApplicationDomain("test.com");
        assertEquals(mTracker.getApplicationDomain(), "test.com");
        assertEquals(mTracker.getApplicationBaseURL(), "http://test.com");
        TrackMe trackMe = new TrackMe();
        mTracker.track(trackMe);
        assertEquals("http://test.com/", trackMe.get(QueryParams.URL_PATH));

        trackMe.set(QueryParams.URL_PATH, "me");
        mTracker.track(trackMe);
        assertEquals("http://test.com/me", trackMe.get(QueryParams.URL_PATH));

        // override protocol
        trackMe.set(QueryParams.URL_PATH, "https://my.com/secure");
        mTracker.track(trackMe);
        assertEquals("https://my.com/secure", trackMe.get(QueryParams.URL_PATH));
    }

    @Test
    public void testSetApplicationDomain() throws Exception {
        mTracker.setApplicationDomain("my-domain.com");
        TrackHelper.track().screen("test/test").title("Test title").with(mTracker);
        verify(mDispatcher).submit(mCaptor.capture());
        validateDefaultQuery(mCaptor.getValue());
        assertTrue(mCaptor.getValue().get(QueryParams.URL_PATH).equals("http://my-domain.com/test/test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTooShortVistorId() throws MalformedURLException {
        String tooShortVisitorId = "0123456789ab";
        mTracker.setVisitorId(tooShortVisitorId);
        assertNotEquals(tooShortVisitorId, mTracker.getVisitorId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTooLongVistorId() throws MalformedURLException {
        String tooLongVisitorId = "0123456789abcdefghi";
        mTracker.setVisitorId(tooLongVisitorId);
        assertNotEquals(tooLongVisitorId, mTracker.getVisitorId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVistorIdWithInvalidCharacters() throws MalformedURLException {
        String invalidCharacterVisitorId = "01234-6789-ghief";
        mTracker.setVisitorId(invalidCharacterVisitorId);
        assertNotEquals(invalidCharacterVisitorId, mTracker.getVisitorId());
    }

    @Test
    public void testSetVistorId() throws Exception {
        String visitorId = "0123456789abcdef";
        mTracker.setVisitorId(visitorId);
        assertEquals(visitorId, mTracker.getVisitorId());
        TrackMe trackMe = new TrackMe();
        mTracker.track(trackMe);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals(visitorId, mCaptor.getValue().get(QueryParams.VISITOR_ID));
    }

    @Test
    public void testSetUserId() throws Exception {
        assertNotNull(mTracker.getDefaultTrackMe().get(QueryParams.USER_ID));

        mTracker.setUserId("test");
        assertEquals(mTracker.getUserId(), "test");

        mTracker.setUserId("");
        assertEquals(mTracker.getUserId(), "test");

        mTracker.setUserId(null);
        assertNull(mTracker.getUserId());

        String uuid = UUID.randomUUID().toString();
        mTracker.setUserId(uuid);
        assertEquals(uuid, mTracker.getUserId());
        assertEquals(uuid, mTracker.getUserId());
    }

    @Test
    public void testGetResolution() throws Exception {
        TrackMe trackMe = new TrackMe();
        mTracker.track(trackMe);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("480x800", mCaptor.getValue().get(QueryParams.SCREEN_RESOLUTION));
    }

    @Test
    public void testSetNewSession() throws Exception {
        TrackMe trackMe = new TrackMe();
        mTracker.track(trackMe);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));

        TrackHelper.track().screen("").with(mTracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals(null, mCaptor.getValue().get(QueryParams.SESSION_START));

        TrackHelper.track().screen("").with(mTracker);
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        assertEquals(null, mCaptor.getValue().get(QueryParams.SESSION_START));

        mTracker.startNewSession();
        TrackHelper.track().screen("").with(mTracker);
        verify(mDispatcher, times(4)).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));
    }

    @Test
    public void testSetNewSessionRaceCondition() throws Exception {
        for (int retry = 0; retry < 5; retry++) {
            final List<TrackMe> trackMes = Collections.synchronizedList(new ArrayList<TrackMe>());
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    trackMes.add((TrackMe) invocation.getArgument(0));
                    return null;
                }
            }).when(mDispatcher).submit(any(TrackMe.class));
            final Tracker tracker = new Tracker(mPiwik, mTrackerConfig);
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
            assertEquals(count, trackMes.size());
            int found = 0;
            for (TrackMe trackMe : trackMes) {
                if (trackMe.get(QueryParams.SESSION_START) != null) found++;
            }
            assertEquals(1, found);
        }
    }

    @Test
    public void testSetSessionTimeout() throws Exception {
        mTracker.setSessionTimeout(10000);

        TrackHelper.track().screen("test").with(mTracker);
        assertFalse(mTracker.tryNewSession());

        mTracker.setSessionTimeout(0);
        Thread.sleep(1, 0);
        assertTrue(mTracker.tryNewSession());

        mTracker.setSessionTimeout(10000);
        assertFalse(mTracker.tryNewSession());
        assertEquals(mTracker.getSessionTimeout(), 10000);
    }

    @Test
    public void testCheckSessionTimeout() throws Exception {
        mTracker.setSessionTimeout(0);
        TrackHelper.track().screen("test").with(mTracker);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));
        Thread.sleep(1, 0);
        TrackHelper.track().screen("test").with(mTracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));
        mTracker.setSessionTimeout(60000);
        TrackHelper.track().screen("test").with(mTracker);
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        assertEquals(null, mCaptor.getValue().get(QueryParams.SESSION_START));
    }

    @Test
    public void testTrackerEquals() throws Exception {
        Tracker tracker2 = new Tracker(mPiwik, new TrackerConfig("http://localhost", 100, "Default Tracker"));
        Tracker tracker3 = new Tracker(mPiwik, new TrackerConfig("http://example.com", 11, "Default Tracker"));
        assertNotNull(mTracker);
        assertFalse(mTracker.equals(tracker2));
        assertTrue(mTracker.equals(tracker3));
    }

    @Test
    public void testTrackerHashCode() throws Exception {
        assertEquals(mTrackerConfig.hashCode(), mTracker.hashCode());
    }

    @Test
    public void testUrlPathCorrection() throws Exception {
        String[] paths = new String[]{null, "", "/",};
        for (String path : paths) {
            TrackMe trackMe = new TrackMe();
            trackMe.set(QueryParams.URL_PATH, path);
            mTracker.track(trackMe);
            assertEquals("http://org.piwik.sdk.test/", trackMe.get(QueryParams.URL_PATH));
        }
    }

    @Test
    public void testSetUserAgent() throws MalformedURLException {
        TrackMe trackMe = new TrackMe();
        mTracker.track(trackMe);
        assertEquals("aUserAgent", trackMe.get(QueryParams.USER_AGENT));

        // Custom developer specified useragent
        trackMe = new TrackMe();
        String customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0";
        trackMe.set(QueryParams.USER_AGENT, customUserAgent);
        mTracker.track(trackMe);
        assertEquals(customUserAgent, trackMe.get(QueryParams.USER_AGENT));

        // Modifying default TrackMe, no USER_AGENT
        trackMe = new TrackMe();
        mTracker.getDefaultTrackMe().set(QueryParams.USER_AGENT, null);
        mTracker.track(trackMe);
        assertEquals(null, trackMe.get(QueryParams.USER_AGENT));
    }

    @Test
    public void testFirstVisitTimeStamp() throws Exception {
        assertEquals(-1, mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1));

        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher).submit(mCaptor.capture());
        TrackMe trackMe1 = mCaptor.getValue();
        Thread.sleep(10);
        // make sure we are tracking in seconds
        assertTrue(Math.abs((System.currentTimeMillis() / 1000) - Long.parseLong(trackMe1.get(FIRST_VISIT_TIMESTAMP))) < 2);

        mTracker = new Tracker(mPiwik, mTrackerConfig);
        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        TrackMe trackMe2 = mCaptor.getValue();
        assertEquals(Long.parseLong(trackMe1.get(FIRST_VISIT_TIMESTAMP)), Long.parseLong(trackMe2.get(FIRST_VISIT_TIMESTAMP)));
        assertEquals(mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1), Long.parseLong(trackMe1.get(FIRST_VISIT_TIMESTAMP)));
    }

    @Test
    public void testTotalVisitCount() throws Exception {
        assertEquals(-1, mTracker.getPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(mTracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));

        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals(1, Integer.parseInt(mCaptor.getValue().get(QueryParams.TOTAL_NUMBER_OF_VISITS)));

        mTracker = new Tracker(mPiwik, mTrackerConfig);
        assertEquals(1, mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(mTracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));
        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals(2, Integer.parseInt(mCaptor.getValue().get(QueryParams.TOTAL_NUMBER_OF_VISITS)));
        assertEquals(2, mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
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
                        Thread.sleep(new Random().nextInt(20 - 0) + 0);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    TrackHelper.track().event("TestCategory", "TestAction").with(new Tracker(mPiwik, mTrackerConfig));
                    countDownLatch.countDown();
                }
            }).start();
        }
        countDownLatch.await();
        assertEquals(threadCount, mTrackerPreferences.getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, 0));
    }

    @Test
    public void testSessionStartRaceCondition() throws Exception {
        final List<TrackMe> trackMes = Collections.synchronizedList(new ArrayList<TrackMe>());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                trackMes.add((TrackMe) invocation.getArgument(0));
                return null;
            }
        }).when(mDispatcher).submit(any(TrackMe.class));
        when(mDispatcher.getConnectionTimeOut()).thenReturn(1000);
        for (int i = 0; i < 1000; i++) {
            trackMes.clear();
            final Tracker tracker = new Tracker(mPiwik, mTrackerConfig);
            final CountDownLatch countDownLatch = new CountDownLatch(10);
            for (int j = 0; j < 10; j++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(new Random().nextInt(4 - 0) + 0);
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
            for (TrackMe out : trackMes) {
                if (trackMes.indexOf(out) == 0) {
                    assertTrue(i + "#" + out.toMap().size(), out.get(QueryParams.LANGUAGE) != null);
                    assertTrue(out.get(QueryParams.FIRST_VISIT_TIMESTAMP) != null);
                    assertTrue(out.get(SESSION_START) != null);
                } else {
                    assertTrue(out.get(QueryParams.LANGUAGE) == null);
                    assertTrue(out.get(QueryParams.FIRST_VISIT_TIMESTAMP) == null);
                    assertTrue(out.get(SESSION_START) == null);
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

                        Thread.sleep(new Random().nextInt(20 - 0) + 0);
                        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
                        long firstVisit = Long.valueOf(mTracker.getDefaultTrackMe().get(FIRST_VISIT_TIMESTAMP));
                        firstVisitTimes.add(firstVisit);
                        countDownLatch.countDown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        countDownLatch.await();
        for (Long firstVisit : firstVisitTimes) assertEquals(firstVisitTimes.get(0), firstVisit);
    }

    @Test
    public void testPreviousVisits() throws Exception {
        final List<Long> previousVisitTimes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {


            TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
            String previousVisit = mTracker.getDefaultTrackMe().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP);
            if (previousVisit != null)
                previousVisitTimes.add(Long.parseLong(previousVisit));
            Thread.sleep(1010);

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
        // No timestamp yet
        assertEquals(-1, mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1));
        mTracker = new Tracker(mPiwik, mTrackerConfig);
        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher).submit(mCaptor.capture());
        long _startTime = System.currentTimeMillis() / 1000;
        // There was no previous visit
        assertNull(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP));
        Thread.sleep(1000);

        // After the first visit we now have a timestamp for the previous visit
        long previousVisit = mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
        assertTrue(previousVisit - _startTime < 2000);
        assertNotEquals(-1, previousVisit);
        mTracker = new Tracker(mPiwik, mTrackerConfig);
        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        // Transmitted timestamp is the one from the first visit visit
        assertEquals(previousVisit, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        Thread.sleep(1000);
        mTracker = new Tracker(mPiwik, mTrackerConfig);
        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        // Now the timestamp changed as this is the 3rd visit.
        assertNotEquals(previousVisit, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
        Thread.sleep(1000);

        previousVisit = mTracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
        mTracker = new Tracker(mPiwik, mTrackerConfig);
        TrackHelper.track().event("TestCategory", "TestAction").with(mTracker);
        verify(mDispatcher, times(4)).submit(mCaptor.capture());
        // Just make sure the timestamp in the 4th visit is from the 3rd visit
        assertEquals(previousVisit, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        // Test setting a custom timestamp
        TrackMe custom = new TrackMe();
        custom.set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, 1000L);
        mTracker.track(custom);
        verify(mDispatcher, times(5)).submit(mCaptor.capture());
        assertEquals(1000L, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
    }

    private static void validateDefaultQuery(TrackMe params) {
        assertEquals(params.get(QueryParams.SITE_ID), "11");
        assertEquals(params.get(QueryParams.RECORD), "1");
        assertEquals(params.get(QueryParams.SEND_IMAGE), "0");
        assertEquals(params.get(QueryParams.VISITOR_ID).length(), 16);
        assertTrue(params.get(QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(QueryParams.RANDOM_NUMBER)) > 0);
    }
}
