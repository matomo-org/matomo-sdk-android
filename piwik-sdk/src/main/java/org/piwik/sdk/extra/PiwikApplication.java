/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.extra;

import android.app.Application;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;
import org.piwik.sdk.TrackerBuilder;

public abstract class PiwikApplication extends Application {
    private Tracker mPiwikTracker;

    public Piwik getPiwik() {
        return Piwik.getInstance(this);
    }

    /**
     * Gives you an all purpose thread-safe persisted Tracker.
     *
     * @return a shared Tracker
     */
    public synchronized Tracker getTracker() {
        if (mPiwikTracker == null) mPiwikTracker = onCreateTrackerConfig().build(getPiwik());
        return mPiwikTracker;
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
        if (mPiwikTracker != null) mPiwikTracker.dispatch();
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if ((level == TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_COMPLETE) && mPiwikTracker != null) {
            mPiwikTracker.dispatch();
        }
        super.onTrimMemory(level);
    }

}
