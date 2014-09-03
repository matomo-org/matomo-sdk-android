package com.piwik.demo;

import org.piwik.sdk.PiwikApplication;

public class DemoApp extends PiwikApplication {

    @Override
    public String getTrackerUrl() {
        return "http://beacons.testing.piwik.pro/piwik.php";
    }

    @Override
    public String getAuthToken() {
        return "2f0e4cf3431b6b4ed8614c4649496aaf";
    }

    @Override
    public Integer getSiteId() {
        return 4;
    }

}
