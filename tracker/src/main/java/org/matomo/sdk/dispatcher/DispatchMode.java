package org.matomo.sdk.dispatcher;

import androidx.annotation.Nullable;


public enum DispatchMode {
    /**
     * Dispatch always (default)
     */
    ALWAYS("always"),
    /**
     * Dispatch only on WIFI
     */
    WIFI_ONLY("wifi_only"),
    /**
     * The dispatcher will assume being offline. This is not persisted and will revert on app restart.
     * Ensures no information is lost when tracking exceptions. See #247
     */
    EXCEPTION("exception");

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
