/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.demo

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import info.hannes.timber.DebugFormatTree
import org.matomo.sdk.TrackMe
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.DimensionQueue
import org.matomo.sdk.extra.DownloadTracker.Extra
import org.matomo.sdk.extra.MatomoApplication
import org.matomo.sdk.extra.TrackHelper
import timber.log.Timber
import timber.log.Timber.Forest.plant

class DemoApp : MatomoApplication() {
    override fun onCreateTrackerConfig(): TrackerBuilder {
        return TrackerBuilder.createDefault("https://demo2.matomo.org/matomo.php", 81)
    }

    override fun onCreate() {
        super.onCreate()
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyDialog()
                .penaltyLog()
                .build()
        )
        onInitTracker()
    }

    private fun onInitTracker() {
        // Print debug output when working on an app.
        plant(DebugFormatTree())

        // When working on an app we don't want to skew tracking results.
        // getMatomo().setDryRun(BuildConfig.DEBUG);

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        // String userEmail = ....preferences....getString
        // getTracker().setUserId(userEmail);

        // Track this app install, this will only trigger once per app version.
        // i.e. "http://org.matomo.demo:1/185DECB5CFE28FDB2F45887022D668B4"
        TrackHelper.track().download().identifier(Extra.ApkChecksum(this)).with(tracker)
        // Alternative:
        // i.e. "http://org.matomo.demo:1/com.android.vending"
        // getTracker().download();
        val mDimensionQueue = DimensionQueue(tracker)

        // This will be send the next time something is tracked.
        mDimensionQueue.add(0, "test")
        tracker.addTrackingCallback { trackMe: TrackMe? ->
            Timber.i("Tracker.Callback.onTrack(%s)", trackMe)
            trackMe
        }
    }
}