package org.piwik.sdk;

import android.app.Application;
import android.util.Log;

import java.net.MalformedURLException;

public abstract class PiwikApplication extends Application {
    Tracker piwikTracker;

    public synchronized Tracker getTracker() {
        if (piwikTracker != null) {
            return piwikTracker;
        }

        Piwik analytics = Piwik.getInstance(this);

        try {
            piwikTracker = analytics.newTracker(getTrackerUrl(), getSiteId(), getAuthToken());
        } catch (MalformedURLException e) {
            Log.i(Tracker.LOGGER_TAG, getTrackerUrl());
            Log.w(Tracker.LOGGER_TAG, "url is malformed", e);
            return null;
        }

        return piwikTracker;

    }

    public String getTrackerUrl() {
        return "";
    }

    public String getAuthToken() {
        return "";
    }

    public Integer getSiteId() {
        return 1;
    }

}
