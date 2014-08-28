package org.piwik.sdk;

import android.app.Application;

import java.net.MalformedURLException;

public abstract class PiwikApplication extends Application{
    Tracker piwikTracker;

    public synchronized Tracker getTracker(){
         if (piwikTracker != null) {
            return piwikTracker;
         }

         Piwik analytics = Piwik.getInstance(this);

        try {
            piwikTracker = analytics.newTracker(getTrackerUrl(), getSiteId(), getAuthToken());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        return piwikTracker;

    }

    public String getTrackerUrl(){
        return "";
    }

    public String getAuthToken(){
        return "";
    }

    public Integer getSiteId(){
        return 1;
    }

}
