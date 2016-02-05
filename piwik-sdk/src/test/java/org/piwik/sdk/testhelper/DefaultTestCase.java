package org.piwik.sdk.testhelper;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public abstract class DefaultTestCase {
    public Tracker createTracker() throws MalformedURLException {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        return Piwik.getInstance(Robolectric.application).newTracker(app.getTrackerUrl(), app.getSiteId());
    }

    public Piwik getPiwik() {
        return Piwik.getInstance(Robolectric.application);
    }

    @Before
    public void setup() {
        Piwik.getInstance(Robolectric.application).setDryRun(true);
        Piwik.getInstance(Robolectric.application).setOptOut(true);
        Piwik.getInstance(Robolectric.application).getSharedPreferences().edit().clear().apply();
    }
}
