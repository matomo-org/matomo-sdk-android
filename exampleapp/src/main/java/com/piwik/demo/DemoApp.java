/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package com.piwik.demo;

import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.Tracker;

public class DemoApp extends PiwikApplication {

    @Override
    public String getTrackerUrl() {
        return "http://demo.piwik.org/";
    }

    @Override
    public Integer getSiteId() {
        return 53;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initPiwik();
    }


    private void initPiwik() {
        // Print debug output when working on an app.
        getPiwik().setDebug(BuildConfig.DEBUG);

        // When working on an app we don't want to skew tracking results.
        getPiwik().setDryRun(BuildConfig.DEBUG);

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        // String userEmail = ....preferences....getString
        // getTracker().setUserId(userEmail);

        // Track this app install, this will only trigger once per app version.
        // i.e. "http://com.piwik.demo:1/185DECB5CFE28FDB2F45887022D668B4"
        getTracker().trackAppDownload(this, Tracker.ExtraIdentifier.APK_CHECKSUM);
        // Alternative:
        // i.e. "http://com.piwik.demo:1/com.android.vending"
        // getTracker().trackAppDownload();
    }
}
