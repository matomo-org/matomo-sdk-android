package org.matomo.sdk

import android.content.Context
import android.content.SharedPreferences
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.matomo.sdk.dispatcher.DispatchMode
import org.matomo.sdk.dispatcher.Dispatcher
import org.matomo.sdk.dispatcher.DispatcherFactory
import org.matomo.sdk.extra.TrackHelper
import org.matomo.sdk.tools.DeviceHelper
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import testhelpers.TestHelper
import testhelpers.TestPreferences
import java.util.Collections
import java.util.Random
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.math.abs

class TrackerTest {
    private var argumentCaptor: ArgumentCaptor<TrackMe> = ArgumentCaptor.forClass(TrackMe::class.java)

    @Mock
    var matomo: Matomo? = null

    @Mock
    var context: Context? = null

    @Mock
    var dispatcher: Dispatcher? = null

    @Mock
    var dispatcherFactory: DispatcherFactory? = null

    @Mock
    var deviceHelper: DeviceHelper? = null
    var trackerPreferences: SharedPreferences = TestPreferences()
    var preferences: SharedPreferences = TestPreferences()

    @Mock
    var mTrackerBuilder: TrackerBuilder? = null

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(matomo!!.context).thenReturn(context)
        Mockito.`when`(matomo!!.getTrackerPreferences(ArgumentMatchers.any(Tracker::class.java))).thenReturn(trackerPreferences)
        Mockito.`when`(matomo!!.preferences).thenReturn(preferences)
        Mockito.`when`(matomo!!.dispatcherFactory).thenReturn(dispatcherFactory)
        Mockito.`when`(dispatcherFactory!!.build(ArgumentMatchers.any(Tracker::class.java))).thenReturn(dispatcher)
        Mockito.`when`(matomo!!.deviceHelper).thenReturn(deviceHelper)
        Mockito.`when`(deviceHelper!!.resolution).thenReturn(intArrayOf(480, 800))
        Mockito.`when`(deviceHelper!!.userAgent).thenReturn("aUserAgent")
        Mockito.`when`(deviceHelper!!.userLanguage).thenReturn("en")

        val mApiUrl = "http://example.com"
        Mockito.`when`(mTrackerBuilder!!.apiUrl).thenReturn(mApiUrl)
        val mSiteId = 11
        Mockito.`when`(mTrackerBuilder!!.siteId).thenReturn(mSiteId)
        val mTrackerName = "Default Tracker"
        Mockito.`when`(mTrackerBuilder!!.trackerName).thenReturn(mTrackerName)
        Mockito.`when`(mTrackerBuilder!!.applicationBaseUrl).thenReturn("http://this.is.our.package/")

