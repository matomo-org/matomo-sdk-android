/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk;

import android.annotation.SuppressLint;
import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.dispatcher.DefaultDispatcher;
import org.matomo.sdk.dispatcher.DefaultDispatcherFactory;
import org.matomo.sdk.dispatcher.Dispatcher;
import org.matomo.sdk.dispatcher.DispatcherFactory;
import org.matomo.sdk.dispatcher.EventCache;
import org.matomo.sdk.dispatcher.EventDiskCache;
import org.matomo.sdk.dispatcher.Packet;
import org.matomo.sdk.dispatcher.PacketFactory;
import org.matomo.sdk.dispatcher.PacketSender;
import org.matomo.sdk.extra.TrackHelper;
import org.matomo.sdk.tools.Connectivity;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;

import testhelpers.BaseTest;
import testhelpers.FullEnvTestRunner;
import testhelpers.MatomoTestApplication;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Config(sdk = 28, manifest = Config.NONE, application = MatomoTestApplication.class)
@RunWith(FullEnvTestRunner.class)
public class MatomoTest extends BaseTest {

    @Test
    public void testNewTracker() {
        MatomoTestApplication app = ApplicationProvider.getApplicationContext();
        Tracker tracker = app.onCreateTrackerConfig().build(Matomo.getInstance(ApplicationProvider.getApplicationContext()));
        assertNotNull(tracker);
        assertEquals(app.onCreateTrackerConfig().getApiUrl(), tracker.getAPIUrl());
        assertEquals(app.onCreateTrackerConfig().getSiteId(), tracker.getSiteId());
    }

    @Test
    public void testNormalTracker() {
        Matomo matomo = Matomo.getInstance(ApplicationProvider.getApplicationContext());
        Tracker tracker = new TrackerBuilder("http://test/matomo.php", 1, "Default Tracker").build(matomo);
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
        MatomoTestApplication app = ApplicationProvider.getApplicationContext();
        final PacketSender packetSender = mock(PacketSender.class);
        app.getMatomo().setDispatcherFactory(new DefaultDispatcherFactory() {
            @Override
            public Dispatcher build(Tracker tracker) {
                return new DefaultDispatcher(
                        new EventCache(new EventDiskCache(tracker)),
                        new Connectivity(tracker.getMatomo().getContext()),
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

        final Matomo matomo = Matomo.getInstance(ApplicationProvider.getApplicationContext());
        assertEquals(matomo.getTrackerPreferences(tracker1), matomo.getTrackerPreferences(tracker1));
        assertNotEquals(matomo.getTrackerPreferences(tracker1), matomo.getTrackerPreferences(tracker2));
        assertEquals(matomo.getTrackerPreferences(tracker1), matomo.getTrackerPreferences(tracker3));
    }

    @Test
    public void testSetDispatcherFactory() {
        final Matomo matomo = Matomo.getInstance(ApplicationProvider.getApplicationContext());
        Dispatcher dispatcher = mock(Dispatcher.class);
        DispatcherFactory factory = mock(DispatcherFactory.class);
        when(factory.build(any(Tracker.class))).thenReturn(dispatcher);
        assertThat(matomo.getDispatcherFactory(), is(not(nullValue())));
        matomo.setDispatcherFactory(factory);
        assertThat(matomo.getDispatcherFactory(), is(factory));
    }

}
