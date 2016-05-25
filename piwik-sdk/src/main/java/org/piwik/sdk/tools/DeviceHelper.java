/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.piwik.sdk.Piwik;

import java.lang.reflect.Method;
import java.util.Locale;

import timber.log.Timber;

/**
 * Helper class to gain information about the device we are running on
 */
public class DeviceHelper {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "DeviceHelper";

    /**
     * Returns user language
     *
     * @return language
     */
    public static String getUserLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * Returns user country
     *
     * @return country
     */
    public static String getUserCountry() {
        return Locale.getDefault().getCountry();
    }

    /**
     * Returns android system user agent
     *
     * @return well formatted user agent
     */
    public static String getUserAgent() {
        return System.getProperty("http.agent");
    }

    /**
     * Tries to get the most accurate device resolution.
     * On devices below API17 resolution might not account for statusbar/softkeys.
     *
     * @param context your application context
     * @return [width, height]
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int[] getResolution(Context context) {
        int width = -1, height = -1;

        Display display;
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            display = wm.getDefaultDisplay();
        } catch (NullPointerException e) {
            Timber.tag(LOGGER_TAG).e(e, "Window service was not available from this context");
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Recommended way to get the resolution but only available since API17
            DisplayMetrics dm = new DisplayMetrics();
            display.getRealMetrics(dm);
            width = dm.widthPixels;
            height = dm.heightPixels;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Reflection bad, still this is the best way to get an accurate screen size on API14-16.
            try {
                Method getRawWidth = Display.class.getMethod("getRawWidth");
                Method getRawHeight = Display.class.getMethod("getRawHeight");
                width = (int) getRawWidth.invoke(display);
                height = (int) getRawHeight.invoke(display);
            } catch (Exception e) {
                Timber.tag(LOGGER_TAG).w(e, "Reflection of getRawWidth/getRawHeight failed on API14-16 unexpectedly.");
            }
        }

        if (width == -1 || height == -1) {
            // This is not accurate on all 4.2+ devices, usually the height is wrong due to statusbar/softkeys
            // Better than nothing though.
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            width = dm.widthPixels;
            height = dm.heightPixels;
        }

        return new int[]{width, height};
    }
}
