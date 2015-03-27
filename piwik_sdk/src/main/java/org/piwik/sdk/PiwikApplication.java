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
     * Gives you a persisted Tracker object.
     * <p/>
     * The returned Tracker is not threadsafe at the moment.
     * For use in threads create yourself a new tracker, see {@link #newTracker()}
     *
     * @return a shared Tracker
     */
    public synchronized Tracker getTracker() {
        if (mPiwikTracker == null) {
            mPiwikTracker = newTracker();
        }
        return mPiwikTracker;
    }

    /**
     * Creates a new Tracker instance.
     *
     * @return a new Tracker, just for you.
     */
    public Tracker newTracker() {
        try {
            return getPiwik().newTracker(getTrackerUrl(), getSiteId(), getAuthToken());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Tracker URL was malformed.");
        }
    }

    /**
     * The URL of your remote Piwik server.
     *
     * @return
     */
    public abstract String getTrackerUrl();

    /**
     * The siteID you specified for this application in Piwik.
     *
     * @return
     */
    public abstract Integer getSiteId();


    /**
     * @deprecated An authoken from the Piwik server that allows more advanced features.
     * It is encouraged that you do not use this within your app as the token can't be stored securely.
     */
    @Deprecated
    public String getAuthToken() {
        return null;
    }

}
