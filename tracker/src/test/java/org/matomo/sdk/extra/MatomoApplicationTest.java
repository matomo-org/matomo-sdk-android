package org.matomo.sdk.extra;

import android.app.Application;

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

import androidx.test.core.app.ApplicationProvider;
import testhelpers.DefaultTestCase;
import testhelpers.FullEnvTestRunner;
import testhelpers.MatomoTestApplication;
import testhelpers.QueryHashMap;
import testhelpers.TestActivity;

import static org.junit.Assert.assertEquals;

@Config(sdk = 28, manifest = Config.NONE, application = MatomoTestApplication.class)
@RunWith(FullEnvTestRunner.class)
public class MatomoApplicationTest extends DefaultTestCase {

    @Test
    public void testAutoBindActivities() {
        Application app = ApplicationProvider.getApplicationContext();
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
        MatomoApplication matomoApplication = ApplicationProvider.getApplicationContext();
        assertEquals(matomoApplication.getTracker(), matomoApplication.getTracker());
    }

    @Test
    public void testApplication() {
        MatomoApplication matomoApplication = ApplicationProvider.getApplicationContext();
        Assert.assertEquals(matomoApplication.getMatomo(), Matomo.getInstance(matomoApplication));
    }
}
