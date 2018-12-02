package testhelpers;


import org.matomo.sdk.TrackerBuilder;
import org.matomo.sdk.extra.MatomoApplication;
import org.robolectric.TestLifecycleApplication;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Method;

public class MatomoTestApplication extends MatomoApplication implements TestLifecycleApplication {

    @Override
    public void onCreate() {
        ShadowLog.stream = System.out;
        super.onCreate();
    }

    @Override
    public void beforeTest(Method method) {

    }

    @Override
    public void prepareTest(Object test) {
    }

    @Override
    public void afterTest(Method method) {

    }

    @Override
    public String getPackageName() {
        return "11";
    }


    @Override
    public TrackerBuilder onCreateTrackerConfig() {
        return TrackerBuilder.createDefault("http://example.com", 1);
    }
}
