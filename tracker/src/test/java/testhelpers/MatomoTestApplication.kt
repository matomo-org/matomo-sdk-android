package testhelpers

import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.MatomoApplication
import org.robolectric.TestLifecycleApplication
import org.robolectric.shadows.ShadowLog
import java.lang.reflect.Method


class MatomoTestApplication : MatomoApplication(), TestLifecycleApplication {
    override fun onCreate() {
        ShadowLog.stream = System.out
        super.onCreate()
    }

    override fun beforeTest(method: Method) {
    }

    override fun prepareTest(test: Any) {
    }

    override fun afterTest(method: Method) {
    }

    override fun getPackageName(): String {
        return "11"
    }


    override fun onCreateTrackerConfig(): TrackerBuilder {
        return TrackerBuilder.createDefault("http://example.com", 1)
    }
}
