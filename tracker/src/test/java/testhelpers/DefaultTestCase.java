package testhelpers;

import org.junit.runner.RunWith;
import org.matomo.sdk.Matomo;
import org.matomo.sdk.Tracker;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;

@Config(sdk = 19, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public abstract class DefaultTestCase extends BaseTest {
    public Tracker createTracker() {
        MatomoTestApplication app = ApplicationProvider.getApplicationContext();
        final Tracker tracker = app.onCreateTrackerConfig().build(Matomo.getInstance(ApplicationProvider.getApplicationContext()));
        tracker.getPreferences().edit().clear().apply();
        return tracker;
    }

    public Matomo getMatomo() {
        return Matomo.getInstance(ApplicationProvider.getApplicationContext());
    }

}
