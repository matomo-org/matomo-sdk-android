/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.tools;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.matomo.sdk.Matomo;

import java.util.Locale;

import timber.log.Timber;

/**
 * Helper class to gain information about the device we are running on
 */
public class DeviceHelper {
    private static final String TAG = Matomo.tag(DeviceHelper.class);
    private final Context mContext;
    private final PropertySource mPropertySource;
    private final BuildInfo mBuildInfo;

    public DeviceHelper(Context context, PropertySource propertySource, BuildInfo buildInfo) {
        mContext = context;
        mPropertySource = propertySource;
        mBuildInfo = buildInfo;
    }

    /**
     * Returns user language
     *
     * @return language
     */
    public String getUserLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * Returns android system user agent
     *
     * @return well formatted user agent
     */
    public String getUserAgent() {
        String httpAgent = mPropertySource.getHttpAgent();
        if (httpAgent == null || httpAgent.startsWith("Apache-HttpClient/UNAVAILABLE (java")) {
            String dalvik = mPropertySource.getJVMVersion();
            if (dalvik == null) dalvik = "0.0.0";
            String android = mBuildInfo.getRelease();
            String model = mBuildInfo.getModel();
            String build = mBuildInfo.getBuildId();
            httpAgent = String.format(Locale.US,
                    "Dalvik/%s (Linux; U; Android %s; %s Build/%s)",
                    dalvik, android, model, build
            );
        }
        return httpAgent;
    }

    /**
     * Tries to get the most accurate device resolution.
     * On devices below API17 resolution might not account for statusbar/softkeys.
     *
     * @return [width, height]
     */
    public int[] getResolution() {
        int width, height;

        Display display;
        try {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            display = wm.getDefaultDisplay();
        } catch (NullPointerException e) {
            Timber.tag(TAG).e(e, "Window service was not available from this context");
            return null;
        }

        // Recommended way to get the resolution but only available since API17
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

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
