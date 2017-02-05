package org.piwik.sdk.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConnectivityTest {
    @Mock Context mContext;
    @Mock ConnectivityManager mConnectivityManager;
    @Mock NetworkInfo mNetworkInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mConnectivityManager);

    }

    @Test
    public void testGetType_none() {
        Connectivity connectivity = new Connectivity(mContext);
        assertEquals(Connectivity.Type.NONE, connectivity.getType());
        verify(mConnectivityManager).getActiveNetworkInfo();
    }

    @Test
    public void testGetType_wifi() {
        Connectivity connectivity = new Connectivity(mContext);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(mNetworkInfo);
        when(mNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        assertEquals(Connectivity.Type.WIFI, connectivity.getType());
        verify(mConnectivityManager).getActiveNetworkInfo();
    }

    @Test
    public void testGetType_else() {
        Connectivity connectivity = new Connectivity(mContext);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(mNetworkInfo);
        when(mNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIMAX);
        assertEquals(Connectivity.Type.MOBILE, connectivity.getType());
        verify(mConnectivityManager).getActiveNetworkInfo();
    }
}
