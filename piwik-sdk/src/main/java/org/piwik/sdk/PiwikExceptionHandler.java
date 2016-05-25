/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import timber.log.Timber;

/**
 * An exception handler that wraps the existing exception handler and dispatches event to a {@link org.piwik.sdk.Tracker}.
 * <p/>
 * Also see documentation for {@link TrackHelper#uncaughtExceptions()}
 */
public class PiwikExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Tracker mTracker;
    private final TrackMe mTrackMe;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    public PiwikExceptionHandler(Tracker tracker, TrackMe trackMe) {
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
            TrackHelper.track().exception(ex).description(excInfo).fatal(true).with(getTracker());
            // Immediately dispatch as the app might be dying after rethrowing the exception
            getTracker().dispatch();
        } catch (Exception e) {
            Timber.tag(Tracker.LOGGER_TAG).e(e, "Couldn't track uncaught exception");
        } finally {
            // re-throw critical exception further to the os (important)
            if (getDefaultExceptionHandler() != null && getDefaultExceptionHandler() != this) {
                getDefaultExceptionHandler().uncaughtException(thread, ex);
            }
        }
    }
}
