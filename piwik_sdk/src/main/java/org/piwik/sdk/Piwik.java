/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.net.MalformedURLException;
import java.util.HashMap;


public class Piwik {
    public static final String LOGGER_PREFIX = "PIWIK:";

    private final static Object lock = new Object();

    private static HashMap<Application, Piwik> applications = new HashMap<Application, Piwik>();

    private Application application;

    private boolean optOut = false;

    private boolean dryRun = false;

    protected final static Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

    private Piwik(Application application) {
        this.application = application;
    }

    public static Piwik getInstance(Application application) {
        synchronized (lock) {
            Piwik piwik = applications.get(application);
            if (piwik != null) {
                return piwik;
            }
            piwik = new Piwik(application);
            applications.put(application, piwik);
            return piwik;
        }
    }

    /**
     * @param trackerUrl (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId     (required) id of site
     * @param authToken  (optional) could be null or valid auth token. @deprecated since Piwik 2.8.
     *
     * @return Tracker object
     * @throws MalformedURLException
     */
    public Tracker newTracker(String trackerUrl, int siteId, String authToken) throws MalformedURLException {
        return new Tracker(trackerUrl, siteId, authToken, this);
    }

    public Tracker newTracker(String trackerUrl, int siteId) throws MalformedURLException {
        return new Tracker(trackerUrl, siteId, null, this);
    }

    public void setAppOptOut(boolean optOut) {
        this.optOut = optOut;
    }

    public boolean isOptOut() {
        return optOut;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * The dryRun flag set to true prevents any data from being sent to Piwik.
     * The dryRun flag should be set whenever you are testing or debugging an implementation and do not want
     * test data to appear in your Piwik reports. To set the dry run flag, use:
     * <p/>
     * Piwik.getInstance(this).setDryRun(true);
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getApplicationDomain() {
        return application.getPackageName();
    }

    public Context getApplicationContext() {
        return application.getApplicationContext();
    }

    public SharedPreferences getSharedPreferences(String s, int modePrivate) {
        return application.getSharedPreferences(s, modePrivate);
    }
}
