package org.piwik.sdk;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TrackerConfigTest {

    @Test
    public void testURL() throws MalformedURLException {
        TrackerConfig trackerConfig = new TrackerConfig("http://example.com", 1337, "Test Name");
        assertThat(trackerConfig.getApiUrl(), is(new URL("http://example.com/piwik.php")));

        trackerConfig = new TrackerConfig("http://test/piwik.php", 1, "Default Tracker");
        assertThat(trackerConfig.getApiUrl().toString(), is("http://test/piwik.php"));

        trackerConfig = new TrackerConfig("http://test/piwik-proxy.php", 1, "Default Tracker");
        assertThat(trackerConfig.getApiUrl().toString(), is("http://test/piwik-proxy.php"));

        trackerConfig = new TrackerConfig("https://demo.org/piwik/piwik.php", 1, "Default Tracker");
        assertThat(trackerConfig.getApiUrl().toString(), is("https://demo.org/piwik/piwik.php"));

        trackerConfig = new TrackerConfig("https://demo.org/piwik/", 1, "Default Tracker");
        assertThat(trackerConfig.getApiUrl().toString(), is("https://demo.org/piwik/piwik.php"));

        trackerConfig = new TrackerConfig("https://demo.org/piwik", 1, "Default Tracker");
        assertThat(trackerConfig.getApiUrl().toString(), is("https://demo.org/piwik/piwik.php"));

        trackerConfig = new TrackerConfig("http://demo.org/test/piwik-proxy.php", 1, "Default Tracker");
        assertThat(trackerConfig.getApiUrl().toString(), is("http://demo.org/test/piwik-proxy.php"));
    }

    @Test
    public void testSiteId() {
        TrackerConfig trackerConfig = new TrackerConfig("http://example.com", 1337, "Test Name");
        assertThat(trackerConfig.getSiteId(), is(1337));
    }


    @Test
    public void testGetName() {
        TrackerConfig trackerConfig = new TrackerConfig("http://example.com", 1337, "Test Name");
        assertThat(trackerConfig.getTrackerName(), is("Test Name"));
    }


    @Test
    public void testEquals() throws MalformedURLException {
        TrackerConfig trackerConfig1 = new TrackerConfig("http://example.com", 1337, "Test Name");
        TrackerConfig trackerConfig2 = new TrackerConfig("http://example.com", 1337, "Test Name");
        assertThat(trackerConfig1, is(trackerConfig2));
    }

    @Test
    public void testHashCode() throws MalformedURLException {
        TrackerConfig trackerConfig = new TrackerConfig("http://example.com", 1337, "Test Name");
        int result = new URL("http://example.com/piwik.php").hashCode();
        result = 31 * result + 1337;
        result = 31 * result + "Test Name".hashCode();
        assertThat(result, is(trackerConfig.hashCode()));
    }
}
