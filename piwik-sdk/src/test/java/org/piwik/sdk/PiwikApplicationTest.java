package org.piwik.sdk;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.QueryHashMap;
import org.piwik.sdk.testhelper.TestActivity;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class PiwikApplicationTest extends DefaultTestCase {
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void testPiwikAutoBindActivities() throws Exception {
        Application app = Robolectric.application;
        Piwik piwik = Piwik.getInstance(app);
        piwik.setOptOut(true);
        Tracker tracker = createTracker();
        //auto attach tracking screen view
        TrackHelper.track().screens(app).with(tracker);

        // emulate default trackScreenView
        Robolectric.buildActivity(TestActivity.class).create().start().resume().visible().get();

        assertEquals(TestActivity.getTestTitle(), new QueryHashMap(tracker.getLastEventX()).get(QueryParams.ACTION_NAME));
    }

    @Test
    public void testPiwikApplicationGetTracker() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getTracker(), piwikApplication.getTracker());
    }

    @Test
    public void testPiwikApplicationgetPiwik() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getPiwik(), Piwik.getInstance(piwikApplication));
    }
}
