/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;

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
     *
     */
    public abstract String getTrackerUrl();

    /**
     * The siteID you specified for this application in Piwik.
     *
     */
    public abstract Integer getSiteId();

}
