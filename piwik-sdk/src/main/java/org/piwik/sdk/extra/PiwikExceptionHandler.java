/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.extra;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.Tracker;

import timber.log.Timber;

/**
 * An exception handler that wraps the existing exception handler and dispatches event to a {@link org.piwik.sdk.Tracker}.
 * <p>
 * Also see documentation for {@link TrackHelper#uncaughtExceptions()}
 */
public class PiwikExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "PiwikExceptionHandler";
    private final Tracker mTracker;
    private final TrackMe mTrackMe;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    public PiwikExceptionHandler(@NonNull Tracker tracker, @Nullable TrackMe trackMe) {
        mTracker = tracker;
        mTrackMe = trackMe;
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public Tracker getTracker() {
        return mTracker;
    }

    /**
     * This will give you the previous exception handler that is now wrapped.
     */
    public Thread.UncaughtExceptionHandler getDefaultExceptionHandler() {
        return mDefaultExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            String excInfo = ex.getMessage();
            TrackHelper.track(mTrackMe).exception(ex).description(excInfo).fatal(true).with(getTracker());
            // Immediately dispatch as the app might be dying after rethrowing the exception
            getTracker().dispatch();
        } catch (Exception e) {
            Timber.tag(LOGGER_TAG).e(e, "Couldn't track uncaught exception");
        } finally {
            // re-throw critical exception further to the os (important)
            if (getDefaultExceptionHandler() != null && getDefaultExceptionHandler() != this) {
                getDefaultExceptionHandler().uncaughtException(thread, ex);
            }
        }
    }
}
