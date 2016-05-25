/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.testhelper;

import android.app.Application;
import android.content.pm.PackageInfo;

import org.robolectric.AndroidManifest;
import org.robolectric.DefaultTestLifecycle;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;

import java.lang.reflect.Method;

/**
 * Tries to emulate a full app environment to satisfy more in-depth tests
 */
public class FullEnvTestLifeCycle extends DefaultTestLifecycle {

    @Override
    public Application createApplication(Method method, AndroidManifest appManifest, Config config) {
        // FIXME If a future version of Robolectric implements "setInstallerPackageName", remove this.
        RobolectricPackageManager oldManager = Robolectric.packageManager;
        RobolectricPackageManager newManager = new FullEnvPackageManager();
        for (PackageInfo pkg : oldManager.getInstalledPackages(0))
            newManager.addPackage(pkg);
        Robolectric.packageManager = newManager;
        return new PiwikTestApplication();
    }
}
