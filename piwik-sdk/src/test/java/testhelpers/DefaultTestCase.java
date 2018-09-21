package testhelpers;

import org.junit.runner.RunWith;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public abstract class DefaultTestCase extends BaseTest {
    public Tracker createTracker() {
        PiwikTestApplication app = (PiwikTestApplication) Robolectric.application;
        final Tracker tracker = app.onCreateTrackerConfig().build(Piwik.getInstance(Robolectric.application));
        tracker.getPreferences().edit().clear().apply();
        return tracker;
    }

    public Piwik getPiwik() {
        return Piwik.getInstance(Robolectric.application);
    }

}
