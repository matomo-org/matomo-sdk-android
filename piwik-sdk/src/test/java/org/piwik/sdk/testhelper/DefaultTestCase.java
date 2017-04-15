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
        final Tracker tracker = Piwik.getInstance(Robolectric.application).newTracker(app.onCreateTrackerConfig());
        tracker.getPreferences().edit().clear().apply();
        return tracker;
    }

    public Piwik getPiwik() {
        return Piwik.getInstance(Robolectric.application);
    }

    @Before
    public void setup() {

    }
}
