package org.piwik.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.piwik.sdk.tools.Logy;

import java.util.Arrays;
import java.util.List;


public class InstallReferrerReceiver extends BroadcastReceiver {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "InstallReferrerReceiver";

    // Google Play
    static final String REFERRER_SOURCE_GPLAY = "com.android.vending.INSTALL_REFERRER";
    static final String ARG_KEY_GPLAY_REFERRER = "referrer";

    static final String PREF_KEY_INSTALL_REFERRER_EXTRAS = "referrer.extras";
    static final List<String> RESPONSIBILITIES = Arrays.asList(REFERRER_SOURCE_GPLAY);

    @Override
    public void onReceive(Context context, Intent intent) {
        Logy.d("LOGGER_TAG", intent.toString());
        if (intent.getAction() == null || !RESPONSIBILITIES.contains(intent.getAction())) {
            Logy.w(LOGGER_TAG, "Got called outside our responsibilities: " + intent.getAction());
            return;
        }
        SharedPreferences piwikPreferences = Piwik.getInstance(context.getApplicationContext()).getSharedPreferences();
        if (intent.getAction().equals(REFERRER_SOURCE_GPLAY)) {
            String referrer = intent.getStringExtra(ARG_KEY_GPLAY_REFERRER);
            if (referrer != null) {
                piwikPreferences.edit().putString(PREF_KEY_INSTALL_REFERRER_EXTRAS, referrer).apply();
                Logy.d("LOGGER_TAG", "Stored Google Play referrer extras: " + referrer);
            }
        }
    }
}