        trackerPreferences.edit().clear()
        preferences.edit().clear()
    }

    @Test
    fun testGetPreferences() {
        val tracker1 = Tracker(matomo!!, mTrackerBuilder!!)
        Mockito.verify(matomo)?.getTrackerPreferences(tracker1)
    }

    /**
     * https://github.com/matomo-org/matomo-sdk-android/issues/92
     */
    @Test
    fun testLastScreenUrl() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)

        tracker.track(TrackMe())
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals("http://this.is.our.package/", argumentCaptor.value[QueryParams.URL_PATH])

        tracker.track(TrackMe().set(QueryParams.URL_PATH, "http://some.thing.com/foo/bar"))
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        Assert.assertEquals("http://some.thing.com/foo/bar", argumentCaptor.value[QueryParams.URL_PATH])

        tracker.track(TrackMe().set(QueryParams.URL_PATH, "http://some.other/thing"))
        Mockito.verify(dispatcher, Mockito.times(3))?.submit(argumentCaptor.capture())
        Assert.assertEquals("http://some.other/thing", argumentCaptor.value[QueryParams.URL_PATH])

        tracker.track(TrackMe())
        Mockito.verify(dispatcher, Mockito.times(4))?.submit(argumentCaptor.capture())
        Assert.assertEquals("http://some.other/thing", argumentCaptor.value[QueryParams.URL_PATH])

        tracker.track(TrackMe().set(QueryParams.URL_PATH, "thang"))
        Mockito.verify(dispatcher, Mockito.times(5))?.submit(argumentCaptor.capture())
        Assert.assertEquals("http://this.is.our.package/thang", argumentCaptor.value[QueryParams.URL_PATH])
    }

    @Test
    fun testSetDispatchInterval() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.setDispatchInterval(1)
        Mockito.verify(dispatcher)?.dispatchInterval = 1
        tracker.dispatchInterval
        Mockito.verify(dispatcher)?.dispatchInterval
    }

    @Test
    fun testSetDispatchTimeout() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val timeout = 1337
        tracker.dispatchTimeout = timeout
        Mockito.verify(dispatcher)?.connectionTimeOut = timeout
        tracker.dispatchTimeout
        Mockito.verify(dispatcher)?.connectionTimeOut
    }

    @Test
    fun testGetOfflineCacheAge_defaultValue() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals((24 * 60 * 60 * 1000).toLong(), tracker.offlineCacheAge)
    }

    @Test
    fun testSetOfflineCacheAge() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.offlineCacheAge = 80085
        Assert.assertEquals(80085, tracker.offlineCacheAge)
    }

    @Test
    fun testGetOfflineCacheSize_defaultValue() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals((4 * 1024 * 1024).toLong(), tracker.offlineCacheSize)
    }

    @Test
    fun testSetOfflineCacheSize() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.offlineCacheSize = 16 * 1000 * 1000
        Assert.assertEquals((16 * 1000 * 1000).toLong(), tracker.offlineCacheSize)
    }

    @Test
    fun testDispatchMode_default() {
        trackerPreferences.edit().clear()
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals(DispatchMode.ALWAYS, tracker.dispatchMode)
        Mockito.verify(dispatcher, Mockito.times(1))?.dispatchMode = DispatchMode.ALWAYS
    }

    @Test
    fun testDispatchMode_change() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.dispatchMode = DispatchMode.WIFI_ONLY
        Assert.assertEquals(DispatchMode.WIFI_ONLY, tracker.dispatchMode)
        Mockito.verify(dispatcher, Mockito.times(1))?.dispatchMode =
            DispatchMode.WIFI_ONLY
    }

    @Test
    fun testDispatchMode_fallback() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.preferences!!.edit().putString(Tracker.PREF_KEY_DISPATCHER_MODE, "lol").apply()
        Assert.assertEquals(DispatchMode.ALWAYS, tracker.dispatchMode)
        Mockito.verify(dispatcher, Mockito.times(1))?.dispatchMode = DispatchMode.ALWAYS
    }

    @Test
    fun testSetDispatchMode_propagation() {
        trackerPreferences.edit().clear()
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Mockito.verify(dispatcher, Mockito.times(1))?.dispatchMode =
            ArgumentMatchers.any()
    }

    @Test
    fun testSetDispatchMode_propagation_change() {
        trackerPreferences.edit().clear()
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.dispatchMode = DispatchMode.WIFI_ONLY
        tracker.dispatchMode = DispatchMode.WIFI_ONLY
        Assert.assertEquals(DispatchMode.WIFI_ONLY, tracker.dispatchMode)
        Mockito.verify(dispatcher, Mockito.times(2))?.dispatchMode =
            DispatchMode.WIFI_ONLY
        Mockito.verify(dispatcher, Mockito.times(3))?.dispatchMode =
            ArgumentMatchers.any()
    }

    @Test
    fun testSetDispatchMode_exception() {
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.dispatchMode = DispatchMode.WIFI_ONLY // This is persisted
        tracker.dispatchMode = DispatchMode.EXCEPTION // This isn't
        Assert.assertEquals(DispatchMode.EXCEPTION, tracker.dispatchMode)
        Mockito.verify(dispatcher, Mockito.times(1))?.dispatchMode =
            DispatchMode.EXCEPTION

        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals(DispatchMode.WIFI_ONLY, tracker.dispatchMode)
    }

    @Test
    fun testsetDispatchGzip() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.setDispatchGzipped(true)
        Mockito.verify(dispatcher)?.dispatchGzipped = true
    }

    @Test
    fun testOptOut_set() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.isOptOut = true
        Assert.assertTrue(tracker.isOptOut)
        tracker.isOptOut = false
        Assert.assertFalse(tracker.isOptOut)
    }

    @Test
    fun testOptOut_init() {
        trackerPreferences.edit().putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, false).apply()
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertFalse(tracker.isOptOut)
        trackerPreferences.edit().putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, true).apply()
        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertTrue(tracker.isOptOut)
    }

    @Test
    fun testDispatch() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.dispatch()
        Mockito.verify(dispatcher)?.forceDispatch()
        tracker.dispatch()
        Mockito.verify(dispatcher, Mockito.times(2))?.forceDispatch()
    }

    @Test
    fun testDispatch_optOut() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.isOptOut = true
        tracker.dispatch()
        Mockito.verify(dispatcher, Mockito.never())?.forceDispatch()
        tracker.isOptOut = false
        tracker.dispatch()
        Mockito.verify(dispatcher)?.forceDispatch()
    }

    @Test
    fun testGetSiteId() {
        Mockito.`when`(mTrackerBuilder!!.siteId).thenReturn(11)
        Assert.assertEquals(Tracker(matomo!!, mTrackerBuilder!!).siteId.toLong(), 11)
    }

    @Test
    fun testGetMatomo() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals(matomo, tracker.matomo)
    }

    @Test
    fun testSetURL() {
        Mockito.`when`(mTrackerBuilder!!.applicationBaseUrl).thenReturn("http://test.com/")
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)

        val trackMe = TrackMe()
        tracker.track(trackMe)
        Assert.assertEquals("http://test.com/", trackMe[QueryParams.URL_PATH])

        trackMe[QueryParams.URL_PATH] = "me"
        tracker.track(trackMe)
        Assert.assertEquals("http://test.com/me", trackMe[QueryParams.URL_PATH])

        // override protocol
        trackMe[QueryParams.URL_PATH] = "https://my.com/secure"
        tracker.track(trackMe)
        Assert.assertEquals("https://my.com/secure", trackMe[QueryParams.URL_PATH])
    }

    @Test
    fun testApplicationDomain() {
        Mockito.`when`(mTrackerBuilder!!.applicationBaseUrl).thenReturn("http://my-domain.com")
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)

        TrackHelper.track().screen("test/test").title("Test title").with(tracker)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        validateDefaultQuery(argumentCaptor.value)
        Assert.assertEquals("http://my-domain.com/test/test", argumentCaptor.value[QueryParams.URL_PATH])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVisitorId_invalid_short() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val tooShortVisitorId = "0123456789ab"
        tracker.setVisitorId(tooShortVisitorId)
        Assert.assertNotEquals(tooShortVisitorId, tracker.visitorId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVisitorId_invalid_long() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val tooLongVisitorId = "0123456789abcdefghi"
        tracker.setVisitorId(tooLongVisitorId)
        Assert.assertNotEquals(tooLongVisitorId, tracker.visitorId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVisitorId_invalid_charset() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val invalidCharacterVisitorId = "01234-6789-ghief"
        tracker.setVisitorId(invalidCharacterVisitorId)
        Assert.assertNotEquals(invalidCharacterVisitorId, tracker.visitorId)
    }

    @Test
    fun testVisitorId_init() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        MatcherAssert.assertThat(tracker.visitorId, Is.`is`(Matchers.notNullValue()))
    }

    @Test
    fun testVisitorId_restore() {
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        MatcherAssert.assertThat(tracker.visitorId, Is.`is`(Matchers.notNullValue()))
        val visitorId = tracker.visitorId

        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        MatcherAssert.assertThat(tracker.visitorId, Is.`is`(visitorId))
    }

    @Test
    fun testVisitorId_dispatch() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val visitorId = "0123456789abcdef"
        tracker.setVisitorId(visitorId)
        Assert.assertEquals(visitorId, tracker.visitorId)

        tracker.track(TrackMe())
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals(visitorId, argumentCaptor.value[QueryParams.VISITOR_ID])

        tracker.track(TrackMe())
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        Assert.assertEquals(visitorId, argumentCaptor.value[QueryParams.VISITOR_ID])
    }

    @Test
    fun testUserID_init() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertNull(tracker.defaultTrackMe[QueryParams.USER_ID])
        Assert.assertNull(tracker.userId)
    }

    @Test
    fun testUserID_restore() {
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertNull(tracker.userId)
        tracker.setUserId("cake")
        MatcherAssert.assertThat(tracker.userId, Is.`is`("cake"))

        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        MatcherAssert.assertThat(tracker.userId, Is.`is`("cake"))
        MatcherAssert.assertThat(tracker.defaultTrackMe[QueryParams.USER_ID], Is.`is`("cake"))
    }

    @Test
    fun testUserID_invalid() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertNull(tracker.userId)

        tracker.setUserId("test")
        Assert.assertEquals(tracker.userId, "test")

        tracker.setUserId("")
        Assert.assertEquals(tracker.userId, "test")

        tracker.setUserId(null)
        Assert.assertNull(tracker.userId)

        val uuid = UUID.randomUUID().toString()
        tracker.setUserId(uuid)
        Assert.assertEquals(uuid, tracker.userId)
        Assert.assertEquals(uuid, tracker.userId)
    }

    @Test
    fun testUserID_dispatch() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val uuid = UUID.randomUUID().toString()
        tracker.setUserId(uuid)

        tracker.track(TrackMe())
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals(uuid, argumentCaptor.value[QueryParams.USER_ID])

        tracker.track(TrackMe())
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        Assert.assertEquals(uuid, argumentCaptor.value[QueryParams.USER_ID])
    }

    @Test
    fun testGetResolution() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val trackMe = TrackMe()
        tracker.track(trackMe)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals("480x800", argumentCaptor.value[QueryParams.SCREEN_RESOLUTION])
    }

    @Test
    fun testSetNewSession() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val trackMe = TrackMe()
        tracker.track(trackMe)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals("1", argumentCaptor.value[QueryParams.SESSION_START])

        tracker.startNewSession()
        TrackHelper.track().screen("").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        Assert.assertEquals("1", argumentCaptor.value[QueryParams.SESSION_START])
    }

    @Test
    fun testSetNewSessionRaceCondition() {
        for (retry in 0..4) {
            val trackMes = Collections.synchronizedList(ArrayList<TrackMe>())
            Mockito.doAnswer { invocation: InvocationOnMock ->
                trackMes.add(invocation.getArgument(0))
                null
            }.`when`<Dispatcher?>(dispatcher).submit(ArgumentMatchers.any<TrackMe>(TrackMe::class.java))
            val tracker = Tracker(matomo!!, mTrackerBuilder!!)
            tracker.setDispatchInterval(0)
            val count = 20
            for (i in 0 until count) {
                Thread {
                    TestHelper.sleep(10)
                    TrackHelper.track().screen("Test").with(tracker)
                }.start()
            }
            TestHelper.sleep(500)
            Assert.assertEquals(count.toLong(), trackMes.size.toLong())
            var found = 0
            for (trackMe in trackMes) {
                if (trackMe[QueryParams.SESSION_START] != null) found++
            }
            Assert.assertEquals(1, found.toLong())
        }
    }

    @Test
    fun testSetSessionTimeout() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.setSessionTimeout(10000)

        TrackHelper.track().screen("test1").with(tracker)
        MatcherAssert.assertThat(tracker.lastEventX!![QueryParams.SESSION_START], Matchers.notNullValue())

        TrackHelper.track().screen("test2").with(tracker)
        MatcherAssert.assertThat(tracker.lastEventX!![QueryParams.SESSION_START], Matchers.nullValue())

        tracker.setSessionTimeout(0)
        TestHelper.sleep(1)
        TrackHelper.track().screen("test3").with(tracker)
        MatcherAssert.assertThat(tracker.lastEventX!![QueryParams.SESSION_START], Matchers.notNullValue())

        tracker.setSessionTimeout(10000)
        Assert.assertEquals(tracker.sessionTimeout, 10000)
        TrackHelper.track().screen("test3").with(tracker)
        MatcherAssert.assertThat(tracker.lastEventX!![QueryParams.SESSION_START], Matchers.nullValue())
    }

    @Test
    fun testCheckSessionTimeout() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        tracker.setSessionTimeout(0)
        TrackHelper.track().screen("test").with(tracker)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals("1", argumentCaptor.value[QueryParams.SESSION_START])
        TestHelper.sleep(1)
        TrackHelper.track().screen("test").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        Assert.assertEquals("1", argumentCaptor.value[QueryParams.SESSION_START])
        tracker.setSessionTimeout(60000)
        TrackHelper.track().screen("test").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(3))?.submit(argumentCaptor.capture())
        Assert.assertNull(argumentCaptor.value[QueryParams.SESSION_START])
    }

    @Test
    fun testReset() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val callback: Tracker.Callback = object : Tracker.Callback {
            override fun onTrack(trackMe: TrackMe?): TrackMe? {
                return null
            }
        }
        tracker.addTrackingCallback(callback)
        tracker.defaultTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES] = "custom1"
        tracker.defaultTrackMe[QueryParams.CAMPAIGN_NAME] = "campaign_name"
        tracker.defaultTrackMe[QueryParams.CAMPAIGN_KEYWORD] = "campaign_keyword"

        TrackHelper.track().screen("test1").with(tracker)
        tracker.startNewSession()
        TrackHelper.track().screen("test2").with(tracker)

        val preResetDefaultVisitorId = tracker.defaultTrackMe[QueryParams.VISITOR_ID]
        val preResetFirstVisitTimestamp = tracker.defaultTrackMe[QueryParams.FIRST_VISIT_TIMESTAMP]
        val preResetTotalNumberOfVisits = tracker.defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS]
        val preResetPreviousVisitTimestamp = tracker.defaultTrackMe[QueryParams.PREVIOUS_VISIT_TIMESTAMP]

        tracker.reset()

        val prefs = tracker.preferences

        Assert.assertNotEquals(preResetDefaultVisitorId, tracker.visitorId)
        Assert.assertNotEquals(preResetDefaultVisitorId, tracker.defaultTrackMe[QueryParams.VISITOR_ID])
        Assert.assertNotEquals(preResetDefaultVisitorId, prefs!!.getString(Tracker.PREF_KEY_TRACKER_VISITORID, ""))

        Assert.assertNotEquals(preResetFirstVisitTimestamp, tracker.defaultTrackMe[QueryParams.FIRST_VISIT_TIMESTAMP])
        Assert.assertNotEquals(preResetFirstVisitTimestamp.toLong(), prefs.getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1))

        Assert.assertNotEquals(preResetPreviousVisitTimestamp, tracker.defaultTrackMe[QueryParams.PREVIOUS_VISIT_TIMESTAMP])
        Assert.assertNotEquals(preResetPreviousVisitTimestamp.toLong(), prefs.getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1))

        Assert.assertNotEquals(preResetTotalNumberOfVisits, tracker.defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS])
        Assert.assertNotEquals(preResetTotalNumberOfVisits, prefs.getString(Tracker.PREF_KEY_TRACKER_VISITCOUNT, ""))

        Assert.assertNull(tracker.defaultTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES])
        Assert.assertNull(tracker.defaultTrackMe[QueryParams.CAMPAIGN_NAME])
        Assert.assertNull(tracker.defaultTrackMe[QueryParams.CAMPAIGN_KEYWORD])
    }

    @Test
    fun testTrackerEquals() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val builder2 = Mockito.mock(TrackerBuilder::class.java)
        Mockito.`when`(builder2.apiUrl).thenReturn("http://localhost")
        Mockito.`when`(builder2.siteId).thenReturn(100)
        Mockito.`when`(builder2.trackerName).thenReturn("Default Tracker")
        val tracker2 = Tracker(matomo!!, builder2)

        val builder3 = Mockito.mock(TrackerBuilder::class.java)
        Mockito.`when`(builder3.apiUrl).thenReturn("http://example.com")
        Mockito.`when`(builder3.siteId).thenReturn(11)
        Mockito.`when`(builder3.trackerName).thenReturn("Default Tracker")
        val tracker3 = Tracker(matomo!!, builder3)

        Assert.assertNotNull(tracker)
        Assert.assertNotEquals(tracker, tracker2)
        Assert.assertEquals(tracker, tracker3)
    }

    @Test
    fun testTrackerHashCode() {
        Assert.assertEquals(Tracker(matomo!!, mTrackerBuilder!!).hashCode().toLong(), Tracker(matomo!!, mTrackerBuilder!!).hashCode().toLong())
    }

    @Test
    fun testUrlPathCorrection() {
        Mockito.`when`(mTrackerBuilder!!.applicationBaseUrl).thenReturn("https://package/")
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val paths = arrayOf(null, "", "/")
        for (path in paths) {
            val trackMe = TrackMe()
            trackMe[QueryParams.URL_PATH] = path
            tracker.track(trackMe)
            Assert.assertEquals("https://package/", trackMe[QueryParams.URL_PATH])
        }
    }

    @Test
    fun testSetUserAgent() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        var trackMe = TrackMe()
        tracker.track(trackMe)
        Assert.assertEquals("aUserAgent", trackMe[QueryParams.USER_AGENT])

        // Custom developer specified useragent
        trackMe = TrackMe()
        val customUserAgent = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0"
        trackMe[QueryParams.USER_AGENT] = customUserAgent
        tracker.track(trackMe)
        Assert.assertEquals(customUserAgent, trackMe[QueryParams.USER_AGENT])

        // Modifying default TrackMe, no USER_AGENT
        trackMe = TrackMe()
        tracker.defaultTrackMe[QueryParams.USER_AGENT] = null
        tracker.track(trackMe)
        Assert.assertNull(trackMe[QueryParams.USER_AGENT])
    }

    @Test
    fun testFirstVisitTimeStamp() {
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals(-1, tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1))

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        val trackMe1 = argumentCaptor.value
        TestHelper.sleep(10)
        // make sure we are tracking in seconds
        Assert.assertTrue(abs(((System.currentTimeMillis() / 1000) - trackMe1[QueryParams.FIRST_VISIT_TIMESTAMP].toLong()).toDouble()) < 2)

        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        val trackMe2 = argumentCaptor.value
        Assert.assertEquals(trackMe1[QueryParams.FIRST_VISIT_TIMESTAMP].toLong(), trackMe2[QueryParams.FIRST_VISIT_TIMESTAMP].toLong())
        Assert.assertEquals(
            tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, -1),
            trackMe1[QueryParams.FIRST_VISIT_TIMESTAMP].toLong()
        )
    }

    @Test
    fun testTotalVisitCount() {
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals(-1, tracker.preferences!!.getInt(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1).toLong())
        Assert.assertNull(tracker.defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS])

        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        Assert.assertEquals(1, argumentCaptor.value[QueryParams.TOTAL_NUMBER_OF_VISITS].toInt())

        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Assert.assertEquals(1, tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1))
        Assert.assertNull(tracker.defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS])
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        Assert.assertEquals(2, argumentCaptor.value[QueryParams.TOTAL_NUMBER_OF_VISITS].toInt())
        Assert.assertEquals(2, tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, -1))
    }

    @Test
    @Throws(Exception::class)
    fun testVisitCountMultipleThreads() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val threadCount = 1000
        val countDownLatch = CountDownLatch(threadCount)
        for (i in 0 until threadCount) {
            Thread {
                TestHelper.sleep((Random().nextInt(20 - 0) + 0).toLong())
                TrackHelper.track().event("TestCategory", "TestAction").with(Tracker(matomo!!, mTrackerBuilder!!))
                countDownLatch.countDown()
            }.start()
        }
        countDownLatch.await()
        Assert.assertEquals(threadCount.toLong(), trackerPreferences.getLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, 0))
    }

    @Test
    @Throws(Exception::class)
    fun testSessionStartRaceCondition() {
        val trackMes = Collections.synchronizedList(ArrayList<TrackMe>())
        Mockito.doAnswer { invocation: InvocationOnMock ->
            trackMes.add(invocation.getArgument(0))
            null
        }.`when`<Dispatcher?>(dispatcher).submit(ArgumentMatchers.any<TrackMe>(TrackMe::class.java))
        Mockito.`when`(dispatcher!!.connectionTimeOut).thenReturn(1000)
        for (i in 0..999) {
            trackMes.clear()
            val tracker = Tracker(matomo!!, mTrackerBuilder!!)
            val countDownLatch = CountDownLatch(10)
            for (j in 0..9) {
                Thread {
                    try {
                        TestHelper.sleep((Random().nextInt(4 - 0) + 0).toLong())
                        val trackMe = TrackMe()
                            .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                            .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                            .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                            .set(QueryParams.EVENT_VALUE, 1)
                        tracker.track(trackMe)
                        countDownLatch.countDown()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Assert.fail()
                    }
                }.start()
            }
            countDownLatch.await()
            for (out in trackMes) {
                if (trackMes.indexOf(out) == 0) {
                    Assert.assertNotNull(i.toString() + "#" + out.toMap().size, out[QueryParams.LANGUAGE])
                    Assert.assertNotNull(out[QueryParams.FIRST_VISIT_TIMESTAMP])
                    Assert.assertNotNull(out[QueryParams.SESSION_START])
                } else {
                    Assert.assertNull(out[QueryParams.FIRST_VISIT_TIMESTAMP])
                    Assert.assertNull(out[QueryParams.SESSION_START])
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFirstVisitMultipleThreads() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val threadCount = 100
        val countDownLatch = CountDownLatch(threadCount)
        val firstVisitTimes = Collections.synchronizedList(ArrayList<Long>())
        for (i in 0 until threadCount) {
            Thread {
                TestHelper.sleep((Random().nextInt(20 - 0) + 0).toLong())
                TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
                val firstVisit = tracker.defaultTrackMe[QueryParams.FIRST_VISIT_TIMESTAMP].toLong()
                firstVisitTimes.add(firstVisit)
                countDownLatch.countDown()
            }.start()
        }
        countDownLatch.await()
        for (firstVisit in firstVisitTimes) Assert.assertEquals(firstVisitTimes[0], firstVisit)
    }

    @Test
    fun testPreviousVisits() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val previousVisitTimes: MutableList<Long> = ArrayList()
        for (i in 0..4) {
            TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
            val previousVisit = tracker.defaultTrackMe[QueryParams.PREVIOUS_VISIT_TIMESTAMP]
            if (previousVisit != null) previousVisitTimes.add(previousVisit.toLong())
            TestHelper.sleep(1010)
        }
        Assert.assertFalse(previousVisitTimes.contains(0L))
        var lastTime = 0L
        for (time in previousVisitTimes) {
            Assert.assertTrue(lastTime < time)
            lastTime = time
        }
    }

    @Test
    fun testPreviousVisit() {
        var tracker = Tracker(matomo!!, mTrackerBuilder!!)
        // No timestamp yet
        Assert.assertEquals(-1, tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1))
        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher)?.submit(argumentCaptor.capture())
        val _startTime = System.currentTimeMillis() / 1000
        // There was no previous visit
        Assert.assertNull(argumentCaptor.value[QueryParams.PREVIOUS_VISIT_TIMESTAMP])
        TestHelper.sleep(1000)

        // After the first visit we now have a timestamp for the previous visit
        var previousVisit = tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1)
        Assert.assertTrue(previousVisit - _startTime < 2000)
        Assert.assertNotEquals(-1, previousVisit)
        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(2))?.submit(argumentCaptor.capture())
        // Transmitted timestamp is the one from the first visit visit
        Assert.assertEquals(previousVisit, argumentCaptor.value[QueryParams.PREVIOUS_VISIT_TIMESTAMP].toLong())

        TestHelper.sleep(1000)
        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(3))?.submit(argumentCaptor.capture())
        // Now the timestamp changed as this is the 3rd visit.
        Assert.assertNotEquals(previousVisit, argumentCaptor.value[QueryParams.PREVIOUS_VISIT_TIMESTAMP].toLong())
        TestHelper.sleep(1000)

        previousVisit = tracker.preferences!!.getLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, -1)
        tracker = Tracker(matomo!!, mTrackerBuilder!!)
        TrackHelper.track().event("TestCategory", "TestAction").with(tracker)
        Mockito.verify(dispatcher, Mockito.times(4))?.submit(argumentCaptor.capture())
        // Just make sure the timestamp in the 4th visit is from the 3rd visit
        Assert.assertEquals(previousVisit, argumentCaptor.value[QueryParams.PREVIOUS_VISIT_TIMESTAMP].toLong())

        // Test setting a custom timestamp
        val custom = TrackMe()
        custom[QueryParams.PREVIOUS_VISIT_TIMESTAMP] = 1000L
        tracker.track(custom)
        Mockito.verify(dispatcher, Mockito.times(5))?.submit(argumentCaptor.capture())
        Assert.assertEquals(1000L, argumentCaptor.value[QueryParams.PREVIOUS_VISIT_TIMESTAMP].toLong())
    }

    @Test
    fun testTrackingCallback() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val callback = Mockito.mock(Tracker.Callback::class.java)

        val pre = TrackMe()
        tracker.track(pre)
        Mockito.verify(dispatcher)?.submit(pre)
        Mockito.verify(callback, Mockito.never()).onTrack(argumentCaptor.capture())

        Mockito.reset(dispatcher, callback)
        tracker.addTrackingCallback(callback)
        tracker.track(TrackMe())
        Mockito.verify(callback).onTrack(argumentCaptor.capture())
        Mockito.verify(dispatcher, Mockito.never())?.submit(ArgumentMatchers.any())

        Mockito.reset(dispatcher, callback)
        val orig = TrackMe()
        val replaced = TrackMe().set("some", "thing")
        Mockito.`when`(callback.onTrack(orig)).thenReturn(replaced)
        tracker.track(orig)
        Mockito.verify(callback).onTrack(orig)
        Mockito.verify(dispatcher)?.submit(replaced)

        Mockito.reset(dispatcher, callback)
        val post = TrackMe()
        tracker.removeTrackingCallback(callback)
        tracker.track(post)
        Mockito.verify(callback, Mockito.never()).onTrack(ArgumentMatchers.any())
        Mockito.verify(dispatcher)?.submit(post)
    }

    @Test
    fun testTrackingCallbacks() {
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        val callback1 = Mockito.mock(Tracker.Callback::class.java)
        val callback2 = Mockito.mock(Tracker.Callback::class.java)

        val orig = TrackMe()
        val replaced = TrackMe()
        Mockito.`when`(callback1.onTrack(orig)).thenReturn(replaced)
        Mockito.`when`(callback2.onTrack(replaced)).thenReturn(replaced)

        tracker.addTrackingCallback(callback1)
        tracker.addTrackingCallback(callback1)
        tracker.addTrackingCallback(callback2)
        tracker.track(orig)
        Mockito.verify(callback1).onTrack(orig)
        Mockito.verify(callback2).onTrack(replaced)
        Mockito.verify(dispatcher)?.submit(replaced)

        tracker.removeTrackingCallback(callback1)
        tracker.track(orig)

        Mockito.verify(callback2).onTrack(orig)
    }

    @Test
    fun testCustomDispatcherFactory() {
        val dispatcherLocal = Mockito.mock(Dispatcher::class.java)
        val factory = Mockito.mock(DispatcherFactory::class.java)
        Mockito.`when`(factory.build(ArgumentMatchers.any(Tracker::class.java))).thenReturn(dispatcherLocal)
        Mockito.`when`(matomo!!.dispatcherFactory).thenReturn(factory)
        val tracker = Tracker(matomo!!, mTrackerBuilder!!)
        Mockito.verify(factory).build(tracker)
    }

    companion object {
        private fun validateDefaultQuery(params: TrackMe) {
            Assert.assertEquals(params[QueryParams.SITE_ID], "11")
            Assert.assertEquals(params[QueryParams.RECORD], "1")
            Assert.assertEquals(params[QueryParams.SEND_IMAGE], "0")
            Assert.assertEquals(params[QueryParams.VISITOR_ID].length.toLong(), 16)
            Assert.assertTrue(params[QueryParams.URL_PATH].startsWith("http://"))
            Assert.assertTrue(params[QueryParams.RANDOM_NUMBER].toInt() > 0)
        }
    }
}
