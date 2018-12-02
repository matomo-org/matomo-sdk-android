package org.matomo.sdk.tools;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class BuildInfoTest extends BaseTest {

    private BuildInfo mBuildInfo;

    @Before
    public void setup() throws Exception {
        super.setup();
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
