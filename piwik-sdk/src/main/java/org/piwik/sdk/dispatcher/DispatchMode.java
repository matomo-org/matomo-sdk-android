package org.piwik.sdk.dispatcher;

import android.support.annotation.Nullable;


public enum DispatchMode {
    /**
     * Dispatch always (default)
     */
    ALWAYS("always"),
    /**
     * Dispatch only on WIFI
     */
    WIFI_ONLY("wifi_only");

    private final String key;

    DispatchMode(String key) {this.key = key;}

    @Override
    public String toString() {
        return key;
    }

    @Nullable
    public static DispatchMode fromString(String raw) {
        for (DispatchMode mode : DispatchMode.values()) {
            if (mode.key.equals(raw)) return mode;
        }
        return null;
    }
}
