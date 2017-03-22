package org.piwik.sdk;

import android.support.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Configuration details for a {@link Tracker}
 */
public class TrackerConfig {
    private final URL mApiUrl;
    private final int mSiteId;
    private final String mTrackerName;

    /**
     * Creates a configuration for the default tracker, implicitly setting the tracker name to "Default Tracker".
     */
    public static TrackerConfig createDefault(@NonNull String apiUrl, int siteId) {
        return new TrackerConfig(apiUrl, siteId, "Default Tracker");
    }

    /**
     * Use Piwik.newTracker() method to create new trackers
     *
     * @param apiUrl      Tracking HTTP API endpoint, for example, https://piwik.yourdomain.tld
     * @param siteId      id of site
     * @param trackerName unique name for this Tracker. Used to store Tracker settings independent of URL and id changes.
     * @throws RuntimeException if the supplied Piwik-Tracker URL is incompatible
     */
    public TrackerConfig(@NonNull String apiUrl, int siteId, String trackerName) {
        try {
            if (apiUrl.endsWith("piwik.php") || apiUrl.endsWith("piwik-proxy.php")) {
                mApiUrl = new URL(apiUrl);
            } else {
                if (!apiUrl.endsWith("/")) apiUrl += "/";
                mApiUrl = new URL(apiUrl + "piwik.php");
            }
        } catch (MalformedURLException e) { throw new RuntimeException(e); }
        mSiteId = siteId;
        mTrackerName = trackerName;
    }

    public URL getApiUrl() {
        return mApiUrl;
    }

    public int getSiteId() {
        return mSiteId;
    }

    public String getTrackerName() {
        return mTrackerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackerConfig that = (TrackerConfig) o;

        return mSiteId == that.mSiteId && mApiUrl.equals(that.mApiUrl) && mTrackerName.equals(that.mTrackerName);
    }

    @Override
    public int hashCode() {
        int result = mApiUrl.hashCode();
        result = 31 * result + mSiteId;
        result = 31 * result + mTrackerName.hashCode();
        return result;
    }
}
