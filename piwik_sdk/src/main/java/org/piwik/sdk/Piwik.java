/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.MalformedURLException;


public class Piwik {
    public static final String LOGGER_PREFIX = "PIWIK:";
    private final Context mContext;
    private boolean mOptOut = false;
    private boolean mDryRun = false;

    protected final static Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

    private static Piwik sInstance;

    public static synchronized Piwik getInstance(Context context) {
        if (sInstance == null)
            sInstance = new Piwik(context);
        return sInstance;
    }

    private Piwik(Context context) {
        mContext = context.getApplicationContext();
    }

    protected Context getContext() {
        return mContext;
    }

    /**
     * @deprecated
     * Use {@link #newTracker(String, int)} as there are security concerns over the authToken.
     *
     * @param trackerUrl (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId     (required) id of site
     * @param authToken  (optional) could be null or valid auth token.
     * @return Tracker object
     * @throws MalformedURLException
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

    public void setAppOptOut(boolean optOut) {
        mOptOut = optOut;
    }

    public boolean isOptOut() {
        return mOptOut;
    }

    public boolean isDryRun() {
        return mDryRun;
    }

    /**
     * The dryRun flag set to true prevents any data from being sent to Piwik.
     * The dryRun flag should be set whenever you are testing or debugging an implementation and do not want
     * test data to appear in your Piwik reports. To set the dry run flag, use:
     * @param dryRun true if you don't want to send any data to piwik
     */
    public void setDryRun(boolean dryRun) {
        mDryRun = dryRun;
    }

    public String getApplicationDomain() {
        return getContext().getPackageName();
    }

    protected SharedPreferences getSharedPreferences(Tracker tracker) {
        String preferenceName = tracker.getAPIUrl().toString();
        preferenceName = preferenceName.replace("https://","").replace("http://","").replace("/","_");
        return getContext().getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
    }
}
