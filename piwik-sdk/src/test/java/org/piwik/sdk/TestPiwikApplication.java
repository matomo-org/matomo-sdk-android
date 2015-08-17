package org.piwik.sdk;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;

import org.robolectric.Robolectric;
import org.robolectric.TestLifecycleApplication;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

public class TestPiwikApplication extends PiwikApplication implements TestLifecycleApplication {

    public static final String INSTALLER_PACKAGENAME = "com.android.vending users can screw with this value !$()=%ÄÖÜ";
    public static final byte[] FAKE_APK_DATA = "this is an apk, awesome right?".getBytes();
    public static final String FAKE_APK_DATA_MD5 = "771BD8971508985852AF8F96170C52FB";
    public static final int VERSION_CODE = 1;
    public static final String PACKAGENAME = "org.piwik.sdk.test";
    private File mFakeApk;

    @Override
    public void onCreate() {
        ShadowLog.stream = System.out;
        // Setup a fake PackageInfo for this app within the packagemanager
        RobolectricPackageManager rpm = (RobolectricPackageManager) Robolectric.application.getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = getPackageName();
        packageInfo.versionCode = VERSION_CODE;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        mFakeApk = new File(Environment.getExternalStorageDirectory(), "base.apk");
        applicationInfo.sourceDir = mFakeApk.getAbsolutePath();
        try {
            FileOutputStream out = new FileOutputStream(applicationInfo.sourceDir);
            out.write(FAKE_APK_DATA);
            out.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        packageInfo.applicationInfo = applicationInfo;
        rpm.addPackage(packageInfo);

        rpm.setInstallerPackageName(getPackageName(), INSTALLER_PACKAGENAME);
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        mFakeApk.delete();
        super.onTerminate();
    }

    @Override
    public void beforeTest(Method method) {

    }

    @Override
    public void prepareTest(Object test) {
    }

    @Override
    public void afterTest(Method method) {

    }

    @Override
    public String getPackageName() {
        return PACKAGENAME;
    }

    @Override
    public String getTrackerUrl() {
        return "http://example.com";
    }

    @Override
    public Integer getSiteId() {
        return 1;
    }
}
