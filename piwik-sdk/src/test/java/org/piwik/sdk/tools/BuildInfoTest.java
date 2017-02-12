package org.piwik.sdk.tools;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class BuildInfoTest {

    private BuildInfo mBuildInfo;

    @Before
    public void setup() {
        mBuildInfo = new BuildInfo();
    }

    @Test
    public void testGetRelease() {
        assertEquals(Build.VERSION.RELEASE, mBuildInfo.getRelease());
    }

    @Test
    public void testGetModel() {
        assertEquals(Build.MODEL, mBuildInfo.getModel());
    }

    @Test
    public void testGetBuildId() {
        assertEquals(Build.ID, mBuildInfo.getBuildId());
    }
}
