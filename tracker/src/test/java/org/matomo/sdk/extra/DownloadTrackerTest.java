package org.matomo.sdk.extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.Matomo;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.Tracker;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import testhelpers.BaseTest;
import testhelpers.TestHelper;
import testhelpers.TestPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadTrackerTest extends BaseTest {
    @Mock Tracker mTracker;
    @Mock Matomo mMatomo;
    @Mock Context mContext;
    @Mock PackageManager mPackageManager;
    ArgumentCaptor<TrackMe> mCaptor = ArgumentCaptor.forClass(TrackMe.class);
    SharedPreferences mSharedPreferences = new TestPreferences();
    private PackageInfo mPackageInfo;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        when(mTracker.getPreferences()).thenReturn(mSharedPreferences);
        when(mTracker.getMatomo()).thenReturn(mMatomo);
        when(mMatomo.getContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("package");

        mPackageInfo = new PackageInfo();
        mPackageInfo.versionCode = 123;
        mPackageInfo.packageName = "package";
        //noinspection WrongConstant
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(mPackageInfo);
        when(mPackageManager.getInstallerPackageName("package")).thenReturn("installer");
    }

    @Test
    public void testTrackAppDownload() {
        DownloadTracker downloadTracker = new DownloadTracker(mTracker);
        downloadTracker.trackOnce(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());

        // track only once
        downloadTracker.trackOnce(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker, times(1)).track(mCaptor.capture());
    }

    @Test
    public void testTrackIdentifier() {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        mPackageInfo.applicationInfo = applicationInfo;
        applicationInfo.sourceDir = UUID.randomUUID().toString();
        final byte[] FAKE_APK_DATA = "this is an apk, awesome right?".getBytes();
        final String FAKE_APK_DATA_MD5 = "771BD8971508985852AF8F96170C52FB";

        try {
            FileOutputStream out = new FileOutputStream(applicationInfo.sourceDir);
            out.write(FAKE_APK_DATA);
            out.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        DownloadTracker downloadTracker = new DownloadTracker(mTracker);
        downloadTracker.trackNewAppDownload(new TrackMe(), new DownloadTracker.Extra.ApkChecksum(mContext));
        TestHelper.sleep(100); // APK checksum happens off thread
        verify(mTracker).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());
        Matcher m = REGEX_DOWNLOADTRACK.matcher(mCaptor.getValue().get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals("package", m.group(1));
        assertEquals(123, Integer.parseInt(m.group(2)));
        assertEquals(FAKE_APK_DATA_MD5, m.group(3));
        assertEquals("http://installer", mCaptor.getValue().get(QueryParams.REFERRER));

        downloadTracker.trackNewAppDownload(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker, times(2)).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());
        String downloadParams = mCaptor.getValue().get(QueryParams.DOWNLOAD);
        m = REGEX_DOWNLOADTRACK.matcher(downloadParams);
        assertTrue(downloadParams, m.matches());
        assertEquals(3, m.groupCount());
        assertEquals("package", m.group(1));
        assertEquals(123, Integer.parseInt(m.group(2)));
        assertNull(m.group(3));
        assertEquals("http://installer", mCaptor.getValue().get(QueryParams.REFERRER));
        //noinspection ResultOfMethodCallIgnored
        new File(applicationInfo.sourceDir).delete();
    }

    // http://org.matomo.sdk.test:1/some.package or http://org.matomo.sdk.test:1
    private final Pattern REGEX_DOWNLOADTRACK = Pattern.compile("https?://([\\w.]+):([\\d]+)(?:/([\\W\\w]+))?");

    @Test
    public void testTrackReferrer() {
        DownloadTracker downloadTracker = new DownloadTracker(mTracker);
        downloadTracker.trackNewAppDownload(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());
        String downloadParams = mCaptor.getValue().get(QueryParams.DOWNLOAD);
        Matcher m = REGEX_DOWNLOADTRACK.matcher(downloadParams);
        assertTrue(downloadParams, m.matches());
        assertEquals(3, m.groupCount());
        assertEquals("package", m.group(1));
        assertEquals(123, Integer.parseInt(m.group(2)));
        assertNull(m.group(3));
        assertEquals("http://installer", mCaptor.getValue().get(QueryParams.REFERRER));

        when(mPackageManager.getInstallerPackageName(anyString())).thenReturn(null);
        downloadTracker.trackNewAppDownload(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker, times(2)).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());
        m = REGEX_DOWNLOADTRACK.matcher(mCaptor.getValue().get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals(3, m.groupCount());
        assertEquals("package", m.group(1));
        assertEquals(123, Integer.parseInt(m.group(2)));
        assertNull(m.group(3));
        assertNull(mCaptor.getValue().get(QueryParams.REFERRER));
    }

    @Test
    public void testTrackNewAppDownloadWithVersion() {
        DownloadTracker downloadTracker = new DownloadTracker(mTracker);
        downloadTracker.setVersion("2");
        downloadTracker.trackOnce(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());
        Matcher m = REGEX_DOWNLOADTRACK.matcher(mCaptor.getValue().get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals("package", m.group(1));
        assertEquals("2", m.group(2));
        assertEquals("2", downloadTracker.getVersion());
        assertEquals("http://installer", mCaptor.getValue().get(QueryParams.REFERRER));

        downloadTracker.trackOnce(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker, times(1)).track(mCaptor.capture());

        downloadTracker.setVersion(null);
        downloadTracker.trackOnce(new TrackMe(), new DownloadTracker.Extra.None());
        verify(mTracker, times(2)).track(mCaptor.capture());
        checkNewAppDownload(mCaptor.getValue());
        m = REGEX_DOWNLOADTRACK.matcher(mCaptor.getValue().get(QueryParams.DOWNLOAD));
        assertTrue(m.matches());
        assertEquals("package", m.group(1));
        assertEquals(123, Integer.parseInt(m.group(2)));
        assertEquals("http://installer", mCaptor.getValue().get(QueryParams.REFERRER));
    }

    private void checkNewAppDownload(TrackMe trackMe) {
        assertTrue(trackMe.get(QueryParams.DOWNLOAD).length() > 0);
        assertTrue(trackMe.get(QueryParams.URL_PATH).length() > 0);
        assertEquals(trackMe.get(QueryParams.EVENT_CATEGORY), "Application");
        assertEquals(trackMe.get(QueryParams.EVENT_ACTION), "downloaded");
        assertEquals(trackMe.get(QueryParams.ACTION_NAME), "application/downloaded");
    }
}
