package testhelpers

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.matomo.sdk.Matomo
import org.matomo.sdk.Matomo.Companion.getInstance
import org.matomo.sdk.Tracker
import org.robolectric.annotation.Config

@Config(sdk = [19], manifest = Config.NONE)
@RunWith(FullEnvTestRunner::class)
abstract class DefaultTestCase : BaseTest() {
    fun createTracker(): Tracker {
        val app = ApplicationProvider.getApplicationContext<MatomoTestApplication>()
        val tracker = app.onCreateTrackerConfig().build(getInstance(ApplicationProvider.getApplicationContext()))
        tracker.preferences.edit().clear().apply()
        return tracker
    }

    val matomo: Matomo?
        get() = getInstance(ApplicationProvider.getApplicationContext())
}
