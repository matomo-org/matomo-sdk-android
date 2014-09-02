package com.piwik.demo;

import org.piwik.sdk.PiwikApplication;

public class DemoApp extends PiwikApplication {

    @Override
    public String getTrackerUrl() {
        return "http://example.com/piwik.php";
    }

    @Override
    public String getAuthToken() {
        return "abcdef1234567890";
    }

    @Override
    public Integer getSiteId() {
        return 1;
    }

}
