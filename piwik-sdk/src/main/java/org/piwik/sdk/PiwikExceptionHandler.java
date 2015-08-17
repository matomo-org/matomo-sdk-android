/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import org.piwik.sdk.tools.Logy;

/**
 * An exception handler that wraps the existing exception handler and dispatches event to a {@link org.piwik.sdk.Tracker}.
 * <p/>
 * Also see documentation for {@link org.piwik.sdk.QuickTrack#trackUncaughtExceptions(Tracker)}
 */
public class PiwikExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Tracker mTracker;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    public PiwikExceptionHandler(Tracker tracker) {
        mTracker = tracker;
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public Tracker getTracker() {
        return mTracker;
    }

    /**
     * This will give you the previous exception handler that is now wrapped.
     *
     * @return
     */
    public Thread.UncaughtExceptionHandler getDefaultExceptionHandler() {
        return mDefaultExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            String excInfo = ex.getMessage();
            getTracker().trackException(ex, excInfo, true);
            // Immediately dispatch as the app might be dying after rethrowing the exception
            getTracker().dispatch();
        } catch (Exception e) {
            Logy.e(Tracker.LOGGER_TAG, "Couldn't track uncaught exception", e);
        } finally {
            // re-throw critical exception further to the os (important)
            if (getDefaultExceptionHandler() != null && getDefaultExceptionHandler() != this) {
                getDefaultExceptionHandler().uncaughtException(thread, ex);
            }
        }
    }
}
