package org.matomo.sdk.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import testhelpers.BaseTest

@RunWith(MockitoJUnitRunner::class)
class ConnectivityTest : BaseTest() {
    @Mock
    var context: Context? = null

    @Mock
    var connectivityManager: ConnectivityManager? = null

    @Mock
    var networkInfo: NetworkInfo? = null

    @Before
    override fun setup() {
        Mockito.`when`(context!!.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
    }

    @Test
    fun testGetType_none() {
        val connectivity = Connectivity(context)
        Assert.assertEquals(Connectivity.Type.NONE, connectivity.type)
        Mockito.verify(connectivityManager)?.activeNetworkInfo
    }

    @Test
    fun testGetType_wifi() {
        val connectivity = Connectivity(context)
        Mockito.`when`(connectivityManager!!.activeNetworkInfo).thenReturn(networkInfo)
        Mockito.`when`(networkInfo!!.type).thenReturn(ConnectivityManager.TYPE_WIFI)
        Assert.assertEquals(Connectivity.Type.WIFI, connectivity.type)
        Mockito.verify(connectivityManager)?.activeNetworkInfo
    }

    @Test
    fun testGetType_else() {
        val connectivity = Connectivity(context)
        Mockito.`when`(connectivityManager!!.activeNetworkInfo).thenReturn(networkInfo)
        Mockito.`when`(networkInfo!!.type).thenReturn(ConnectivityManager.TYPE_WIMAX)
        Assert.assertEquals(Connectivity.Type.MOBILE, connectivity.type)
        Mockito.verify(connectivityManager)?.activeNetworkInfo
    }
}
