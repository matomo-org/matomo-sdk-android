package org.matomo.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.matomo.sdk.dispatcher.DispatchMode;
import org.matomo.sdk.dispatcher.Dispatcher;
import org.matomo.sdk.dispatcher.DispatcherFactory;
import org.matomo.sdk.extra.TrackHelper;
import org.matomo.sdk.tools.DeviceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

import testhelpers.TestHelper;
import testhelpers.TestPreferences;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.matomo.sdk.QueryParams.FIRST_VISIT_TIMESTAMP;
import static org.matomo.sdk.QueryParams.PREVIOUS_VISIT_TIMESTAMP;
import static org.matomo.sdk.QueryParams.SESSION_START;
import static org.matomo.sdk.QueryParams.TOTAL_NUMBER_OF_VISITS;
import static org.matomo.sdk.QueryParams.VISITOR_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;


@SuppressWarnings("PointlessArithmeticExpression")
public class TrackerTest {
    ArgumentCaptor<TrackMe> mCaptor = ArgumentCaptor.forClass(TrackMe.class);
    @Mock Matomo mMatomo;
    @Mock Context mContext;
    @Mock Dispatcher mDispatcher;
    @Mock DispatcherFactory mDispatcherFactory;
    @Mock DeviceHelper mDeviceHelper;
    SharedPreferences mTrackerPreferences = new TestPreferences();
    SharedPreferences mPreferences = new TestPreferences();
    @Mock TrackerBuilder mTrackerBuilder;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mMatomo.getContext()).thenReturn(mContext);
        when(mMatomo.getTrackerPreferences(any(Tracker.class))).thenReturn(mTrackerPreferences);
        when(mMatomo.getPreferences()).thenReturn(mPreferences);
        when(mMatomo.getDispatcherFactory()).thenReturn(mDispatcherFactory);
        when(mDispatcherFactory.build(any(Tracker.class))).thenReturn(mDispatcher);
        when(mMatomo.getDeviceHelper()).thenReturn(mDeviceHelper);
        when(mDeviceHelper.getResolution()).thenReturn(new int[]{480, 800});
        when(mDeviceHelper.getUserAgent()).thenReturn("aUserAgent");
        when(mDeviceHelper.getUserLanguage()).thenReturn("en");

        String mApiUrl = "http://example.com";
        when(mTrackerBuilder.getApiUrl()).thenReturn(mApiUrl);
        int mSiteId = 11;
        when(mTrackerBuilder.getSiteId()).thenReturn(mSiteId);
        String mTrackerName = "Default Tracker";
        when(mTrackerBuilder.getTrackerName()).thenReturn(mTrackerName);
        when(mTrackerBuilder.getApplicationBaseUrl()).thenReturn("http://this.is.our.package/");

        mTrackerPreferences.edit().clear();
        mPreferences.edit().clear();
    }

    @Test
    public void testGetPreferences() {
        Tracker tracker1 = new Tracker(mMatomo, mTrackerBuilder);
        verify(mMatomo).getTrackerPreferences(tracker1);
    }

    /**
     * https://github.com/matomo-org/matomo-sdk-android/issues/92
     */
    @Test
    public void testLastScreenUrl() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);

        tracker.track(new TrackMe());
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("http://this.is.our.package/", mCaptor.getValue().get(QueryParams.URL_PATH));

        tracker.track(new TrackMe().set(QueryParams.URL_PATH, "http://some.thing.com/foo/bar"));
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals("http://some.thing.com/foo/bar", mCaptor.getValue().get(QueryParams.URL_PATH));

        tracker.track(new TrackMe().set(QueryParams.URL_PATH, "http://some.other/thing"));
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        assertEquals("http://some.other/thing", mCaptor.getValue().get(QueryParams.URL_PATH));

        tracker.track(new TrackMe());
        verify(mDispatcher, times(4)).submit(mCaptor.capture());
        assertEquals("http://some.other/thing", mCaptor.getValue().get(QueryParams.URL_PATH));

        tracker.track(new TrackMe().set(QueryParams.URL_PATH, "thang"));
        verify(mDispatcher, times(5)).submit(mCaptor.capture());
        assertEquals("http://this.is.our.package/thang", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testSetDispatchInterval() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setDispatchInterval(1);
        verify(mDispatcher).setDispatchInterval(1);
        tracker.getDispatchInterval();
        verify(mDispatcher).getDispatchInterval();
    }

    @Test
    public void testSetDispatchTimeout() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        int timeout = 1337;
        tracker.setDispatchTimeout(timeout);
        verify(mDispatcher).setConnectionTimeOut(timeout);
        tracker.getDispatchTimeout();
        verify(mDispatcher).getConnectionTimeOut();
    }

    @Test
    public void testGetOfflineCacheAge_defaultValue() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(24 * 60 * 60 * 1000, tracker.getOfflineCacheAge());
    }

    @Test
    public void testSetOfflineCacheAge() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setOfflineCacheAge(80085);
        assertEquals(80085, tracker.getOfflineCacheAge());
    }

    @Test
    public void testGetOfflineCacheSize_defaultValue() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(4 * 1024 * 1024, tracker.getOfflineCacheSize());
    }

    @Test
    public void testSetOfflineCacheSize() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setOfflineCacheSize(16 * 1000 * 1000);
        assertEquals(16 * 1000 * 1000, tracker.getOfflineCacheSize());
    }

    @Test
    public void testDispatchMode_default() {
        mTrackerPreferences.edit().clear();
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(DispatchMode.ALWAYS, tracker.getDispatchMode());
        verify(mDispatcher, times(1)).setDispatchMode(DispatchMode.ALWAYS);
    }

    @Test
    public void testDispatchMode_change() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setDispatchMode(DispatchMode.WIFI_ONLY);
        assertEquals(DispatchMode.WIFI_ONLY, tracker.getDispatchMode());
        verify(mDispatcher, times(1)).setDispatchMode(DispatchMode.WIFI_ONLY);
    }

    @Test
    public void testDispatchMode_fallback() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.getPreferences().edit().putString(Tracker.PREF_KEY_DISPATCHER_MODE, "lol").apply();
        assertEquals(DispatchMode.ALWAYS, tracker.getDispatchMode());
        verify(mDispatcher, times(1)).setDispatchMode(DispatchMode.ALWAYS);
    }

    @Test
    public void testSetDispatchMode_propagation() {
        mTrackerPreferences.edit().clear();
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        verify(mDispatcher, times(1)).setDispatchMode(any());
    }

    @Test
    public void testSetDispatchMode_propagation_change() {
        mTrackerPreferences.edit().clear();
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setDispatchMode(DispatchMode.WIFI_ONLY);
        tracker.setDispatchMode(DispatchMode.WIFI_ONLY);
        assertEquals(DispatchMode.WIFI_ONLY, tracker.getDispatchMode());
        verify(mDispatcher, times(2)).setDispatchMode(DispatchMode.WIFI_ONLY);
        verify(mDispatcher, times(3)).setDispatchMode(any());
    }

    @Test
    public void testSetDispatchMode_exception() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setDispatchMode(DispatchMode.WIFI_ONLY); // This is persisted
        tracker.setDispatchMode(DispatchMode.EXCEPTION); // This isn't
        assertEquals(DispatchMode.EXCEPTION, tracker.getDispatchMode());
        verify(mDispatcher, times(1)).setDispatchMode(DispatchMode.EXCEPTION);

        tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(DispatchMode.WIFI_ONLY, tracker.getDispatchMode());
    }

    @Test
    public void testsetDispatchGzip() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setDispatchGzipped(true);
        verify(mDispatcher).setDispatchGzipped(true);
    }

    @Test
    public void testOptOut_set() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setOptOut(true);
        verify(mDispatcher).clear();
        assertTrue(tracker.isOptOut());
        tracker.setOptOut(false);
        assertFalse(tracker.isOptOut());
    }

    @Test
    public void testOptOut_init() {
        mTrackerPreferences.edit().putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, false).apply();
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertFalse(tracker.isOptOut());
        mTrackerPreferences.edit().putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, true).apply();
        tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertTrue(tracker.isOptOut());
    }

    @Test
    public void testDispatch() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.dispatch();
        verify(mDispatcher).forceDispatch();
        tracker.dispatch();
        verify(mDispatcher, times(2)).forceDispatch();
    }

    @Test
    public void testDispatch_optOut() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setOptOut(true);
        tracker.dispatch();
        verify(mDispatcher, never()).forceDispatch();
        tracker.setOptOut(false);
        tracker.dispatch();
        verify(mDispatcher).forceDispatch();
    }

    @Test
    public void testGetSiteId() {
        when(mTrackerBuilder.getSiteId()).thenReturn(11);
        assertEquals(new Tracker(mMatomo, mTrackerBuilder).getSiteId(), 11);
    }

    @Test
    public void testGetMatomo() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(mMatomo, tracker.getMatomo());
    }

    @Test
    public void testSetURL() {
        when(mTrackerBuilder.getApplicationBaseUrl()).thenReturn("http://test.com/");
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);

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
    public void testApplicationDomain() {
        when(mTrackerBuilder.getApplicationBaseUrl()).thenReturn("http://my-domain.com");
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);

        TrackHelper.track().screen("test/test").title("Test title").with(tracker);
        verify(mDispatcher).submit(mCaptor.capture());
        validateDefaultQuery(mCaptor.getValue());
        assertEquals("http://my-domain.com/test/test", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVisitorId_invalid_short() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        String tooShortVisitorId = "0123456789ab";
        tracker.setVisitorId(tooShortVisitorId);
        assertNotEquals(tooShortVisitorId, tracker.getVisitorId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVisitorId_invalid_long() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        String tooLongVisitorId = "0123456789abcdefghi";
        tracker.setVisitorId(tooLongVisitorId);
        assertNotEquals(tooLongVisitorId, tracker.getVisitorId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVisitorId_invalid_charset() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        String invalidCharacterVisitorId = "01234-6789-ghief";
        tracker.setVisitorId(invalidCharacterVisitorId);
        assertNotEquals(invalidCharacterVisitorId, tracker.getVisitorId());
    }

    @Test
    public void testVisitorId_init() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertThat(tracker.getVisitorId(), is(notNullValue()));
    }

    @Test
    public void testVisitorId_restore() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertThat(tracker.getVisitorId(), is(notNullValue()));
        String visitorId = tracker.getVisitorId();

        tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertThat(tracker.getVisitorId(), is(visitorId));
    }

    @Test
    public void testVisitorId_dispatch() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        String visitorId = "0123456789abcdef";
        tracker.setVisitorId(visitorId);
        assertEquals(visitorId, tracker.getVisitorId());

        tracker.track(new TrackMe());
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals(visitorId, mCaptor.getValue().get(QueryParams.VISITOR_ID));

        tracker.track(new TrackMe());
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals(visitorId, mCaptor.getValue().get(QueryParams.VISITOR_ID));
    }

    @Test
    public void testUserID_init() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.USER_ID));
        assertNull(tracker.getUserId());
    }

    @Test
    public void testUserID_restore() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertNull(tracker.getUserId());
        tracker.setUserId("cake");
        assertThat(tracker.getUserId(), is("cake"));

        tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertThat(tracker.getUserId(), is("cake"));
        assertThat(tracker.getDefaultTrackMe().get(QueryParams.USER_ID), is("cake"));
    }

    @Test
    public void testUserID_invalid() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertNull(tracker.getUserId());

        tracker.setUserId("test");
        assertEquals(tracker.getUserId(), "test");

        tracker.setUserId("");
        assertEquals(tracker.getUserId(), "test");

        tracker.setUserId(null);
        assertNull(tracker.getUserId());

        String uuid = UUID.randomUUID().toString();
        tracker.setUserId(uuid);
        assertEquals(uuid, tracker.getUserId());
        assertEquals(uuid, tracker.getUserId());
    }

    @Test
    public void testUserID_dispatch() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        String uuid = UUID.randomUUID().toString();
        tracker.setUserId(uuid);

        tracker.track(new TrackMe());
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals(uuid, mCaptor.getValue().get(QueryParams.USER_ID));

        tracker.track(new TrackMe());
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals(uuid, mCaptor.getValue().get(QueryParams.USER_ID));
    }

    @Test
    public void testGetResolution() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("480x800", mCaptor.getValue().get(QueryParams.SCREEN_RESOLUTION));
    }

    @Test
    public void testSetNewSession() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));

        tracker.startNewSession();
        TrackHelper.track().screen("").with(tracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));
    }

    @Test
    public void testSetNewSessionRaceCondition() {
        for (int retry = 0; retry < 5; retry++) {
            final List<TrackMe> trackMes = Collections.synchronizedList(new ArrayList<>());
            doAnswer(invocation -> {
                trackMes.add(invocation.getArgument(0));
                return null;
            }).when(mDispatcher).submit(any(TrackMe.class));
            final Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
            tracker.setDispatchInterval(0);
            int count = 20;
            for (int i = 0; i < count; i++) {
                new Thread(() -> {
                    TestHelper.sleep(10);
                    TrackHelper.track().screen("Test").with(tracker);
                }).start();
            }
            TestHelper.sleep(500);
            assertEquals(count, trackMes.size());
            int found = 0;
            for (TrackMe trackMe : trackMes) {
                if (trackMe.get(QueryParams.SESSION_START) != null) found++;
            }
            assertEquals(1, found);
        }
    }

    @Test
    public void testSetSessionTimeout() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setSessionTimeout(10000);

        TrackHelper.track().screen("test1").with(tracker);
        assertThat(tracker.getLastEventX().get(QueryParams.SESSION_START), notNullValue());

        TrackHelper.track().screen("test2").with(tracker);
        assertThat(tracker.getLastEventX().get(QueryParams.SESSION_START), nullValue());

        tracker.setSessionTimeout(0);
        TestHelper.sleep(1);
        TrackHelper.track().screen("test3").with(tracker);
        assertThat(tracker.getLastEventX().get(QueryParams.SESSION_START), notNullValue());

        tracker.setSessionTimeout(10000);
        assertEquals(tracker.getSessionTimeout(), 10000);
        TrackHelper.track().screen("test3").with(tracker);
        assertThat(tracker.getLastEventX().get(QueryParams.SESSION_START), nullValue());
    }

    @Test
    public void testCheckSessionTimeout() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        tracker.setSessionTimeout(0);
        TrackHelper.track().screen("test").with(tracker);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));
        TestHelper.sleep(1);
        TrackHelper.track().screen("test").with(tracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals("1", mCaptor.getValue().get(QueryParams.SESSION_START));
        tracker.setSessionTimeout(60000);
        TrackHelper.track().screen("test").with(tracker);
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        assertNull(mCaptor.getValue().get(SESSION_START));
    }

    @Test
    public void testReset() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        Tracker.Callback callback = new Tracker.Callback() {
            @Nullable
            @Override
            public TrackMe onTrack(TrackMe trackMe) {
                return null;
            }
        };
        tracker.addTrackingCallback(callback);
        tracker.getDefaultTrackMe().set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, "custom1");
        tracker.getDefaultTrackMe().set(QueryParams.CAMPAIGN_NAME, "campaign_name");
        tracker.getDefaultTrackMe().set(QueryParams.CAMPAIGN_KEYWORD, "campaign_keyword");

        TrackHelper.track().screen("test1").with(tracker);
        tracker.startNewSession();
        TrackHelper.track().screen("test2").with(tracker);

        String preResetDefaultVisitorId = tracker.getDefaultTrackMe().get(VISITOR_ID);
        String preResetFirstVisitTimestamp = tracker.getDefaultTrackMe().get(FIRST_VISIT_TIMESTAMP);
        String preResetTotalNumberOfVisits = tracker.getDefaultTrackMe().get(TOTAL_NUMBER_OF_VISITS);
        String preResetPreviousVisitTimestamp = tracker.getDefaultTrackMe().get(PREVIOUS_VISIT_TIMESTAMP);

        tracker.reset();

        SharedPreferences prefs = tracker.getPreferences();

        assertNotEquals(preResetDefaultVisitorId, tracker.getVisitorId());
        assertNotEquals(preResetDefaultVisitorId, tracker.getDefaultTrackMe().get(VISITOR_ID));
        assertNotEquals(preResetDefaultVisitorId, prefs.getString(Tracker.PREF_KEY_TRACKER_VISITORID, ""));

        assertNotEquals(preResetFirstVisitTimestamp, tracker.getDefaultTrackMe().get(FIRST_VISIT_TIMESTAMP));
        assertNotEquals(Long.parseLong(preResetFirstVisitTimestamp), prefs.getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1));

        assertNotEquals(preResetPreviousVisitTimestamp, tracker.getDefaultTrackMe().get(PREVIOUS_VISIT_TIMESTAMP));
        assertNotEquals(Long.parseLong(preResetPreviousVisitTimestamp), prefs.getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1));

        assertNotEquals(preResetTotalNumberOfVisits, tracker.getDefaultTrackMe().get(TOTAL_NUMBER_OF_VISITS));
        assertNotEquals(preResetTotalNumberOfVisits, prefs.getString(Tracker.PREF_KEY_TRACKER_VISITCOUNT, ""));

        assertNull(tracker.getDefaultTrackMe().get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.CAMPAIGN_NAME));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.CAMPAIGN_KEYWORD));
    }

    @Test
    public void testTrackerEquals() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackerBuilder builder2 = mock(TrackerBuilder.class);
        when(builder2.getApiUrl()).thenReturn("http://localhost");
        when(builder2.getSiteId()).thenReturn(100);
        when(builder2.getTrackerName()).thenReturn("Default Tracker");
        Tracker tracker2 = new Tracker(mMatomo, builder2);

        TrackerBuilder builder3 = mock(TrackerBuilder.class);
        when(builder3.getApiUrl()).thenReturn("http://example.com");
        when(builder3.getSiteId()).thenReturn(11);
        when(builder3.getTrackerName()).thenReturn("Default Tracker");
        Tracker tracker3 = new Tracker(mMatomo, builder3);

        assertNotNull(tracker);
        assertNotEquals(tracker, tracker2);
        assertEquals(tracker, tracker3);
    }

    @Test
    public void testTrackerHashCode() {
        assertEquals(new Tracker(mMatomo, mTrackerBuilder).hashCode(), new Tracker(mMatomo, mTrackerBuilder).hashCode());
    }

    @Test
    public void testUrlPathCorrection() {
        when(mTrackerBuilder.getApplicationBaseUrl()).thenReturn("https://package/");
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        String[] paths = new String[]{null, "", "/",};
        for (String path : paths) {
            TrackMe trackMe = new TrackMe();
            trackMe.set(QueryParams.URL_PATH, path);
            tracker.track(trackMe);
            assertEquals("https://package/", trackMe.get(QueryParams.URL_PATH));
        }
    }

    @Test
    public void testSetUserAgent() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        assertEquals("aUserAgent", trackMe.get(QueryParams.USER_AGENT));

        // Custom developer specified useragent
        trackMe = new TrackMe();
        String customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0";
        trackMe.set(QueryParams.USER_AGENT, customUserAgent);
        tracker.track(trackMe);
        assertEquals(customUserAgent, trackMe.get(QueryParams.USER_AGENT));

        // Modifying default TrackMe, no USER_AGENT
        trackMe = new TrackMe();
        tracker.getDefaultTrackMe().set(QueryParams.USER_AGENT, null);
        tracker.track(trackMe);
        assertNull(trackMe.get(QueryParams.USER_AGENT));
    }

    @Test
    public void testFirstVisitTimeStamp() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(-1, tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1));

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher).submit(mCaptor.capture());
        TrackMe trackMe1 = mCaptor.getValue();
        TestHelper.sleep(10);
        // make sure we are tracking in seconds
        assertTrue(Math.abs((System.currentTimeMillis() / 1000) - Long.parseLong(trackMe1.get(FIRST_VISIT_TIMESTAMP))) < 2);

        tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        TrackMe trackMe2 = mCaptor.getValue();
        assertEquals(Long.parseLong(trackMe1.get(FIRST_VISIT_TIMESTAMP)), Long.parseLong(trackMe2.get(FIRST_VISIT_TIMESTAMP)));
        assertEquals(tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1), Long.parseLong(trackMe1.get(FIRST_VISIT_TIMESTAMP)));
    }

    @Test
    public void testTotalVisitCount() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(-1, tracker.getPreferences().getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher).submit(mCaptor.capture());
        assertEquals(1, Integer.parseInt(mCaptor.getValue().get(QueryParams.TOTAL_NUMBER_OF_VISITS)));

        tracker = new Tracker(mMatomo, mTrackerBuilder);
        assertEquals(1, tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
        assertNull(tracker.getDefaultTrackMe().get(QueryParams.TOTAL_NUMBER_OF_VISITS));
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        assertEquals(2, Integer.parseInt(mCaptor.getValue().get(QueryParams.TOTAL_NUMBER_OF_VISITS)));
        assertEquals(2, tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1));
    }

    @Test
    public void testVisitCountMultipleThreads() throws Exception {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        int threadCount = 1000;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                TestHelper.sleep(new Random().nextInt(20 - 0) + 0);
                TrackHelper.track().event("TestCategory", "TestAction").with(new Tracker(mMatomo, mTrackerBuilder));
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        assertEquals(threadCount, mTrackerPreferences.getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, 0));
    }

    @Test
    public void testSessionStartRaceCondition() throws Exception {
        final List<TrackMe> trackMes = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            trackMes.add(invocation.getArgument(0));
            return null;
        }).when(mDispatcher).submit(any(TrackMe.class));
        when(mDispatcher.getConnectionTimeOut()).thenReturn(1000);
        for (int i = 0; i < 1000; i++) {
            trackMes.clear();
            final Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
            final CountDownLatch countDownLatch = new CountDownLatch(10);
            for (int j = 0; j < 10; j++) {
                new Thread(() -> {
                    try {
                        TestHelper.sleep(new Random().nextInt(4 - 0) + 0);
                        TrackMe trackMe = new TrackMe()
                                .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_VALUE, 1);
                        tracker.track(trackMe);
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }
                }).start();
            }
            countDownLatch.await();
            for (TrackMe out : trackMes) {
                if (trackMes.indexOf(out) == 0) {
                    assertNotNull(i + "#" + out.toMap().size(), out.get(QueryParams.LANGUAGE));
                    assertNotNull(out.get(FIRST_VISIT_TIMESTAMP));
                    assertNotNull(out.get(SESSION_START));
                } else {
                    assertNull(out.get(FIRST_VISIT_TIMESTAMP));
                    assertNull(out.get(SESSION_START));
                }
            }
        }
    }

    @Test
    public void testFirstVisitMultipleThreads() throws Exception {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        int threadCount = 100;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        final List<Long> firstVisitTimes = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                TestHelper.sleep(new Random().nextInt(20 - 0) + 0);
                TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
                long firstVisit = Long.parseLong(tracker.getDefaultTrackMe().get(FIRST_VISIT_TIMESTAMP));
                firstVisitTimes.add(firstVisit);
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        for (Long firstVisit : firstVisitTimes) assertEquals(firstVisitTimes.get(0), firstVisit);
    }

    @Test
    public void testPreviousVisits() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        final List<Long> previousVisitTimes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {


            TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
            String previousVisit = tracker.getDefaultTrackMe().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP);
            if (previousVisit != null)
                previousVisitTimes.add(Long.parseLong(previousVisit));
            TestHelper.sleep(1010);

        }
        assertFalse(previousVisitTimes.contains(0L));
        long lastTime = 0L;
        for (Long time : previousVisitTimes) {
            assertTrue(lastTime < time);
            lastTime = time;
        }
    }

    @Test
    public void testPreviousVisit() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        // No timestamp yet
        assertEquals(-1, tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1));
        tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher).submit(mCaptor.capture());
        long _startTime = System.currentTimeMillis() / 1000;
        // There was no previous visit
        assertNull(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP));
        TestHelper.sleep(1000);

        // After the first visit we now have a timestamp for the previous visit
        long previousVisit = tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
        assertTrue(previousVisit - _startTime < 2000);
        assertNotEquals(-1, previousVisit);
        tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher, times(2)).submit(mCaptor.capture());
        // Transmitted timestamp is the one from the first visit visit
        assertEquals(previousVisit, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        TestHelper.sleep(1000);
        tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher, times(3)).submit(mCaptor.capture());
        // Now the timestamp changed as this is the 3rd visit.
        assertNotEquals(previousVisit, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
        TestHelper.sleep(1000);

        previousVisit = tracker.getPreferences().getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
        tracker = new Tracker(mMatomo, mTrackerBuilder);
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker);
        verify(mDispatcher, times(4)).submit(mCaptor.capture());
        // Just make sure the timestamp in the 4th visit is from the 3rd visit
        assertEquals(previousVisit, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));

        // Test setting a custom timestamp
        TrackMe custom = new TrackMe();
        custom.set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, 1000L);
        tracker.track(custom);
        verify(mDispatcher, times(5)).submit(mCaptor.capture());
        assertEquals(1000L, Long.parseLong(mCaptor.getValue().get(QueryParams.PREVIOUS_VISIT_TIMESTAMP)));
    }

    @Test
    public void testTrackingCallback() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        Tracker.Callback callback = mock(Tracker.Callback.class);

        TrackMe pre = new TrackMe();
        tracker.track(pre);
        verify(mDispatcher).submit(pre);
        verify(callback, never()).onTrack(mCaptor.capture());

        reset(mDispatcher, callback);
        tracker.addTrackingCallback(callback);
        tracker.track(new TrackMe());
        verify(callback).onTrack(mCaptor.capture());
        verify(mDispatcher, never()).submit(any());

        reset(mDispatcher, callback);
        TrackMe orig = new TrackMe();
        TrackMe replaced = new TrackMe().set("some", "thing");
        when(callback.onTrack(orig)).thenReturn(replaced);
        tracker.track(orig);
        verify(callback).onTrack(orig);
        verify(mDispatcher).submit(replaced);

        reset(mDispatcher, callback);
        TrackMe post = new TrackMe();
        tracker.removeTrackingCallback(callback);
        tracker.track(post);
        verify(callback, never()).onTrack(any());
        verify(mDispatcher).submit(post);
    }

    @Test
    public void testTrackingCallbacks() {
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        Tracker.Callback callback1 = mock(Tracker.Callback.class);
        Tracker.Callback callback2 = mock(Tracker.Callback.class);

        TrackMe orig = new TrackMe();
        TrackMe replaced = new TrackMe();
        when(callback1.onTrack(orig)).thenReturn(replaced);
        when(callback2.onTrack(replaced)).thenReturn(replaced);

        tracker.addTrackingCallback(callback1);
        tracker.addTrackingCallback(callback1);
        tracker.addTrackingCallback(callback2);
        tracker.track(orig);
        verify(callback1).onTrack(orig);
        verify(callback2).onTrack(replaced);
        verify(mDispatcher).submit(replaced);

        tracker.removeTrackingCallback(callback1);
        tracker.track(orig);

        verify(callback2).onTrack(orig);
    }

    private static void validateDefaultQuery(TrackMe params) {
        assertEquals(params.get(QueryParams.SITE_ID), "11");
        assertEquals(params.get(QueryParams.RECORD), "1");
        assertEquals(params.get(QueryParams.SEND_IMAGE), "0");
        assertEquals(params.get(QueryParams.VISITOR_ID).length(), 16);
        assertTrue(params.get(QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(QueryParams.RANDOM_NUMBER)) > 0);
    }

    @Test
    public void testCustomDispatcherFactory() {
        Dispatcher dispatcher = mock(Dispatcher.class);
        DispatcherFactory factory = mock(DispatcherFactory.class);
        when(factory.build(any(Tracker.class))).thenReturn(dispatcher);
        when(mMatomo.getDispatcherFactory()).thenReturn(factory);
        Tracker tracker = new Tracker(mMatomo, mTrackerBuilder);
        verify(factory).build(tracker);
    }
}
