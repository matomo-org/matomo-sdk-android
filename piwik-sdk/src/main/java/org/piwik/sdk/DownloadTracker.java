package org.piwik.sdk;


import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.piwik.sdk.tools.Checksum;

import java.io.File;

import timber.log.Timber;

public class DownloadTracker {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "DownloadTrackingHelper";
    private static final String INSTALL_SOURCE_GOOGLE_PLAY = "com.android.vending";
    private final Tracker mTracker;
    private final TrackMe mBaseTrackMe;
    private final Object TRACK_ONCE_LOCK = new Object();
    private final PackageManager mPackMan;
    private final String mPackageName;
    private final SharedPreferences mPreferences;
    private String mVersion;
    private PackageInfo mPkgInfo;

    public enum Extra {
        /**
         * The MD5 checksum of the apk file.
         * com.example.pkg:1/ABCDEF01234567
         */
        APK_CHECKSUM,
        /**
         * No extra identifier.
         * com.example.pkg:1
         */
        NONE
    }

    public DownloadTracker(Tracker tracker) {
        this(tracker, new TrackMe());
    }

    public DownloadTracker(Tracker tracker, TrackMe baseTrackMe) {
        mTracker = tracker;
        mBaseTrackMe = baseTrackMe;
        mPreferences = tracker.getPiwik().getSharedPreferences();
        mPackageName = tracker.getPiwik().getContext().getPackageName();
        mPackMan = tracker.getPiwik().getContext().getPackageManager();
        try {
            mPkgInfo = mPackMan.getPackageInfo(mPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setVersion(@Nullable String version) {
        mVersion = version;
    }

    public String getVersion() {
        if (mVersion != null) return mVersion;
        return Integer.toString(mPkgInfo.versionCode);
    }

    public void trackOnce(@NonNull Extra extra) {
        String firedKey = "downloaded:" + mPackageName + ":" + getVersion();
        synchronized (TRACK_ONCE_LOCK) {
            if (!mPreferences.getBoolean(firedKey, false)) {
                mPreferences.edit().putBoolean(firedKey, true).apply();
                trackNewAppDownload(extra);
            }
        }
    }

    public void trackNewAppDownload(@NonNull final Extra extra) {
        final Thread trackTask = new Thread(new Runnable() {
            @Override
            public void run() {
                trackNewAppDownloadInternal(extra);
            }
        });

        boolean delay = INSTALL_SOURCE_GOOGLE_PLAY.equals(mPackMan.getInstallerPackageName(mPackageName));
        if (delay) {
            // Delay tracking incase we were called from within Application.onCreate
            Timber.tag(LOGGER_TAG).d("Google Play is install source, deferring tracking.");
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (extra == Extra.APK_CHECKSUM) {
                    // Don't do APK checksum on this thread, we don't want to block.
                    trackTask.start();
                } else {
                    trackTask.run();
                }
            }
        }, delay ? 3000 : 0);
    }

    private void trackNewAppDownloadInternal(@NonNull Extra extra) {
        Timber.tag(LOGGER_TAG).d("Tracking app download...");

        StringBuilder installIdentifier = new StringBuilder();
        installIdentifier.append("http://").append(mPackageName).append(":").append(getVersion());

        if (extra == Extra.APK_CHECKSUM) {
            if (mPkgInfo == null) return;
            if (mPkgInfo.applicationInfo != null && mPkgInfo.applicationInfo.sourceDir != null) {
                try {
                    String md5Identifier = Checksum.getMD5Checksum(new File(mPkgInfo.applicationInfo.sourceDir));
                    if (md5Identifier != null) installIdentifier.append("/").append(md5Identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Usual USEFUL values of this field will be: "com.android.vending" or "com.android.browser", i.e. app packagenames.
        // This is not guaranteed, values can also look like: app_process /system/bin com.android.commands.pm.Pm install -r /storage/sdcard0/...
        String referringApp = mPackMan.getInstallerPackageName(mPackageName);
        if (referringApp != null && referringApp.length() > 200) referringApp = referringApp.substring(0, 200);

        if (referringApp != null && referringApp.equals(INSTALL_SOURCE_GOOGLE_PLAY)) {
            // For this type of install source we could have extra referral information
            String referrerExtras = mPreferences.getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
            if (referrerExtras != null) referringApp = referringApp + "/?" + referrerExtras;
        }

        if (referringApp != null) referringApp = "http://" + referringApp;

        mTracker.track(new TrackMe(mBaseTrackMe)
                .set(QueryParams.EVENT_CATEGORY, "Application")
                .set(QueryParams.EVENT_ACTION, "downloaded")
                .set(QueryParams.ACTION_NAME, "application/downloaded")
                .set(QueryParams.URL_PATH, "/application/downloaded")
                .set(QueryParams.DOWNLOAD, installIdentifier.toString())
                .set(QueryParams.REFERRER, referringApp)); // Can be null in which case the TrackMe removes the REFERRER parameter.

        Timber.tag(LOGGER_TAG).d("... app download tracked.");
    }
}
