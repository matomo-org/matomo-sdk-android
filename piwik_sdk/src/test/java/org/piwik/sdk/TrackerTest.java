package org.piwik.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TrackerTest {

    Piwik piwik = Piwik.getInstance(new TestPiwikApplication());

    @Test
    public void testEmptyDispatch() throws Exception {
        assertFalse(piwik.newTracker("http://example.com", 1).dispatch());
    }

    @Test
    public void testSetDispatchInterval() throws Exception {

    }

    @Test
    public void testGetDispatchInterval() throws Exception {

    }

    @Test
    public void testGetDispatchIntervalMillis() throws Exception {

    }

    @Test
    public void testDispatchingCompleted() throws Exception {

    }

    @Test
    public void testDispatchingStarted() throws Exception {

    }

    @Test
    public void testIsDispatching() throws Exception {

    }

    @Test
    public void testSet() throws Exception {

    }

    @Test
    public void testSet1() throws Exception {

    }

    @Test
    public void testSetUserId() throws Exception {

    }

    @Test
    public void testSetUserId1() throws Exception {

    }

    @Test
    public void testGetResolution() throws Exception {

    }

    @Test
    public void testSetUserCustomVariable() throws Exception {

    }

    @Test
    public void testSetScreenCustomVariable() throws Exception {

    }

    @Test
    public void testSetNewSession() throws Exception {

    }

    @Test
    public void testSetSessionTimeout() throws Exception {

    }

    @Test
    public void testTrackScreenView() throws Exception {

    }

    @Test
    public void testTrackEvent() throws Exception {

    }

    @Test
    public void testTrackGoal() throws Exception {

    }

    @Test
    public void testTrackGoal1() throws Exception {

    }

    @Test
    public void testTrackAppDownload() throws Exception {

    }

    @Test
    public void testTrackException() throws Exception {

    }

    @Test
    public void testGetParamUlr() throws Exception {

    }

    @Test
    public void testSetAPIUrl() throws Exception {

    }

    @Test
    public void testMd5() throws Exception {

    }
}