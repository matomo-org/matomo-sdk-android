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
    var mPropertySource: PropertySource? = null

    @Mock
    var mBuildInfo: BuildInfo? = null

    @Mock
    var mContext: Context? = null
    private var mDeviceHelper: DeviceHelper? = null

    @Before
    @Throws(Exception::class)
    override fun setup() {
        super.setup()
        Mockito.`when`(mBuildInfo!!.buildId).thenReturn("ABCDEF")
        Mockito.`when`(mBuildInfo!!.model).thenReturn("UnitTest")
        Mockito.`when`(mBuildInfo!!.release).thenReturn("8.0.0")
        Mockito.`when`(mPropertySource!!.jvmVersion).thenReturn("2.2.0")
        mDeviceHelper = DeviceHelper(mContext, mPropertySource, mBuildInfo)
    }

    @Test
    fun testGetHttpAgent_normal() {
        Mockito.`when`(mPropertySource!!.httpAgent).thenReturn("testagent")
        Assert.assertEquals("testagent", mDeviceHelper!!.userAgent)
    }

    @Test
    fun testGetHttpAgent_badAgent() {
        Mockito.`when`(mPropertySource!!.httpAgent).thenReturn("Apache-HttpClient/UNAVAILABLE (java 1.4)")
        Assert.assertEquals("Dalvik/2.2.0 (Linux; U; Android 8.0.0; UnitTest Build/ABCDEF)", mDeviceHelper!!.userAgent)
        Mockito.verify(mBuildInfo)?.buildId
        Mockito.verify(mBuildInfo)?.model
        Mockito.verify(mBuildInfo)?.release
        Mockito.verify(mPropertySource)?.jvmVersion

        Mockito.`when`(mPropertySource!!.jvmVersion).thenReturn(null)
        Assert.assertEquals("Dalvik/0.0.0 (Linux; U; Android 8.0.0; UnitTest Build/ABCDEF)", mDeviceHelper!!.userAgent)
    }
}
