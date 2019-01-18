/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.demo;

import android.os.StrictMode;

import org.matomo.sdk.TrackerBuilder;
import org.matomo.sdk.extra.DimensionQueue;
import org.matomo.sdk.extra.DownloadTracker;
import org.matomo.sdk.extra.MatomoApplication;
import org.matomo.sdk.extra.TrackHelper;

import timber.log.Timber;

public class DemoApp extends MatomoApplication {
    private DimensionQueue mDimensionQueue;

    @Override
    public TrackerBuilder onCreateTrackerConfig() {
        return TrackerBuilder.createDefault("https://demo.matomo.org/matomo.php", 53);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        onInitTracker();
    }


    private void onInitTracker() {
        // Print debug output when working on an app.
        Timber.plant(new Timber.DebugTree());

        // When working on an app we don't want to skew tracking results.
        // getMatomo().setDryRun(BuildConfig.DEBUG);

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        // String userEmail = ....preferences....getString
        // getTracker().setUserId(userEmail);

        // Track this app install, this will only trigger once per app version.
        // i.e. "http://org.matomo.demo:1/185DECB5CFE28FDB2F45887022D668B4"
        TrackHelper.track().download().identifier(new DownloadTracker.Extra.ApkChecksum(this)).with(getTracker());
        // Alternative:
        // i.e. "http://org.matomo.demo:1/com.android.vending"
        // getTracker().download();

        mDimensionQueue = new DimensionQueue(getTracker());

        // This will be send the next time something is tracked.
        mDimensionQueue.add(0, "test");

        getTracker().addTrackingCallback(trackMe -> {
            Timber.i("Tracker.Callback.onTrack(%s)", trackMe);
            return trackMe;
        });
    }
}
