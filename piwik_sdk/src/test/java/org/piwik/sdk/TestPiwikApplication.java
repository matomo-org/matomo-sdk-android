package org.piwik.sdk;


import android.content.SharedPreferences;
import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestPiwikApplication extends PiwikApplication implements TestLifecycleApplication {

    HashMap<String, SharedPreferences> prefsHolder;

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
        prefsHolder = new HashMap<String, SharedPreferences>();
    }

    @Override
    public SharedPreferences getSharedPreferences(String namespace, int modePrivate) {
        SharedPreferences pref = prefsHolder.get(namespace);

        if (pref == null) {
            pref = new SharedPreferences() {
                HashMap<String, Boolean> booleanHolder = new HashMap<String, Boolean>();

                @Override
                public Map<String, ?> getAll() {
                    return null;
                }

                @Override
                public String getString(String s, String s2) {
                    return null;
                }

                @Override
                public Set<String> getStringSet(String s, Set<String> strings) {
                    return null;
                }

                @Override
                public int getInt(String s, int i) {
                    return 0;
                }

                @Override
                public long getLong(String s, long l) {
                    return 0;
                }

                @Override
                public float getFloat(String s, float v) {
                    return 0;
                }

                @Override
                public boolean getBoolean(String s, boolean defaultValue) {
                    return booleanHolder.containsKey(s) || defaultValue;
                }

                @Override
                public boolean contains(String s) {
                    return false;
                }

                @Override
                public Editor edit() {
                    return new Editor() {
                        @Override
                        public Editor putString(String s, String s2) {
                            return null;
                        }

                        @Override
                        public Editor putStringSet(String s, Set<String> strings) {
                            return null;
                        }

                        @Override
                        public Editor putInt(String s, int i) {
                            return null;
                        }

                        @Override
                        public Editor putLong(String s, long l) {
                            return null;
                        }

                        @Override
                        public Editor putFloat(String s, float v) {
                            return null;
                        }

                        @Override
                        public Editor putBoolean(String s, boolean b) {
                            booleanHolder.put(s, b);
                            return this;
                        }

                        @Override
                        public Editor remove(String s) {
                            return null;
                        }

                        @Override
                        public Editor clear() {
                            return null;
                        }

                        @Override
                        public boolean commit() {
                            return true;
                        }

                        @Override
                        public void apply() {

                        }
                    };
                }

                @Override
                public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {

                }

                @Override
                public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {

                }
            };
            prefsHolder.put(namespace, pref);
        }

        return pref;
    }
}
