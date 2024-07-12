package org.matomo.sdk.tools

import android.content.Context
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import testhelpers.BaseTest


@RunWith(MockitoJUnitRunner::class)
class DeviceHelperTest : BaseTest() {
    @Mock
    var propertySource: PropertySource? = null

    @Mock
    var buildInfo: BuildInfo? = null

    @Mock
    var context: Context? = null
    private var deviceHelper: DeviceHelper? = null

    @Before
    @Throws(Exception::class)
    override fun setup() {
        super.setup()
        Mockito.`when`(buildInfo!!.buildId).thenReturn("ABCDEF")
        Mockito.`when`(buildInfo!!.model).thenReturn("UnitTest")
        Mockito.`when`(buildInfo!!.release).thenReturn("8.0.0")
        Mockito.`when`(propertySource!!.jvmVersion).thenReturn("2.2.0")
        deviceHelper = DeviceHelper(context, propertySource, buildInfo)
    }

    @Test
    fun testGetHttpAgent_normal() {
        Mockito.`when`(propertySource!!.httpAgent).thenReturn("testagent")
        Assert.assertEquals("testagent", deviceHelper!!.userAgent)
    }

    @Test
    fun testGetHttpAgent_badAgent() {
        Mockito.`when`(propertySource!!.httpAgent).thenReturn("Apache-HttpClient/UNAVAILABLE (java 1.4)")
        Assert.assertEquals("Dalvik/2.2.0 (Linux; U; Android 8.0.0; UnitTest Build/ABCDEF)", deviceHelper!!.userAgent)
        Mockito.verify(buildInfo)?.buildId
        Mockito.verify(buildInfo)?.model
        Mockito.verify(buildInfo)?.release
        Mockito.verify(propertySource)?.jvmVersion

        Mockito.`when`(propertySource!!.jvmVersion).thenReturn(null)
        Assert.assertEquals("Dalvik/0.0.0 (Linux; U; Android 8.0.0; UnitTest Build/ABCDEF)", deviceHelper!!.userAgent)
    }
}
