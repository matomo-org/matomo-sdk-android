package org.piwik.sdk;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;

import org.robolectric.Robolectric;
import org.robolectric.TestLifecycleApplication;
import org.robolectric.res.builder.RobolectricPackageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

public class TestPiwikApplication extends PiwikApplication implements TestLifecycleApplication {

    private File mFakeApk;

    @Override
    public void onCreate() {
        // Setup a fake PackageInfo for this app within the packagemanager
        RobolectricPackageManager rpm = (RobolectricPackageManager) Robolectric.application.getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = getPackageName();
        packageInfo.versionCode = 1;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        mFakeApk = new File(Environment.getExternalStorageDirectory(), "base.apk");
        applicationInfo.sourceDir = mFakeApk.getAbsolutePath();
        try {
            FileOutputStream out = new FileOutputStream(applicationInfo.sourceDir);
            byte dataToWrite[] = "somedata".getBytes();
            out.write(dataToWrite);
            out.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        packageInfo.applicationInfo = applicationInfo;
        rpm.addPackage(packageInfo);
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
        return "org.piwik.sdk.test";
    }

}
