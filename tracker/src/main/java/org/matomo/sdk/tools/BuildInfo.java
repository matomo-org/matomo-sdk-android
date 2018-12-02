package org.matomo.sdk.tools;


import android.os.Build;

public class BuildInfo {
    public String getRelease() {
        return Build.VERSION.RELEASE;
    }

    public String getModel() {
        return Build.MODEL;
    }

    public String getBuildId() {
        return Build.ID;
    }
}
