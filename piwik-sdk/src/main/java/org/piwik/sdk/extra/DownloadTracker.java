package org.piwik.sdk.extra;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.Tracker;
import org.piwik.sdk.tools.Checksum;

import java.io.File;

import timber.log.Timber;

import static android.content.ContentValues.TAG;

public class DownloadTracker {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "DownloadTrackingHelper";
    private static final String INSTALL_SOURCE_GOOGLE_PLAY = "com.android.vending";
    private final Tracker mTracker;
    private final Object mTrackOnceLock = new Object();
    private final PackageManager mPackMan;
    private final SharedPreferences mPreferences;
    private final Context mContext;
    private final boolean mInternalTracking;
    private String mVersion;
    private final PackageInfo mPkgInfo;

    public interface Extra {

        /**
         * Does your {@link Extra} implementation do work intensive stuff?
         * Network? IO?
         *
         * @return true if this should be run async and on a sepperate thread.
         */
        boolean isIntensiveWork();

        /**
         * Example:
         * <br>
         * com.example.pkg:1/ABCDEF01234567
         * <br>
         * "ABCDEF01234567" is the extra identifier here.
         *
         * @return a string that will be used as extra identifier or null
         */
        @Nullable
        String buildExtraIdentifier();

        /**
         * The MD5 checksum of the apk file.
         * com.example.pkg:1/ABCDEF01234567
         */
        class ApkChecksum implements Extra {
            private PackageInfo mPackageInfo;

            public ApkChecksum(Context context) {
                try {
                    mPackageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                } catch (Exception e) {
                    Timber.tag(LOGGER_TAG).e(e);
                    mPackageInfo = null;
                }
            }

            public ApkChecksum(PackageInfo packageInfo) {mPackageInfo = packageInfo;}

            @Override
            public boolean isIntensiveWork() {
                return true;
            }

            @Nullable
            @Override
            public String buildExtraIdentifier() {
                if (mPackageInfo != null && mPackageInfo.applicationInfo != null && mPackageInfo.applicationInfo.sourceDir != null) {
                    try {
                        return Checksum.getMD5Checksum(new File(mPackageInfo.applicationInfo.sourceDir));
                    } catch (Exception e) { Timber.tag(LOGGER_TAG).e(e); }
                }
                return null;
            }
        }

        /**
         * Custom exta identifier. Supply your own \o/.
         */
        abstract class Custom implements Extra {
        }

        /**
         * No extra identifier.
         * com.example.pkg:1
         */
        class None implements Extra {

            @Override
            public boolean isIntensiveWork() {
                return false;
            }

            @Nullable
            @Override
            public String buildExtraIdentifier() {
                return null;
            }
        }
    }

    public DownloadTracker(Tracker tracker) {
        this(tracker, getOurPackageInfo(tracker.getPiwik().getContext()));
    }

    private static PackageInfo getOurPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.tag(LOGGER_TAG).e(e);
            throw new RuntimeException(e);
        }
    }

    public DownloadTracker(Tracker tracker, @NonNull PackageInfo packageInfo) {
        mTracker = tracker;
        mContext = tracker.getPiwik().getContext();
        mPreferences = tracker.getPreferences();
        mPackMan = tracker.getPiwik().getContext().getPackageManager();
        mPkgInfo = packageInfo;
        mInternalTracking = mPkgInfo.packageName.equals(mContext.getPackageName());
    }

    public void setVersion(@Nullable String version) {
        mVersion = version;
    }

    public String getVersion() {
        if (mVersion != null) return mVersion;
        return Integer.toString(mPkgInfo.versionCode);
    }

    public void trackOnce(TrackMe baseTrackme, @NonNull Extra extra) {
        String firedKey = "downloaded:" + mPkgInfo.packageName + ":" + getVersion();
        synchronized (mTrackOnceLock) {
            if (!mPreferences.getBoolean(firedKey, false)) {
                mPreferences.edit().putBoolean(firedKey, true).apply();
                trackNewAppDownload(baseTrackme, extra);
            }
        }
    }

    public void trackNewAppDownload(final TrackMe baseTrackme, @NonNull final Extra extra) {
        // We can only get referrer information if we are tracking our own app download.
        final boolean delay = mInternalTracking && INSTALL_SOURCE_GOOGLE_PLAY.equals(mPackMan.getInstallerPackageName(mPkgInfo.packageName));
        if (delay) {
            // Delay tracking incase we were called from within Application.onCreate
            Timber.tag(LOGGER_TAG).d("Google Play is install source, deferring tracking.");
        }
        final Thread trackTask = new Thread(new Runnable() {
            @Override
            public void run() {
                if (delay) try {Thread.sleep(3000);} catch (Exception e) { Timber.tag(TAG).e(e, null);}
                trackNewAppDownloadInternal(baseTrackme, extra);
            }
        });
        if (!delay && !extra.isIntensiveWork()) trackTask.run();
        else trackTask.start();
    }

    private void trackNewAppDownloadInternal(TrackMe baseTrackMe, @NonNull Extra extra) {
        Timber.tag(LOGGER_TAG).d("Tracking app download...");

        StringBuilder installIdentifier = new StringBuilder();
        installIdentifier.append("http://").append(mPkgInfo.packageName).append(":").append(getVersion());

        String extraIdentifier = extra.buildExtraIdentifier();
        if (extraIdentifier != null) installIdentifier.append("/").append(extraIdentifier);

        // Usual USEFUL values of this field will be: "com.android.vending" or "com.android.browser", i.e. app packagenames.
        // This is not guaranteed, values can also look like: app_process /system/bin com.android.commands.pm.Pm install -r /storage/sdcard0/...
        String referringApp = mPackMan.getInstallerPackageName(mPkgInfo.packageName);
        if (referringApp != null && referringApp.length() > 200) referringApp = referringApp.substring(0, 200);

        if (referringApp != null && referringApp.equals(INSTALL_SOURCE_GOOGLE_PLAY)) {
            // For this type of install source we could have extra referral information
            String referrerExtras = mTracker.getPiwik().getPiwikPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
            if (referrerExtras != null) referringApp = referringApp + "/?" + referrerExtras;
        }

        if (referringApp != null) referringApp = "http://" + referringApp;

        mTracker.track(baseTrackMe
                .set(QueryParams.EVENT_CATEGORY, "Application")
                .set(QueryParams.EVENT_ACTION, "downloaded")
                .set(QueryParams.ACTION_NAME, "application/downloaded")
                .set(QueryParams.URL_PATH, "/application/downloaded")
                .set(QueryParams.DOWNLOAD, installIdentifier.toString())
                .set(QueryParams.REFERRER, referringApp)); // Can be null in which case the TrackMe removes the REFERRER parameter.

        Timber.tag(LOGGER_TAG).d("... app download tracked.");
    }
}
