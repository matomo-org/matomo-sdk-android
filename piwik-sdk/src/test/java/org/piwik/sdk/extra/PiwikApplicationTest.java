package org.piwik.sdk.extra;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.Tracker;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;

import testhelpers.DefaultTestCase;
import testhelpers.FullEnvTestRunner;
import testhelpers.QueryHashMap;
import testhelpers.TestActivity;

import static org.junit.Assert.assertEquals;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class PiwikApplicationTest extends DefaultTestCase {
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void testPiwikAutoBindActivities() {
        Application app = Robolectric.application;
        Tracker tracker = createTracker();
        tracker.setDryRunTarget(Collections.synchronizedList(new ArrayList<>()));
        //auto attach tracking screen view
        TrackHelper.track().screens(app).with(tracker);

        // emulate default trackScreenView
        Robolectric.buildActivity(TestActivity.class).create().start().resume().visible().get();

        assertEquals(TestActivity.getTestTitle(), new QueryHashMap(tracker.getLastEventX()).get(QueryParams.ACTION_NAME));
    }

    @Test
    public void testPiwikApplicationGetTracker() {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getTracker(), piwikApplication.getTracker());
    }

    @Test
    public void testPiwikApplicationgetPiwik() {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        Assert.assertEquals(piwikApplication.getPiwik(), Piwik.getInstance(piwikApplication));
    }
}
