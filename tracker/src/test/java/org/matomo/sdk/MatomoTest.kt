/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk

import android.annotation.SuppressLint
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.matomo.sdk.Matomo.Companion.getInstance
import org.matomo.sdk.dispatcher.DefaultDispatcher
import org.matomo.sdk.dispatcher.DefaultDispatcherFactory
import org.matomo.sdk.dispatcher.Dispatcher
import org.matomo.sdk.dispatcher.DispatcherFactory
import org.matomo.sdk.dispatcher.EventCache
import org.matomo.sdk.dispatcher.EventDiskCache
import org.matomo.sdk.dispatcher.Packet
import org.matomo.sdk.dispatcher.PacketFactory
import org.matomo.sdk.dispatcher.PacketSender
import org.matomo.sdk.extra.TrackHelper
import org.matomo.sdk.tools.Connectivity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.annotation.Config
import testhelpers.BaseTest
import testhelpers.FullEnvTestRunner
import testhelpers.MatomoTestApplication

@Config(sdk = [28], manifest = Config.NONE, application = MatomoTestApplication::class)
@RunWith(
    FullEnvTestRunner::class
)
class MatomoTest : BaseTest() {
    @Test
    fun testNewTracker() {
        val app = ApplicationProvider.getApplicationContext<MatomoTestApplication>()
        val tracker = app.onCreateTrackerConfig().build(getInstance(ApplicationProvider.getApplicationContext()))
        Assert.assertNotNull(tracker)
        Assert.assertEquals(app.onCreateTrackerConfig().apiUrl, tracker.apiUrl)
        Assert.assertEquals(app.onCreateTrackerConfig().siteId.toLong(), tracker.siteId.toLong())
    }

    @Test
    fun testNormalTracker() {
        val matomo = getInstance(ApplicationProvider.getApplicationContext())
        val tracker = TrackerBuilder("http://test/matomo.php", 1, "Default Tracker").build(matomo)
        Assert.assertEquals("http://test/matomo.php", tracker.apiUrl)
        Assert.assertEquals(1, tracker.siteId.toLong())
    }

    @Test
    fun testTrackerNaming() {
        // TODO can we somehow detect naming collisions on tracker creation?
        // Would probably requiring us to track created trackers
    }

    @SuppressLint("InlinedApi")
    @Test
    fun testLowMemoryDispatch() {
        val app = ApplicationProvider.getApplicationContext<MatomoTestApplication>()
        val packetSender = Mockito.mock(PacketSender::class.java)
        app.matomo.dispatcherFactory = object : DefaultDispatcherFactory() {
            override fun build(tracker: Tracker): Dispatcher {
                return DefaultDispatcher(
                    EventCache(EventDiskCache(tracker)),
                    Connectivity(tracker.matomo.context),
                    PacketFactory(tracker.apiUrl),
                    packetSender
                )
            }
        }
        val tracker = app.tracker
        Assert.assertNotNull(tracker)
        tracker.setDispatchInterval(-1)

        tracker.track(TrackHelper.track().screen("test").build())
        tracker.dispatch()
        Mockito.verify(packetSender, Mockito.timeout(500).times(1)).send(ArgumentMatchers.any(Packet::class.java))

        tracker.track(TrackHelper.track().screen("test").build())
        Mockito.verify(packetSender, Mockito.timeout(500).times(1)).send(ArgumentMatchers.any(Packet::class.java))

        app.onTrimMemory(Application.TRIM_MEMORY_UI_HIDDEN)
        Mockito.verify(packetSender, Mockito.timeout(500).atLeast(2)).send(ArgumentMatchers.any(Packet::class.java))
    }

    @Test
    fun testGetSettings() {
        val tracker1 = Mockito.mock(Tracker::class.java)
        Mockito.`when`(tracker1.name).thenReturn("1")
        val tracker2 = Mockito.mock(Tracker::class.java)
        Mockito.`when`(tracker2.name).thenReturn("2")
        val tracker3 = Mockito.mock(Tracker::class.java)
        Mockito.`when`(tracker3.name).thenReturn("1")

        val matomo = getInstance(ApplicationProvider.getApplicationContext())
        Assert.assertEquals(matomo!!.getTrackerPreferences(tracker1), matomo.getTrackerPreferences(tracker1))
        Assert.assertNotEquals(matomo.getTrackerPreferences(tracker1), matomo.getTrackerPreferences(tracker2))
        Assert.assertEquals(matomo.getTrackerPreferences(tracker1), matomo.getTrackerPreferences(tracker3))
    }

    @Test
    fun testSetDispatcherFactory() {
        val matomo = getInstance(ApplicationProvider.getApplicationContext())
        val dispatcher = Mockito.mock(Dispatcher::class.java)
        val factory = Mockito.mock(DispatcherFactory::class.java)
        Mockito.`when`(factory.build(ArgumentMatchers.any(Tracker::class.java))).thenReturn(dispatcher)
        MatcherAssert.assertThat(matomo!!.dispatcherFactory, Matchers.`is`(Matchers.not(Matchers.nullValue())))
        matomo.dispatcherFactory = factory
        MatcherAssert.assertThat(matomo.dispatcherFactory, Matchers.`is`(factory))
    }
}
