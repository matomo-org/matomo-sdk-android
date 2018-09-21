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
import org.piwik.sdk.dispatcher.DefaultDispatcher;
import org.piwik.sdk.dispatcher.DefaultDispatcherFactory;
import org.piwik.sdk.dispatcher.Dispatcher;
import org.piwik.sdk.dispatcher.DispatcherFactory;
import org.piwik.sdk.dispatcher.EventCache;
import org.piwik.sdk.dispatcher.EventDiskCache;
import org.piwik.sdk.dispatcher.Packet;
import org.piwik.sdk.dispatcher.PacketFactory;
import org.piwik.sdk.dispatcher.PacketSender;
import org.piwik.sdk.extra.TrackHelper;
import org.piwik.sdk.tools.Connectivity;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import testhelpers.BaseTest;
import testhelpers.FullEnvTestRunner;
import testhelpers.PiwikTestApplication;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class PiwikTest extends BaseTest {

    @Test
    public void testNewTracker() {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        Tracker tracker = app.onCreateTrackerConfig().build(Piwik.getInstance(Robolectric.application));
        assertNotNull(tracker);
        assertEquals(app.onCreateTrackerConfig().getApiUrl(), tracker.getAPIUrl());
        assertEquals(app.onCreateTrackerConfig().getSiteId(), tracker.getSiteId());
    }

    @Test
    public void testNormalTracker() {
        Piwik piwik = Piwik.getInstance(Robolectric.application);
        Tracker tracker = new TrackerBuilder("http://test/matomo.php", 1, "Default Tracker").build(piwik);
        assertEquals("http://test/matomo.php", tracker.getAPIUrl());
        assertEquals(1, tracker.getSiteId());
    }

    @Test
    public void testTrackerNaming() {
        // TODO can we somehow detect naming collisions on tracker creation?
        // Would probably requiring us to track created trackers
    }

    @SuppressLint("InlinedApi")
    @Test
    public void testLowMemoryDispatch() {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        final PacketSender packetSender = mock(PacketSender.class);
        app.getPiwik().setDispatcherFactory(new DefaultDispatcherFactory() {
            @Override
            public Dispatcher build(Tracker tracker) {
                return new DefaultDispatcher(
                        new EventCache(new EventDiskCache(tracker)),
                        new Connectivity(tracker.getPiwik().getContext()),
                        new PacketFactory(tracker.getAPIUrl()),
                        packetSender
                );
            }
        });
        Tracker tracker = app.getTracker();
        assertNotNull(tracker);
        tracker.setDispatchInterval(-1);

        tracker.track(TrackHelper.track().screen("test").build());
        tracker.dispatch();
        verify(packetSender, timeout(500).times(1)).send(any(Packet.class));

        tracker.track(TrackHelper.track().screen("test").build());
        verify(packetSender, timeout(500).times(1)).send(any(Packet.class));

        app.onTrimMemory(Application.TRIM_MEMORY_UI_HIDDEN);
        verify(packetSender, timeout(500).atLeast(2)).send(any(Packet.class));
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

    @Test
    public void testSetDispatcherFactory() {
        final Piwik piwik = Piwik.getInstance(Robolectric.application);
        Dispatcher dispatcher = mock(Dispatcher.class);
        DispatcherFactory factory = mock(DispatcherFactory.class);
        when(factory.build(any(Tracker.class))).thenReturn(dispatcher);
        assertThat(piwik.getDispatcherFactory(), is(not(nullValue())));
        piwik.setDispatcherFactory(factory);
        assertThat(piwik.getDispatcherFactory(), is(factory));
    }

}
