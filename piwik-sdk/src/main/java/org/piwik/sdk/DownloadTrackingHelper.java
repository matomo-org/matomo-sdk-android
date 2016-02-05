package org.piwik.sdk;


import android.content.Context;
import android.content.SharedPreferences;
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
        final Thread trackTask = new Thread(new Runnable() {
            @Override
            public void run() {
                trackNewAppDownloadInternal(extra);
            }
        });

        boolean delay = INSTALL_SOURCE_GOOGLE_PLAY.equals(mPackMan.getInstallerPackageName(mPackageName));
        if (delay) {
            // Delay tracking incase we were called from within Application.onCreate
            Logy.d(LOGGER_TAG, "Google Play is install source, deferring tracking.");
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (extra == ExtraIdentifier.APK_CHECKSUM) {
                    // Don't do APK checksum on this thread, we don't want to block.
                    trackTask.start();
                } else {
                    trackTask.run();
                }
            }
        }, delay ? 3000 : 0);
    }

    void trackNewAppDownloadInternal(@NonNull ExtraIdentifier extra) {
        Logy.d(LOGGER_TAG, "Tracking app download...");
        PackageInfo pkgInfo = null;
        try {
            pkgInfo = mPackMan.getPackageInfo(mPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (pkgInfo == null)
            return;

        StringBuilder installIdentifier = new StringBuilder();
        installIdentifier.append("http://").append(mPackageName).append(":").append(pkgInfo.versionCode);

        if (extra == ExtraIdentifier.APK_CHECKSUM) {
            if (pkgInfo.applicationInfo != null && pkgInfo.applicationInfo.sourceDir != null) {
                try {
                    String md5Identifier = Checksum.getMD5Checksum(new File(pkgInfo.applicationInfo.sourceDir));
                    if (md5Identifier != null)
                        installIdentifier.append("/").append(md5Identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Usual USEFUL values of this field will be: "com.android.vending" or "com.android.browser", i.e. app packagenames.
        // This is not guaranteed, values can also look like: app_process /system/bin com.android.commands.pm.Pm install -r /storage/sdcard0/...
        String referringApp = mPackMan.getInstallerPackageName(mPackageName);
        if (referringApp != null && referringApp.length() > 200)
            referringApp = referringApp.substring(0, 200);

        if (referringApp != null && referringApp.equals(INSTALL_SOURCE_GOOGLE_PLAY)) {
            // For this type of install source we could have extra referral information
            String referrerExtras = mPreferences.getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
            if (referrerExtras != null)
                referringApp = referringApp + "/?" + referrerExtras;
        }

        if (referringApp != null)
            referringApp = "http://" + referringApp;

        mTracker.track(new TrackMe()
                .set(QueryParams.EVENT_CATEGORY, "Application")
                .set(QueryParams.EVENT_ACTION, "downloaded")
                .set(QueryParams.ACTION_NAME, "application/downloaded")
                .set(QueryParams.URL_PATH, "/application/downloaded")
                .set(QueryParams.DOWNLOAD, installIdentifier.toString())
                .set(QueryParams.REFERRER, referringApp)); // Can be null in which case the TrackMe removes the REFERRER parameter.

        Logy.d(LOGGER_TAG, "... app download tracked.");
    }
}
