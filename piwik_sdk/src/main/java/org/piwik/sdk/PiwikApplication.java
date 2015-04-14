/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;
import android.os.Build;

import java.net.MalformedURLException;

public abstract class PiwikApplication extends Application {
    private Tracker mPiwikTracker;

    public Piwik getPiwik() {
        return Piwik.getInstance(this);
    }

    /**
     * Gives you an all purpose thread-safe persisted Tracker object.
     *
     * @return a shared Tracker
     */
    public synchronized Tracker getTracker() {
        if (mPiwikTracker == null) {
            try {
                mPiwikTracker = getPiwik().newTracker(getTrackerUrl(), getSiteId());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException("Tracker URL was malformed.");
            }
        }
        return mPiwikTracker;
    }

    /**
     * The URL of your remote Piwik server.
     */
    public abstract String getTrackerUrl();

    /**
     * The siteID you specified for this application in Piwik.
     */
    public abstract Integer getSiteId();


    @Override
    public void onLowMemory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH && mPiwikTracker != null) {
            mPiwikTracker.dispatch();
        }
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if ((level == TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_COMPLETE) && mPiwikTracker != null) {
            mPiwikTracker.dispatch();
        }
        super.onTrimMemory(level);
    }

}
