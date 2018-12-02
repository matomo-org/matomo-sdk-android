package testhelpers;

import org.junit.runner.RunWith;
import org.matomo.sdk.Matomo;
import org.matomo.sdk.Tracker;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public abstract class DefaultTestCase extends BaseTest {
    public Tracker createTracker() {
        MatomoTestApplication app = (MatomoTestApplication) Robolectric.application;
        final Tracker tracker = app.onCreateTrackerConfig().build(Matomo.getInstance(Robolectric.application));
        tracker.getPreferences().edit().clear().apply();
        return tracker;
    }

    public Matomo getMatomo() {
        return Matomo.getInstance(Robolectric.application);
    }

}
