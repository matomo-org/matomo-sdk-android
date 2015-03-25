package org.piwik.sdk;


import android.content.SharedPreferences;
import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestPiwikApplication extends PiwikApplication implements TestLifecycleApplication {

    private final List<SharedPreferences> mActivePreferences = new ArrayList<>();

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
        return "org.piwik.sdk.test";
    }

    protected void clearSharedPreferences() {
        for(SharedPreferences pref : mActivePreferences)
            pref.edit().clear().commit();
    }

    @Override
    public SharedPreferences getSharedPreferences(String namespace, int modePrivate) {
        SharedPreferences pref = super.getSharedPreferences(namespace,modePrivate);
        if(!mActivePreferences.contains(pref))
            mActivePreferences.add(pref);
        return pref;
    }
}
