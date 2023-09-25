/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import org.matomo.sdk.dispatcher.DefaultDispatcherFactory
import org.matomo.sdk.dispatcher.DispatcherFactory
import org.matomo.sdk.tools.BuildInfo
import org.matomo.sdk.tools.Checksum
import org.matomo.sdk.tools.DeviceHelper
import org.matomo.sdk.tools.PropertySource
import timber.log.Timber

class Matomo private constructor(context: Context) {
    private val mPreferenceMap: MutableMap<Tracker, SharedPreferences?> = HashMap()
    val context: Context

    /**
     * Base preferences, tracker independent.
     */
    val preferences: SharedPreferences

    /**
     * If you want to use your own [org.matomo.sdk.dispatcher.Dispatcher]
     */
    var dispatcherFactory: DispatcherFactory = DefaultDispatcherFactory()

    init {
        this.context = context.applicationContext
        preferences = context.getSharedPreferences(BASE_PREFERENCE_FILE, Context.MODE_PRIVATE)
    }

    /**
     * @return Tracker specific settings object
     */
    fun getTrackerPreferences(tracker: Tracker): SharedPreferences? {
        synchronized(mPreferenceMap) {
            var newPrefs = mPreferenceMap[tracker]
            if (newPrefs == null) {
                val prefName: String = try {
                    "org.matomo.sdk_" + Checksum.getMD5Checksum(tracker.name)
                } catch (e: Exception) {
                    Timber.e(e)
                    "org.matomo.sdk_" + tracker.name
                }
                newPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                mPreferenceMap[tracker] = newPrefs
            }
            return newPrefs
        }
    }

    val deviceHelper: DeviceHelper
        get() = DeviceHelper(context, PropertySource(), BuildInfo())

    companion object {
        private const val LOGGER_PREFIX = "MATOMO:"
        private const val BASE_PREFERENCE_FILE = "org.matomo.sdk"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var sInstance: Matomo? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): Matomo? {
            if (sInstance == null) {
                synchronized(Matomo::class.java) { if (sInstance == null) sInstance = Matomo(context) }
            }
            return sInstance
        }

        @JvmStatic
        fun tag(vararg classes: Class<*>): String {
            val tags = arrayOfNulls<String>(classes.size)
            for (i in classes.indices) {
                tags[i] = classes[i].simpleName
            }
            return tag(*tags)
        }

        @JvmStatic
        fun tag(vararg tags: String?): String {
            val sb = StringBuilder(LOGGER_PREFIX)
            for (i in tags.indices) {
                sb.append(tags[i])
                if (i < tags.size - 1) sb.append(":")
            }
            return sb.toString()
        }
    }
}