package org.piwik.sdk.tools;


import android.support.annotation.Nullable;

public class PropertySource {
    @Nullable
    public String getHttpAgent() {
        return getSystemProperty("http.agent");
    }

    @Nullable
    public String getJVMVersion() {
        return getSystemProperty("java.vm.version");
    }

    @Nullable
    public String getSystemProperty(String key) {
        return System.getProperty(key);
    }
}
