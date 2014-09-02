package com.piwik.demo;

import org.piwik.sdk.PiwikApplication;

public class DemoApp extends PiwikApplication {

    @Override
    public String getTrackerUrl() {
        return "http://beacons.testing.piwik.pro/piwik.php";
    }

    @Override
    public String getAuthToken() {
        return "1e7996408cbab0c443d71931f08e2fee";
    }

    @Override
    public Integer getSiteId() {
        return 4;
    }

}
