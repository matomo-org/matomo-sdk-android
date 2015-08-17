/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Methods to do tracking easier/quicker
 */
public class QuickTrack {

    /**
     * This will create an exception handler that wraps any existing exception handler.
     * Exceptions will be caught, tracked, dispatched and then rethrown to the previous exception handler.
     * <p/>
     * Be wary of relying on this for complete crash tracking..
     * Think about how to deal with older app versions still throwing already fixed exceptions.
     * <p/>
     * See discussion here: https://github.com/piwik/piwik-sdk-android/issues/28
     *
     * @param tracker the tracker that should receive the exception events.
     * @return returns the new (but already active) exception handler.
     */
    public static Thread.UncaughtExceptionHandler trackUncaughtExceptions(Tracker tracker) {
        if (Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler) {
            throw new RuntimeException("Trying to wrap an existing PiwikExceptionHandler.");
        }
        Thread.UncaughtExceptionHandler handler = new PiwikExceptionHandler(tracker);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        return handler;
    }

    /**
     * This method will bind a tracker to your application,
     * causing it to automatically track Activities within your app.
     *
     * @param app     your app
     * @param tracker the tracker to use
     * @return the registered callback, you need this if you wanted to unregister the callback again
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Application.ActivityLifecycleCallbacks bindToApp(final Application app, final Tracker tracker) {
        final Application.ActivityLifecycleCallbacks callback = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                track(tracker, activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (activity != null && activity.isTaskRoot()) {
                    tracker.dispatch();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        };
        app.registerActivityLifecycleCallbacks(callback);
        return callback;
    }

    /**
     * Calls {@link Tracker#trackScreenView(String, String)} for an activity.
     * Uses the activity-stack as path and activity title as names.
     *
     * @param piwikApplication
     * @param activity
     */
    public static void track(PiwikApplication piwikApplication, Activity activity) {
        track(piwikApplication.getTracker(), activity);
    }

    /**
     * Calls {@link Tracker#trackScreenView(String, String)} for an activity.
     * Uses the activity-stack as path and activity title as names.
     *
     * @param tracker
     * @param activity
     */
    public static void track(Tracker tracker, Activity activity) {
        if (activity != null) {
            String breadcrumbs = getBreadcrumbs(activity);
            tracker.trackScreenView(breadcrumbsToPath(breadcrumbs), breadcrumbs);
        }
    }

    private static String getBreadcrumbs(final Activity activity) {
        Activity currentActivity = activity;
        ArrayList<String> breadcrumbs = new ArrayList<>();

        while (currentActivity != null) {
            breadcrumbs.add(currentActivity.getTitle().toString());
            currentActivity = currentActivity.getParent();
        }
        return joinSlash(breadcrumbs);
    }

    private static String joinSlash(List<String> sequence) {
        if (sequence != null && sequence.size() > 0) {
            return TextUtils.join("/", sequence);
        }
        return "";
    }

    private static String breadcrumbsToPath(String breadcrumbs) {
        return breadcrumbs.replaceAll("\\s", "");
    }

}
