/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.extra;

import android.app.Application;

import org.matomo.sdk.Matomo;
import org.matomo.sdk.Tracker;
import org.matomo.sdk.TrackerBuilder;

public abstract class MatomoApplication extends Application {
    private Tracker mMatomoTracker;

    public Matomo getMatomo() {
        return Matomo.getInstance(this);
    }

    /**
     * Gives you an all purpose thread-safe persisted Tracker.
     *
     * @return a shared Tracker
     */
    public synchronized Tracker getTracker() {
        if (mMatomoTracker == null) mMatomoTracker = onCreateTrackerConfig().build(getMatomo());
        return mMatomoTracker;
    }

    /**
     * See {@link TrackerBuilder}.
     * You may be interested in {@link TrackerBuilder#createDefault(String, int)}
     *
     * @return the tracker configuration you want to use.
     */
    public abstract TrackerBuilder onCreateTrackerConfig();

    @Override
    public void onLowMemory() {
        if (mMatomoTracker != null) mMatomoTracker.dispatch();
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if ((level == TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_COMPLETE) && mMatomoTracker != null) {
            mMatomoTracker.dispatch();
        }
        super.onTrimMemory(level);
    }

}
