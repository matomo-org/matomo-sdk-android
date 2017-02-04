package org.piwik.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvPackageManager;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.PiwikTestApplication;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class DownloadTrackerTest extends DefaultTestCase {

    private boolean checkNewAppDownload(QueryHashMap queryParams) {
        assertTrue(queryParams.get(QueryParams.DOWNLOAD).length() > 0);
        assertTrue(queryParams.get(QueryParams.URL_PATH).length() > 0);
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Application");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "downloaded");
        assertEquals(queryParams.get(QueryParams.ACTION_NAME), "application/downloaded");
        validateDefaultQuery(queryParams);
        return true;
    }

    @Test
    public void testTrackAppDownload() throws Exception {
        Tracker tracker = createTracker();
        DownloadTracker downloadTracker = new DownloadTracker(tracker);
        downloadTracker.trackOnce(DownloadTracker.Extra.NONE);
        checkNewAppDownload(new QueryHashMap(tracker.getLastEvent()));

        tracker.clearLastEvent();

        // track only once
        downloadTracker = new DownloadTracker(tracker);
        downloadTracker.trackOnce(DownloadTracker.Extra.NONE);
        assertNull(tracker.getLastEvent());
    }

    @Test
    public void testTrackIdentifier() throws Exception {
        Tracker tracker = createTracker();
        DownloadTracker downloadTracker = new DownloadTracker(tracker);
        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.APK_CHECKSUM);
        Thread.sleep(100); // APK checksum happens off thread
        QueryHashMap queryParams = new QueryHashMap(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        Matcher m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(PiwikTestApplication.FAKE_APK_DATA_MD5, m.group(3));
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

        tracker.clearLastEvent();

        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.NONE);
        queryParams = new QueryHashMap(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        String downloadParams = queryParams.get(QueryParams.DOWNLOAD);
        m = REGEX_DOWNLOADTRACK.matcher(downloadParams);
        assertTrue(downloadParams, m.matches());
        assertEquals(3, m.groupCount());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(null, m.group(3));
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));
    }

    // http://org.piwik.sdk.test:1/some.package or http://org.piwik.sdk.test:1
    private final Pattern REGEX_DOWNLOADTRACK = Pattern.compile("(?:https?:\\/\\/)([\\w.]+)(?::)([\\d]+)(?:(?:\\/)([\\W\\w]+))?");

    @Test
    public void testTrackReferrer() throws Exception {
        Tracker tracker = createTracker();

        DownloadTracker downloadTracker = new DownloadTracker(tracker);
        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.NONE);
        QueryHashMap queryParams = new QueryHashMap(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        String downloadParams = queryParams.get(QueryParams.DOWNLOAD);
        Matcher m = REGEX_DOWNLOADTRACK.matcher(downloadParams);
        assertTrue(downloadParams, m.matches());
        assertEquals(3, m.groupCount());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(null, m.group(3));
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

        tracker.clearLastEvent();

        downloadTracker = new DownloadTracker(tracker);
        FullEnvPackageManager pm = (FullEnvPackageManager) Robolectric.packageManager;
        pm.getInstallerMap().clear(); // The sdk tries to use the installer as referrer, if we clear this, the referrer should be null
        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.NONE);
        queryParams = new QueryHashMap(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(3, m.groupCount());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(null, m.group(3));
        assertEquals(null, queryParams.get(QueryParams.REFERRER));
    }

    @Test
    public void testTrackNewAppDownloadWithVersion() throws Exception {
        Tracker tracker = createTracker();
        DownloadTracker downloadTracker = new DownloadTracker(tracker);
        downloadTracker.setVersion("2");
        downloadTracker.trackOnce(DownloadTracker.Extra.NONE);
        QueryHashMap queryParams = new QueryHashMap(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        Matcher m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals("2", m.group(2));
        assertEquals("2", downloadTracker.getVersion());
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

        tracker.clearLastEvent();
        downloadTracker.trackOnce(DownloadTracker.Extra.NONE);
        assertNull(tracker.getLastEvent());

        downloadTracker.setVersion(null);
        downloadTracker.trackOnce(DownloadTracker.Extra.NONE);
        queryParams = new QueryHashMap(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));
    }


    private static void validateDefaultQuery(QueryHashMap params) {
        assertEquals(params.get(QueryParams.SITE_ID), "1");
        assertEquals(params.get(QueryParams.RECORD), "1");
        assertEquals(params.get(QueryParams.SEND_IMAGE), "0");
        assertEquals(params.get(QueryParams.VISITOR_ID).length(), 16);
        assertTrue(params.get(QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(QueryParams.RANDOM_NUMBER)) > 0);
    }
}
