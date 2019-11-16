package testhelpers;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class TestPreferences implements SharedPreferences {
    Map<String, Object> mMap = new HashMap<>();
    Editor mEditor = new TestEditor();

    @Override
    public Map<String, ?> getAll() {
        return mMap;
    }

    @Nullable
    @Override
    public String getString(String key, String defValue) {
        if (!mMap.containsKey(key)) return defValue;
        return (String) mMap.get(key);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        if (!mMap.containsKey(key)) return defValues;
        //noinspection unchecked
        return (Set<String>) mMap.get(key);
    }

    @Override
    public int getInt(String key, int defValue) {
        if (!mMap.containsKey(key)) return defValue;
        return (int) mMap.get(key);
    }

    @Override
    public long getLong(String key, long defValue) {
        if (!mMap.containsKey(key)) return defValue;
        return (long) mMap.get(key);
    }

    @Override
    public float getFloat(String key, float defValue) {
        if (!mMap.containsKey(key)) return defValue;
        return (float) mMap.get(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (!mMap.containsKey(key)) return defValue;
        return (boolean) mMap.get(key);
    }

    @Override
    public boolean contains(String key) {
        return mMap.containsKey(key);
    }

    @Override
    public Editor edit() {
        return mEditor;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    public class TestEditor implements Editor {

        @Override
        public Editor putString(String key, String value) {
            mMap.put(key, value);
            return mEditor;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            mMap.put(key, values);
            return mEditor;
        }

        @Override
        public Editor putInt(String key, int value) {
            mMap.put(key, value);
            return mEditor;
        }

        @Override
        public Editor putLong(String key, long value) {
            mMap.put(key, value);
            return mEditor;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mMap.put(key, value);
            return mEditor;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mMap.put(key, value);
            return mEditor;
        }

        @Override
        public Editor remove(String key) {
            mMap.remove(key);
            return mEditor;
        }

        @Override
        public Editor clear() {
            mMap.clear();
            return mEditor;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {

        }
    }
}
