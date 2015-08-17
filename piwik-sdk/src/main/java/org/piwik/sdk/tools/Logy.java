/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.tools;

import android.util.Log;

import org.piwik.sdk.BuildConfig;

/**
 * Wrapper class for {@link android.util.Log} that allows to fine tune what gets logged.
 */
public class Logy {
    public static final int SILENT = -2;
    public static final int QUIET = -1;
    public static final int NORMAL = 0;
    public static final int DEBUG = 1;
    public static final int VERBOSE = 2;

    public static int sLoglevel = BuildConfig.DEBUG ? VERBOSE : QUIET;

    public static void v(String c, String s) {
        if (sLoglevel >= VERBOSE) {
            Log.v(c, s);
        }
    }

    public static void d(String c, String s) {
        if (sLoglevel >= DEBUG) {
            Log.d(c, s);
        }
    }

    public static void i(String c, String s) {
        if (sLoglevel >= NORMAL) {
            Log.i(c, s);
        }
    }

    public static void w(String c, String s) {
        if (sLoglevel > QUIET) {
            Log.w(c, s);
        }
    }

    public static void w(String c, String s, Throwable tr) {
        if (sLoglevel > QUIET) {
            Log.w(c, s, tr);
        }
    }

    public static void e(String c, String s) {
        if (sLoglevel != SILENT) {
            Log.e(c, s);
        }
    }

    public static void e(String c, String s, Throwable tr) {
        if (sLoglevel != SILENT) {
            Log.e(c, s, tr);
        }
    }

}
