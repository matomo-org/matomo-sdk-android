/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.piwik.sdk.dispatcher.DispatcherFactory;
import org.piwik.sdk.tools.BuildInfo;
import org.piwik.sdk.tools.Checksum;
import org.piwik.sdk.tools.DeviceHelper;
import org.piwik.sdk.tools.PropertySource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;


public class Piwik {
    public static final String LOGGER_PREFIX = "PIWIK:";
    private static final String LOGGER_TAG = "PIWIK";
    private static final String BASE_PREFERENCE_FILE = "org.piwik.sdk";
    private final Map<Tracker, SharedPreferences> mPreferenceMap = new HashMap<>();
    private final Context mContext;

    private static Piwik sInstance;
    private SharedPreferences mBasePreferences;

    public static synchronized Piwik getInstance(Context context) {
        if (sInstance == null) sInstance = new Piwik(context);
        return sInstance;
    }

    private Piwik(Context context) {
        mContext = context.getApplicationContext();
        mBasePreferences = context.getSharedPreferences(BASE_PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * @param url    (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId (required) id of site
     * @param name   unique name for this Tracker. Used to store Tracker settings independent of URL and id changes.
     * @return Tracker object
     * @throws RuntimeException if the supplied Piwik-Tracker URL is incompatible
     */
    public synchronized Tracker newTracker(@NonNull String url, int siteId, String name) {
        URL trackerUrl;
        try {
            if (url.endsWith("piwik.php") || url.endsWith("piwik-proxy.php")) {
                trackerUrl = new URL(url);
            } else {
                if (!url.endsWith("/")) url += "/";
                trackerUrl = new URL(url + "piwik.php");
            }
        } catch (MalformedURLException e) { throw new RuntimeException(e); }
        return new Tracker(trackerUrl, siteId, this, name);
    }

    public String getApplicationDomain() {
        return getContext().getPackageName();
    }

    /**
     * Base preferences, tracker idenpendent.
     */
    public SharedPreferences getPiwikPreferences() {
        return mBasePreferences;
    }

    /**
     * @return Tracker specific settings object
     */
    public SharedPreferences getTrackerPreferences(@NonNull Tracker tracker) {
        synchronized (mPreferenceMap) {
            SharedPreferences newPrefs = mPreferenceMap.get(tracker);
            if (newPrefs == null) {
                String prefName;
                try {
                    prefName = "org.piwik.sdk_" + Checksum.getMD5Checksum(tracker.getName());
                } catch (Exception e) {
                    Timber.tag(LOGGER_TAG).e(e, null);
                    prefName = "org.piwik.sdk_" + tracker.getName();
                }
                newPrefs = getContext().getSharedPreferences(prefName, Context.MODE_PRIVATE);
                mPreferenceMap.put(tracker, newPrefs);
            }
            return newPrefs;
        }
    }

    protected DispatcherFactory getDispatcherFactory() {
        return new DispatcherFactory();
    }

    DeviceHelper getDeviceHelper() {
        return new DeviceHelper(mContext, new PropertySource(), new BuildInfo());
    }
}
