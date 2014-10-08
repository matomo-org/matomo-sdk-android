/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;
import android.util.Log;

import java.net.MalformedURLException;

public abstract class PiwikApplication extends Application {
    Tracker piwikTracker;

    public Piwik getGlobalSettings(){
        return Piwik.getInstance(this);
    }

    public Tracker getTracker() {
        if (piwikTracker != null) {
            return piwikTracker;
        }

        try {
            piwikTracker = getGlobalSettings().newTracker(getTrackerUrl(), getSiteId(), getAuthToken());
        } catch (MalformedURLException e) {
            Log.i(Tracker.LOGGER_TAG, getTrackerUrl());
            Log.w(Tracker.LOGGER_TAG, "url is malformed", e);
            return null;
        }

        return piwikTracker;

    }

    public String getTrackerUrl() {
        return "";
    }

    public String getAuthToken() {
        return "";
    }

    public Integer getSiteId() {
        return 1;
    }

}
