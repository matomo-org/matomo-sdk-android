package org.piwik.sdk.tools;


import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceHelperTest {
    @Mock PropertySource mPropertySource;
    @Mock BuildInfo mBuildInfo;
    @Mock Context mContext;
    private DeviceHelper mDeviceHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mBuildInfo.getBuildId()).thenReturn("ABCDEF");
        when(mBuildInfo.getModel()).thenReturn("UnitTest");
        when(mBuildInfo.getRelease()).thenReturn("8.0.0");
        when(mPropertySource.getJVMVersion()).thenReturn("2.2.0");
        mDeviceHelper = new DeviceHelper(mContext, mPropertySource, mBuildInfo);
    }

    @Test
    public void testGetHttpAgent_normal() {
        when(mPropertySource.getHttpAgent()).thenReturn("testagent");
        assertEquals("testagent", mDeviceHelper.getUserAgent());
    }

    @Test
    public void testGetHttpAgent_badAgent() {
        when(mPropertySource.getHttpAgent()).thenReturn("Apache-HttpClient/UNAVAILABLE (java 1.4)");
        assertEquals("Dalvik/2.2.0 (Linux; U; Android 8.0.0; UnitTest Build/ABCDEF)", mDeviceHelper.getUserAgent());
        verify(mBuildInfo).getBuildId();
        verify(mBuildInfo).getModel();
        verify(mBuildInfo).getRelease();
        verify(mPropertySource).getJVMVersion();

        when(mPropertySource.getJVMVersion()).thenReturn(null);
        assertEquals("Dalvik/0.0.0 (Linux; U; Android 8.0.0; UnitTest Build/ABCDEF)", mDeviceHelper.getUserAgent());
    }
}
