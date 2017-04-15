/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.annotation.SuppressLint;
import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.dispatcher.Packet;
import org.piwik.sdk.extra.TrackHelper;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.PiwikTestApplication;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class PiwikTest {

    @Test
    public void testNewTracker() throws Exception {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        Tracker tracker = Piwik.getInstance(Robolectric.application).newTracker(app.onCreateTrackerConfig());
        assertNotNull(tracker);
        assertEquals(app.onCreateTrackerConfig().getApiUrl(), tracker.getAPIUrl());
        assertEquals(app.onCreateTrackerConfig().getSiteId(), tracker.getSiteId());
    }

    @Test
    public void testNormalTracker() throws Exception {
        Piwik piwik = Piwik.getInstance(Robolectric.application);
        Tracker tracker = piwik.newTracker(new TrackerConfig("http://test", 1, "Default Tracker"));
        assertEquals("http://test/piwik.php", tracker.getAPIUrl().toString());
        assertEquals(1, tracker.getSiteId());
    }

    @Test
    public void testTrackerNaming() {
        // TODO can we somehow detect naming collisions on tracker creation?
        // Would probably requiring us to track created trackers
    }

    @SuppressLint("InlinedApi")
    @Test
    public void testLowMemoryDispatch() throws Exception {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        Tracker tracker = app.getTracker();
        assertNotNull(tracker);
        tracker.setDryRunTarget(Collections.synchronizedList(new ArrayList<Packet>()));
        tracker.setDispatchInterval(-1);

        tracker.track(TrackHelper.track().screen("test").build());
        tracker.dispatch();
        Thread.sleep(50);
        assertFalse(tracker.getDryRunTarget().isEmpty());
        tracker.getDryRunTarget().clear();

        tracker.track(TrackHelper.track().screen("test").build());
        Thread.sleep(50);
        assertTrue(tracker.getDryRunTarget().isEmpty());
        app.onTrimMemory(Application.TRIM_MEMORY_UI_HIDDEN);
        Thread.sleep(50);
        assertFalse(tracker.getDryRunTarget().isEmpty());
    }

    @Test
    public void testGetSettings() {
        Tracker tracker1 = mock(Tracker.class);
        when(tracker1.getName()).thenReturn("1");
        Tracker tracker2 = mock(Tracker.class);
        when(tracker2.getName()).thenReturn("2");
        Tracker tracker3 = mock(Tracker.class);
        when(tracker3.getName()).thenReturn("1");

        final Piwik piwik = Piwik.getInstance(Robolectric.application);
        assertEquals(piwik.getTrackerPreferences(tracker1), piwik.getTrackerPreferences(tracker1));
        assertNotEquals(piwik.getTrackerPreferences(tracker1), piwik.getTrackerPreferences(tracker2));
        assertEquals(piwik.getTrackerPreferences(tracker1), piwik.getTrackerPreferences(tracker3));
    }

}