/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.dispatcher.Packet;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.PiwikTestApplication;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class PiwikTest {

    @Test
    public void testNewTracker() throws Exception {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        Tracker tracker = Piwik.getInstance(Robolectric.application).newTracker(app.getTrackerUrl(), app.getSiteId());
        assertNotNull(tracker);
        assertEquals(new URL(app.getTrackerUrl() + "/piwik.php"), tracker.getAPIUrl());
        assertEquals(app.getSiteId(), Integer.valueOf(tracker.getSiteId()));
    }

    @Test
    public void testNormalTracker() throws Exception {
        Piwik piwik = Piwik.getInstance(Robolectric.application);
        Tracker tracker = piwik.newTracker("http://test", 1);
        assertEquals("http://test/piwik.php", tracker.getAPIUrl().toString());
        assertEquals(1, tracker.getSiteId());

        tracker = piwik.newTracker("http://test/piwik.php", 1);
        assertEquals("http://test/piwik.php", tracker.getAPIUrl().toString());

        tracker = piwik.newTracker("http://test/piwik-proxy.php", 1);
        assertEquals("http://test/piwik-proxy.php", tracker.getAPIUrl().toString());
    }

    @Test
    public void testOptions() throws Exception {
        Piwik piwik = Piwik.getInstance(Robolectric.application);
        piwik.setOptOut(true);
        assertTrue(piwik.isOptOut());
        piwik.setOptOut(false);
        assertFalse(piwik.isOptOut());
    }

    @Test
    public void testLowMemoryDispatch() throws Exception {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        Tracker tracker = app.getTracker();
        tracker.setDryRunTarget(Collections.synchronizedList(new ArrayList<Packet>()));
        assertNotNull(tracker);
        tracker.setDispatchInterval(-1);
        tracker.track(TrackHelper.track().screen("test").build());
        Thread.sleep(50);
        assertTrue(tracker.getDryRunTarget().isEmpty());
        app.onTrimMemory(Application.TRIM_MEMORY_UI_HIDDEN);
        Thread.sleep(50);
        assertFalse(tracker.getDryRunTarget().isEmpty());
    }
}