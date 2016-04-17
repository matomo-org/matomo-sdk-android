package org.piwik.sdk;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvPackageManager;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.PiwikTestApplication;
import org.piwik.sdk.tools.UrlHelper;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class DownloadTrackerTest extends DefaultTestCase {

    private boolean checkNewAppDownload(QueryHashMap<String, String> queryParams) {
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
        checkNewAppDownload(parseEventUrl(tracker.getLastEvent()));

        tracker.clearLastEvent();

        // track only once
        downloadTracker = new DownloadTracker(tracker);
        downloadTracker.trackOnce(DownloadTracker.Extra.NONE);
        assertNull(tracker.getLastEvent());

    }

    // http://org.piwik.sdk.test:1/some.package or http://org.piwik.sdk.test:1
    private final Pattern REGEX_DOWNLOADTRACK = Pattern.compile("(?:https?:\\/\\/)([\\w.]+)(?::)([\\d]+)(?:(?:\\/)([\\W\\w]+))?");

    @Test
    public void testTrackNewAppDownload() throws Exception {
        Tracker tracker = createTracker();
        DownloadTracker downloadTracker = new DownloadTracker(tracker);
        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.APK_CHECKSUM);
        Thread.sleep(100); // APK checksum happens off thread
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        Matcher m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(PiwikTestApplication.FAKE_APK_DATA_MD5, m.group(3));
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

        tracker.clearLastEvent();

        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.NONE);
        queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        String downloadParams = queryParams.get(QueryParams.DOWNLOAD);
        m = REGEX_DOWNLOADTRACK.matcher(downloadParams);
        assertTrue(downloadParams, m.matches());
        assertEquals(3, m.groupCount());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(null, m.group(3));
        assertEquals("http://" + PiwikTestApplication.INSTALLER_PACKAGENAME, queryParams.get(QueryParams.REFERRER));

        tracker.clearLastEvent();

        FullEnvPackageManager pm = (FullEnvPackageManager) Robolectric.packageManager;
        pm.getInstallerMap().clear(); // The sdk tries to use the installer as referrer, if we clear this, the referrer should be null
        downloadTracker.trackNewAppDownload(DownloadTracker.Extra.NONE);
        queryParams = parseEventUrl(tracker.getLastEvent());
        checkNewAppDownload(queryParams);
        m = REGEX_DOWNLOADTRACK.matcher(queryParams.get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(3, m.groupCount());
        assertEquals(PiwikTestApplication.PACKAGENAME, m.group(1));
        assertEquals(PiwikTestApplication.VERSION_CODE, Integer.parseInt(m.group(2)));
        assertEquals(null, m.group(3));
        assertEquals(null, queryParams.get(QueryParams.REFERRER));
    }

    private static class QueryHashMap<String, V> extends HashMap<String, V> {

        private QueryHashMap() {
            super(10);
        }

        public V get(QueryParams key) {
            return get(key.toString());
        }
    }

    private static QueryHashMap<String, String> parseEventUrl(String url) throws Exception {
        QueryHashMap<String, String> values = new QueryHashMap<>();

        List<Pair<String, String>> params = UrlHelper.parse(new URI("http://localhost/" + url), "UTF-8");

        for (Pair<String, String> param : params)
            values.put(param.first, param.second);

        return values;
    }

    private static void validateDefaultQuery(QueryHashMap<String, String> params) {
        assertEquals(params.get(QueryParams.SITE_ID), "1");
        assertEquals(params.get(QueryParams.RECORD), "1");
        assertEquals(params.get(QueryParams.SEND_IMAGE), "0");
        assertEquals(params.get(QueryParams.VISITOR_ID).length(), 16);
        assertTrue(params.get(QueryParams.URL_PATH).startsWith("http://"));
        assertTrue(Integer.parseInt(params.get(QueryParams.RANDOM_NUMBER)) > 0);
    }
}
