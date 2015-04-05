/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import org.piwik.sdk.tools.Logy;

import java.net.MalformedURLException;


public class Piwik {
    public static final String LOGGER_PREFIX = "PIWIK:";
    public static final String PREFERENCE_FILE_NAME = "org.piwik.sdk";
    public static final String PREFERENCE_KEY_OPTOUT = "piwik.optout";
    private final Context mContext;
    private boolean mOptOut = false;
    private boolean mDryRun = false;

    private static Piwik sInstance;
    private boolean mDebug = BuildConfig.DEBUG;

    public static synchronized Piwik getInstance(Context context) {
        if (sInstance == null)
            sInstance = new Piwik(context);
        return sInstance;
    }

    private Piwik(Context context) {
        mContext = context.getApplicationContext();
        mOptOut = getSharedPreferences().getBoolean(PREFERENCE_KEY_OPTOUT, false);
    }

    protected Context getContext() {
        return mContext;
    }

    /**
     * @param trackerUrl (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId     (required) id of site
     * @param authToken  (optional) could be null or valid auth token.
     * @return Tracker object
     * @throws MalformedURLException
     * @deprecated Use {@link #newTracker(String, int)} as there are security concerns over the authToken.
     */
    @Deprecated
    public Tracker newTracker(String trackerUrl, int siteId, String authToken) throws MalformedURLException {
        return new Tracker(trackerUrl, siteId, authToken, this);
    }

    /**
     * @param trackerUrl (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId     (required) id of site
     * @return Tracker object
     * @throws MalformedURLException
     */
    public Tracker newTracker(String trackerUrl, int siteId) throws MalformedURLException {
        return new Tracker(trackerUrl, siteId, null, this);
    }

    /**
     * Use this to disable Piwik, e.g. if the user opted out of tracking.
     * Piwik will persist the choice and remain disable on next instance creation.</p>
     * The choice is stored in {@link #PREFERENCE_FILE_NAME} under the key {@link #PREFERENCE_KEY_OPTOUT}.
     *
     * @param optOut true to disable reporting
     */
    public void setOptOut(boolean optOut) {
        mOptOut = optOut;
        getSharedPreferences().edit().putBoolean(PREFERENCE_KEY_OPTOUT, optOut).commit();
    }

    /**
     *
     * @return true if Piwik is currently disabled
     */
    public boolean isOptOut() {
        return mOptOut;
    }

    public boolean isDryRun() {
        return mDryRun;
    }

    public boolean isDebug() {
        return mDebug;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
        Logy.sLoglevel = debug ? Logy.VERBOSE : Logy.NORMAL;
    }

    /**
     * The dryRun flag set to true prevents any data from being sent to Piwik.
     * The dryRun flag should be set whenever you are testing or debugging an implementation and do not want
     * test data to appear in your Piwik reports. To set the dry run flag, use:
     *
     * @param dryRun true if you don't want to send any data to piwik
     */
    public void setDryRun(boolean dryRun) {
        mDryRun = dryRun;
    }

    public String getApplicationDomain() {
        return getContext().getPackageName();
    }

    /**
     * Returns the shared preferences used by Piwik that are stored under {@link #PREFERENCE_FILE_NAME}
     * @return Piwik's SharedPreferences instance
     */
    public SharedPreferences getSharedPreferences() {
        return getContext().getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
    }
}
