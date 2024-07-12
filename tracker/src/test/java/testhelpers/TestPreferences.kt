package testhelpers

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class TestPreferences : SharedPreferences {
    var map: MutableMap<String, Any?> = HashMap()
    var editor: SharedPreferences.Editor = TestEditor()

    override fun getAll(): Map<String, *> {
        return map
    }

    override fun getString(key: String, defValue: String?): String? {
        if (!map.containsKey(key)) return defValue
        return map[key] as String?
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        if (!map.containsKey(key)) return defValues
        return map[key] as Set<String>?
    }

    override fun getInt(key: String, defValue: Int): Int {
        if (!map.containsKey(key)) return defValue
        return map[key] as Int
    }

    override fun getLong(key: String, defValue: Long): Long {
        if (!map.containsKey(key)) return defValue
        return map[key] as Long
    }

    override fun getFloat(key: String, defValue: Float): Float {
        if (!map.containsKey(key)) return defValue
        return map[key] as Float
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        if (!map.containsKey(key)) return defValue
        return map[key] as Boolean
    }

    override fun contains(key: String): Boolean {
        return map.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return editor
    }

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
    }

    inner class TestEditor : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            map[key] = value
            return editor
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            map[key] = values
            return editor
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            map[key] = value
            return editor
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            map[key] = value
            return editor
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            map[key] = value
            return editor
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            map[key] = value
            return editor
        }

        override fun remove(key: String): SharedPreferences.Editor {
            map.remove(key)
            return editor
        }

        override fun clear(): SharedPreferences.Editor {
            map.clear()
            return editor
        }

        override fun commit(): Boolean {
            return true
        }

        override fun apply() {
        }
    }
}
