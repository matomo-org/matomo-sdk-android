package org.matomo.sdk;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Configuration details for a {@link Tracker}
 */
public class TrackerBuilder {
    private final String mApiUrl;
    private final int mSiteId;
    private String mTrackerName;
    private String mApplicationBaseUrl;

    public static TrackerBuilder createDefault(String apiUrl, int siteId) {
        return new TrackerBuilder(apiUrl, siteId, "Default Tracker");
    }

    /**
     * @param apiUrl      Tracking HTTP API endpoint, for example, https://matomo.yourdomain.tld/matomo.php
     * @param siteId      id of your site in the backend
     * @param trackerName name of your tracker, will be used to store configuration data
     */
    public TrackerBuilder(String apiUrl, int siteId, String trackerName) {
        try {
            new URL(apiUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        mApiUrl = apiUrl;
        mSiteId = siteId;
        mTrackerName = trackerName;
    }

    public String getApiUrl() {
        return mApiUrl;
    }

    public int getSiteId() {
        return mSiteId;
    }

    /**
     * A unique name for this Tracker. Used to store Tracker settings independent of URL and id changes.
     */
    public TrackerBuilder setTrackerName(String name) {
        mTrackerName = name;
        return this;
    }

    public String getTrackerName() {
        return mTrackerName;
    }

    /**
     * Domain used to build the required parameter url (http://developer.matomo.org/api-reference/tracking-api)
     * Defaults to`https://your.packagename`
     *
     * @param domain your-domain.com
     */
    public TrackerBuilder setApplicationBaseUrl(String domain) {
        mApplicationBaseUrl = domain;
        return this;
    }

    public String getApplicationBaseUrl() {
        return mApplicationBaseUrl;
    }

    public Tracker build(Matomo matomo) {
        if (mApplicationBaseUrl == null) {
            mApplicationBaseUrl = String.format("https://%s/", matomo.getContext().getPackageName());
        }
        return new Tracker(matomo, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackerBuilder that = (TrackerBuilder) o;

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
