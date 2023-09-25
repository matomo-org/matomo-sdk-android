package org.matomo.sdk

import android.content.SharedPreferences
import java.util.UUID

class LegacySettingsPorter(matomo: Matomo) {
    private val mLegacyPrefs: SharedPreferences

    init {
        mLegacyPrefs = matomo.preferences
    }

    fun port(tracker: Tracker) {
        val newSettings = tracker.preferences
        if (mLegacyPrefs.getBoolean(LEGACY_PREF_OPT_OUT, false)) {
            newSettings.edit()
                .putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, true)
                .apply()
            mLegacyPrefs.edit().remove(LEGACY_PREF_OPT_OUT).apply()
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_USER_ID)) {
            newSettings.edit()
                .putString(Tracker.PREF_KEY_TRACKER_USERID, mLegacyPrefs.getString(LEGACY_PREF_USER_ID, UUID.randomUUID().toString()))
                .apply()
            mLegacyPrefs.edit().remove(LEGACY_PREF_USER_ID).apply()
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_FIRST_VISIT)) {
            newSettings.edit().putLong(
                Tracker.PREF_KEY_TRACKER_FIRSTVISIT,
                mLegacyPrefs.getLong(LEGACY_PREF_FIRST_VISIT, -1L)
            ).apply()
            mLegacyPrefs.edit().remove(LEGACY_PREF_FIRST_VISIT).apply()
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_VISITCOUNT)) {
            newSettings.edit().putLong(
                Tracker.PREF_KEY_TRACKER_VISITCOUNT,
                mLegacyPrefs.getInt(LEGACY_PREF_VISITCOUNT, 0)
                    .toLong()
            ).apply()
            mLegacyPrefs.edit().remove(LEGACY_PREF_VISITCOUNT).apply()
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_PREV_VISIT)) {
            newSettings.edit().putLong(
                Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT,
                mLegacyPrefs.getLong(LEGACY_PREF_PREV_VISIT, -1)
            ).apply()
            mLegacyPrefs.edit().remove(LEGACY_PREF_PREV_VISIT).apply()
        }
        for ((key) in mLegacyPrefs.all) {
            if (key.startsWith("downloaded:")) {
                newSettings.edit().putBoolean(key, true).apply()
                mLegacyPrefs.edit().remove(key).apply()
            }
        }
    }

    companion object {
        const val LEGACY_PREF_OPT_OUT = "matomo.optout"
        const val LEGACY_PREF_USER_ID = "tracker.userid"
        const val LEGACY_PREF_FIRST_VISIT = "tracker.firstvisit"
        const val LEGACY_PREF_VISITCOUNT = "tracker.visitcount"
        const val LEGACY_PREF_PREV_VISIT = "tracker.previousvisit"
    }
}
