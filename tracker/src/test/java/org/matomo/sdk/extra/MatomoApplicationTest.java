package org.matomo.sdk.extra;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.Matomo;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.Tracker;
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
public class MatomoApplicationTest extends DefaultTestCase {
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void testAutoBindActivities() {
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
    public void testApplicationGetTracker() {
        MatomoApplication matomoApplication = (MatomoApplication) Robolectric.application;
        assertEquals(matomoApplication.getTracker(), matomoApplication.getTracker());
    }

    @Test
    public void testApplication() {
        MatomoApplication matomoApplication = (MatomoApplication) Robolectric.application;
        Assert.assertEquals(matomoApplication.getMatomo(), Matomo.getInstance(matomoApplication));
    }
}
