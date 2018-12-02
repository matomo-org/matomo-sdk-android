package org.matomo.sdk.extra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.matomo.sdk.Matomo;

import java.util.Collections;
import java.util.List;

import timber.log.Timber;


public class InstallReferrerReceiver extends BroadcastReceiver {
    private static final String LOGGER_TAG = Matomo.LOGGER_PREFIX + "InstallReferrerReceiver";

    // Google Play
    static final String REFERRER_SOURCE_GPLAY = "com.android.vending.INSTALL_REFERRER";
    static final String ARG_KEY_GPLAY_REFERRER = "referrer";

    static final String PREF_KEY_INSTALL_REFERRER_EXTRAS = "referrer.extras";
    static final List<String> RESPONSIBILITIES = Collections.singletonList(REFERRER_SOURCE_GPLAY);

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.tag(LOGGER_TAG).d(intent.toString());
        if (intent.getAction() == null || !RESPONSIBILITIES.contains(intent.getAction())) {
            Timber.tag(LOGGER_TAG).w("Got called outside our responsibilities: %s", intent.getAction());
            return;
        }
        if (intent.getBooleanExtra("forwarded", false)) {
            Timber.tag(LOGGER_TAG).d("Dropping forwarded intent");
            return;
        }
        SharedPreferences preferences = Matomo.getInstance(context.getApplicationContext()).getPreferences();
        if (intent.getAction().equals(REFERRER_SOURCE_GPLAY)) {
            String referrer = intent.getStringExtra(ARG_KEY_GPLAY_REFERRER);
            if (referrer != null) {
                preferences.edit().putString(PREF_KEY_INSTALL_REFERRER_EXTRAS, referrer).apply();
                Timber.tag(LOGGER_TAG).d("Stored Google Play referrer extras: %s", referrer);
            }
        }
        // Forward to other possible recipients
        intent.setComponent(null);
        intent.setPackage(context.getPackageName());
        intent.putExtra("forwarded", true);
        context.sendBroadcast(intent);
    }
}
