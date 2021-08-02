/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package testhelpers;

import org.robolectric.DefaultTestLifecycle;

/**
 * Tries to emulate a full app environment to satisfy more in-depth tests
 */
public class FullEnvTestLifeCycle extends DefaultTestLifecycle {
//    @Override
//    public Application createApplication(Method method, AndroidManifest appManifest, Config config) {
//        // FIXME If a future version of Robolectric implements "setInstallerPackageName", remove this.
//        RobolectricPackageManager oldManager = Robolectric.packageManager;
//        RobolectricPackageManager newManager = new FullEnvPackageManager();
//        for (PackageInfo pkg : oldManager.getInstalledPackages(0))
//            newManager.addPackage(pkg);
//        Robolectric.packageManager = newManager;
//        return new MatomoTestApplication();
//    }
}
