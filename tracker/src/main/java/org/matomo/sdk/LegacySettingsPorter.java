package org.matomo.sdk;


import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.UUID;

public class LegacySettingsPorter {
    static final String LEGACY_PREF_OPT_OUT = "matomo.optout";
    static final String LEGACY_PREF_USER_ID = "tracker.userid";
    static final String LEGACY_PREF_FIRST_VISIT = "tracker.firstvisit";
    static final String LEGACY_PREF_VISITCOUNT = "tracker.visitcount";
    static final String LEGACY_PREF_PREV_VISIT = "tracker.previousvisit";
    private final SharedPreferences mLegacyPrefs;

    public LegacySettingsPorter(@NonNull Matomo matomo) {
        mLegacyPrefs = matomo.getPreferences();
    }

    public void port(Tracker tracker) {
        SharedPreferences newSettings = tracker.getPreferences();
        if (mLegacyPrefs.getBoolean(LEGACY_PREF_OPT_OUT, false)) {
            newSettings.edit().putBoolean(
                    Tracker.PREF_KEY_TRACKER_OPTOUT,
                    true
            ).apply();
            mLegacyPrefs.edit().remove(LEGACY_PREF_OPT_OUT).apply();
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_USER_ID)) {
            newSettings.edit().putString(
                    Tracker.PREF_KEY_TRACKER_USERID,
                    mLegacyPrefs.getString(LEGACY_PREF_USER_ID, UUID.randomUUID().toString())
            ).apply();
            mLegacyPrefs.edit().remove(LEGACY_PREF_USER_ID).apply();
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_FIRST_VISIT)) {
            newSettings.edit().putLong(
                    Tracker.PREF_KEY_TRACKER_FIRSTVISIT,
                    mLegacyPrefs.getLong(LEGACY_PREF_FIRST_VISIT, -1L)
            ).apply();
            mLegacyPrefs.edit().remove(LEGACY_PREF_FIRST_VISIT).apply();
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_VISITCOUNT)) {
            newSettings.edit().putLong(
                    Tracker.PREF_KEY_TRACKER_VISITCOUNT,
                    mLegacyPrefs.getInt(LEGACY_PREF_VISITCOUNT, 0)
            ).apply();
            mLegacyPrefs.edit().remove(LEGACY_PREF_VISITCOUNT).apply();
        }
        if (mLegacyPrefs.contains(LEGACY_PREF_PREV_VISIT)) {
            newSettings.edit().putLong(
                    Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT,
                    mLegacyPrefs.getLong(LEGACY_PREF_PREV_VISIT, -1)
            ).apply();
            mLegacyPrefs.edit().remove(LEGACY_PREF_PREV_VISIT).apply();
        }
        for (Map.Entry<String, ?> oldEntry : mLegacyPrefs.getAll().entrySet()) {
            if (oldEntry.getKey().startsWith("downloaded:")) {
                newSettings.edit().putBoolean(oldEntry.getKey(), true).apply();
                mLegacyPrefs.edit().remove(oldEntry.getKey()).apply();
            }
        }
    }
}
