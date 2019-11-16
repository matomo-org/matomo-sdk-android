package org.matomo.sdk.tools;


import androidx.annotation.Nullable;

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
