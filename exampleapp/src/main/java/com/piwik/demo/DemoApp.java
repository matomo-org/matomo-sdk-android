/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package com.piwik.demo;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.PiwikApplication;

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
        Piwik.getInstance(this).setDebug(true);
        super.onCreate();
    }
}
