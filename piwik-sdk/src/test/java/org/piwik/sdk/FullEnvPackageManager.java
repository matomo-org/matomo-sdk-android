/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.res.builder.RobolectricPackageManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Because we need to fake things that RobolectricPackageManager does not offer.
 * Currently:<p/>
 * {@link org.robolectric.res.builder.RobolectricPackageManager#setInstallerPackageName(String, String)}
 */
public class FullEnvPackageManager extends RobolectricPackageManager {
    private final HashMap<String, String> mInstallerPackageNames = new HashMap<>();

    @Override
    public Intent getLeanbackLaunchIntentForPackage(String packageName) {
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        return null;
    }

    @Override
    public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
        return null;
    }

    @Override
    public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
        return null;
    }

    @Override
    public Drawable getApplicationBanner(ApplicationInfo info) {
        return null;
    }

    @Override
    public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
        return null;
    }

    @Override
    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return null;
    }

    @Override
    public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
        return null;
    }

    @Override
    public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        return null;
    }

    @Implementation
    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        mInstallerPackageNames.put(targetPackage, installerPackageName);
    }

    @Implementation
    public String getInstallerPackageName(String packageName) {
        return mInstallerPackageNames.get(packageName);
    }

    @NonNull
    @Override
    public PackageInstaller getPackageInstaller() {
        return null;
    }

    public Map<String, String> getInstallerMap() {
        return mInstallerPackageNames;
    }
}
