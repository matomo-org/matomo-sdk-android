package org.piwik.sdk;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.piwik.sdk.Tracker.ExtraIdentifier;
import org.piwik.sdk.tools.Checksum;
import org.piwik.sdk.tools.Logy;

import java.io.File;

class DownloadTrackingHelper {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "DownloadTrackingHelper";
    private static final String INSTALL_SOURCE_GOOGLE_PLAY = "com.android.vending";
    private final Tracker mTracker;
    private final Object TRACK_ONCE_LOCK = new Object();
    private final PackageManager mPackMan;
    private final String mPackageName;
    private final SharedPreferences mPreferences;
    private final Piwik mPiwik;
    private final Context mContext;

    DownloadTrackingHelper(Tracker tracker) {
        mTracker = tracker;
        mPiwik = tracker.getPiwik();
        mContext = mPiwik.getContext();
        mPreferences = mPiwik.getSharedPreferences();
        mPackageName = mContext.getPackageName();
        mPackMan = mContext.getPackageManager();
    }

    void trackOnce(@NonNull ExtraIdentifier extra) {
        try {
            PackageInfo pkgInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            String firedKey = "downloaded:" + pkgInfo.packageName + ":" + pkgInfo.versionCode;
            synchronized (TRACK_ONCE_LOCK) {
                if (!mPreferences.getBoolean(firedKey, false)) {
                    mPreferences.edit().putBoolean(firedKey, true).apply();
                    trackNewAppDownload(extra);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    void trackNewAppDownload(@NonNull final ExtraIdentifier extra) {
        String referringApp = mPackMan.getInstallerPackageName(mPackageName);
        if (INSTALL_SOURCE_GOOGLE_PLAY.equals(referringApp)) {
            Logy.d(LOGGER_TAG, "Google Play is install source, deferring tracking.");
            // Installed by Google Play
            // If we are called from from within Application.onCreate wait for the install REFERRER
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    trackNewAppDownloadInternal(extra);
                }
            }, 3000);
        } else {
            trackNewAppDownloadInternal(extra);
        }
    }

    void trackNewAppDownloadInternal(@NonNull ExtraIdentifier extra) {
        Logy.d(LOGGER_TAG, "Tracking app download...");
        StringBuilder installationIdentifier = new StringBuilder();
        try {
            installationIdentifier.append("http://").append(mPackageName); // Identifies the app

            PackageInfo pkgInfo = mPackMan.getPackageInfo(mPackageName, 0);
            installationIdentifier.append(":").append(pkgInfo.versionCode);

            if (extra == ExtraIdentifier.APK_CHECKSUM) {
                ApplicationInfo appInfo = mPackMan.getApplicationInfo(mPackageName, 0);
                if (appInfo.sourceDir != null) {
                    String md5Identifier = null;
                    try {
                        md5Identifier = Checksum.getMD5Checksum(new File(appInfo.sourceDir));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (md5Identifier != null)
                        installationIdentifier.append("/").append(md5Identifier);
                }
            }

            // Usual USEFUL values of this field will be: "com.android.vending" or "com.android.browser", i.e. app packagenames.
            // This is not guaranteed, values can also look like: app_process /system/bin com.android.commands.pm.Pm install -r /storage/sdcard0/...
            String referringApp = mPackMan.getInstallerPackageName(mPackageName);
            if (referringApp != null && referringApp.length() > 200)
                referringApp = referringApp.substring(0, 200);

            if (referringApp != null && referringApp.equals("com.android.vending")) {
                // For this type of install source we could have extra referral information
                String referrerExtras = mPreferences.getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
                if (referrerExtras != null) {
                    referringApp = referringApp + "/?" + referrerExtras;
                }
            }

            if (referringApp != null)
                referringApp = "http://" + referringApp;

            mTracker.track(new TrackMe()
                    .set(QueryParams.EVENT_CATEGORY, "Application")
                    .set(QueryParams.EVENT_ACTION, "downloaded")
                    .set(QueryParams.ACTION_NAME, "application/downloaded")
                    .set(QueryParams.URL_PATH, "/application/downloaded")
                    .set(QueryParams.DOWNLOAD, installationIdentifier.toString())
                    .set(QueryParams.REFERRER, referringApp)); // Can be null in which case the TrackMe removes the REFERRER parameter.
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Logy.d(LOGGER_TAG, "... app download tracked.");
    }
}
