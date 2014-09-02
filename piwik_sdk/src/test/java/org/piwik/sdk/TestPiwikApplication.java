package org.piwik.sdk;


import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;

public class TestPiwikApplication extends PiwikApplication implements TestLifecycleApplication {
    @Override
    public void beforeTest(Method method) {
    }

    @Override
    public void prepareTest(Object test) {
    }

    @Override
    public void afterTest(Method method) {
    }
}
