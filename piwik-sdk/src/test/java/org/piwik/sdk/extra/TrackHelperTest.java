package org.piwik.sdk.extra;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.Tracker;

import java.net.URL;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.piwik.sdk.extra.TrackHelper.track;


public class TrackHelperTest {
    ArgumentCaptor<TrackMe> mCaptor = ArgumentCaptor.forClass(TrackMe.class);
    @Mock Tracker mTracker;
    @Mock Piwik mPiwik;
    @Mock Context mContext;
    @Mock PackageManager mPackageManager;
    @Mock PiwikApplication mPiwikApplication;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        when(mTracker.getPiwik()).thenReturn(mPiwik);
        when(mPiwik.getContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("packageName");
        when(mPiwikApplication.getTracker()).thenReturn(mTracker);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = 123;
        //noinspection WrongConstant
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
    }

    @Test
    public void testBaseEvent() {
        track().screen("/path").with(mPiwikApplication);
        verify(mPiwikApplication).getTracker();
        verify(mTracker).track(any(TrackMe.class));
    }

    @Test
    public void testOutlink() throws Exception {
        track().outlink(new URL("file://mount/sdcard/something")).with(mTracker);
        verify(mTracker, never()).track(mCaptor.capture());

        URL valid = new URL("https://foo.bar");
        track().outlink(valid).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertEquals(valid.toExternalForm(), mCaptor.getValue().get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), mCaptor.getValue().get(QueryParams.URL_PATH));

        valid = new URL("https://foo.bar");
        track().outlink(valid).with(mTracker);
        verify(mTracker, times(2)).track(mCaptor.capture());
        assertEquals(valid.toExternalForm(), mCaptor.getValue().get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), mCaptor.getValue().get(QueryParams.URL_PATH));

        valid = new URL("ftp://foo.bar");
        track().outlink(valid).with(mTracker);
        verify(mTracker, times(3)).track(mCaptor.capture());
        assertEquals(valid.toExternalForm(), mCaptor.getValue().get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testDownloadTrackChecksum() throws Exception {
        DownloadTracker downloadTracker = mock(DownloadTracker.class);
        track().download(downloadTracker).identifier(DownloadTracker.Extra.APK_CHECKSUM).with(mTracker);
        verify(downloadTracker).trackOnce(any(TrackMe.class), eq(DownloadTracker.Extra.APK_CHECKSUM));
    }

    @Test
    public void testDownloadTrackForced() throws Exception {
        DownloadTracker downloadTracker = mock(DownloadTracker.class);
        track().download(downloadTracker).force().with(mTracker);
        verify(downloadTracker).trackNewAppDownload(any(TrackMe.class), eq(DownloadTracker.Extra.NONE));
    }

    @Test
    public void testDownloadCustomVersion() throws Exception {
        DownloadTracker downloadTracker = mock(DownloadTracker.class);
        String version = UUID.randomUUID().toString();

        track().download(downloadTracker).version(version).with(mTracker);
        verify(downloadTracker).setVersion(version);
        verify(downloadTracker).trackOnce(any(TrackMe.class), any(DownloadTracker.Extra.class));
    }

    @Test
    public void testSetScreenCustomVariable() throws Exception {
        track()
                .screen("")
                .variable(1, "2", "3")
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals("{'1':['2','3']}".replaceAll("'", "\""), mCaptor.getValue().get(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES));
    }

    @Test
    public void testTrackScreenView() throws Exception {
        track().screen("/test/test").title("title").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertTrue(mCaptor.getValue().get(QueryParams.URL_PATH).endsWith("/test/test"));
    }

    @Test
    public void testTrackScreenWithTitleView() throws Exception {
        track().screen("/test/test").title("Test title").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertTrue(mCaptor.getValue().get(QueryParams.URL_PATH).endsWith("/test/test"));
        assertEquals(mCaptor.getValue().get(QueryParams.ACTION_NAME), "Test title");
    }

    @Test
    public void testTrackEvent() throws Exception {
        track().event("category", "test action").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(tracked.get(QueryParams.EVENT_ACTION), "test action");
    }

    @Test
    public void testTrackEventName() throws Exception {
        String name = "test name2";
        track().event("category", "test action").name(name).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(tracked.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(tracked.get(QueryParams.EVENT_NAME), name);
    }

    @Test
    public void testTrackEventNameAndValue() throws Exception {
        String name = "test name3";
        track().event("category", "test action").name(name).value(1f).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(tracked.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(tracked.get(QueryParams.EVENT_NAME), name);
        assertEquals(String.valueOf(tracked.get(QueryParams.EVENT_VALUE)), String.valueOf(1f));
    }

    @Test
    public void testTrackEventNameAndValueWithpath() throws Exception {
        track().event("category", "test action").name("test name3").path("/path").value(1f).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(tracked.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(tracked.get(QueryParams.EVENT_NAME), "test name3");
        assertEquals(tracked.get(QueryParams.URL_PATH), "/path");
        assertEquals(String.valueOf(tracked.get(QueryParams.EVENT_VALUE)), String.valueOf(1f));
    }

    @Test
    public void testTrackGoal() throws Exception {
        track().goal(1).with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertNull(mCaptor.getValue().get(QueryParams.REVENUE));
        assertEquals(mCaptor.getValue().get(QueryParams.GOAL_ID), "1");
    }

    @Test
    public void testTrackSiteSearch() throws Exception {
        track().search("keyword").category("category").count(1337).with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals(mCaptor.getValue().get(QueryParams.SEARCH_KEYWORD), "keyword");
        assertEquals(mCaptor.getValue().get(QueryParams.SEARCH_CATEGORY), "category");
        assertEquals(mCaptor.getValue().get(QueryParams.SEARCH_NUMBER_OF_HITS), String.valueOf(1337));

        track().search("keyword2").with(mTracker);
        verify(mTracker, times(2)).track(mCaptor.capture());

        assertEquals(mCaptor.getValue().get(QueryParams.SEARCH_KEYWORD), "keyword2");
        assertNull(mCaptor.getValue().get(QueryParams.SEARCH_CATEGORY));
        assertNull(mCaptor.getValue().get(QueryParams.SEARCH_NUMBER_OF_HITS));
    }

    @Test
    public void testTrackGoalRevenue() throws Exception {
        track().goal(1).revenue(100f).with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals("1", mCaptor.getValue().get(QueryParams.GOAL_ID));
        assertTrue(100f == Float.valueOf(mCaptor.getValue().get(QueryParams.REVENUE)));
    }

    @Test
    public void testTrackGoalInvalidId() throws Exception {
        track().goal(-1).revenue(100f).with(mTracker);
        verify(mTracker, never()).track(mCaptor.capture());
    }

    @Test
    public void testTrackContentImpression() throws Exception {
        String name = "test name2";
        track().impression(name).piece("test").target("test2").with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_NAME), name);
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_TARGET), "test2");
    }

    @Test
    public void testTrackContentInteraction() throws Exception {
        String interaction = "interaction";
        String name = "test name2";
        track().interaction(name, interaction).piece("test").target("test2").with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_INTERACTION), interaction);
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_NAME), name);
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_TARGET), "test2");
    }

    @Test
    public void testTrackEcommerceCartUpdate() throws Exception {
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        items.addItem(new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3));
        track().cartUpdate(50000).items(items).with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals(mCaptor.getValue().get(QueryParams.GOAL_ID), "0");
        assertEquals(mCaptor.getValue().get(QueryParams.REVENUE), "500.00");

        String ecommerceItemsJson = mCaptor.getValue().get(QueryParams.ECOMMERCE_ITEMS);

        new JSONArray(ecommerceItemsJson); // will throw exception if not valid json

        assertTrue(ecommerceItemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(ecommerceItemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
    }

    @Test
    public void testTrackEcommerceOrder() throws Exception {
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        items.addItem(new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3));
        track().order("orderId", 10020).subTotal(7002).tax(2000).shipping(1000).discount(0).items(items).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.GOAL_ID), "0");
        assertEquals(tracked.get(QueryParams.ORDER_ID), "orderId");
        assertEquals(tracked.get(QueryParams.REVENUE), "100.20");
        assertEquals(tracked.get(QueryParams.SUBTOTAL), "70.02");
        assertEquals(tracked.get(QueryParams.TAX), "20.00");
        assertEquals(tracked.get(QueryParams.SHIPPING), "10.00");
        assertEquals(tracked.get(QueryParams.DISCOUNT), "0.00");

        String ecommerceItemsJson = tracked.get(QueryParams.ECOMMERCE_ITEMS);

        new JSONArray(ecommerceItemsJson); // will throw exception if not valid json

        assertTrue(ecommerceItemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(ecommerceItemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
    }

    @Test
    public void testTrackException() throws Exception {
        Exception catchedException;
        try {
            throw new Exception("Test");
        } catch (Exception e) {
            catchedException = e;
        }
        assertNotNull(catchedException);
        track().exception(catchedException).description("<Null> exception").fatal(false).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertEquals(mCaptor.getValue().get(QueryParams.EVENT_CATEGORY), "Exception");
        StackTraceElement traceElement = catchedException.getStackTrace()[0];
        assertNotNull(traceElement);
        assertEquals(mCaptor.getValue().get(QueryParams.EVENT_ACTION), "org.piwik.sdk.extra.TrackHelperTest" + "/" + "testTrackException" + ":" + traceElement.getLineNumber());
        assertEquals(mCaptor.getValue().get(QueryParams.EVENT_NAME), "<Null> exception");
    }

    @Test
    public void testPiwikExceptionHandler() throws Exception {
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler);
        track().uncaughtExceptions().with(mTracker);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler);
        try {
            //noinspection NumericOverflow
            int i = 1 / 0;
            assertNotEquals(i, 0);
        } catch (Exception e) {
            (Thread.getDefaultUncaughtExceptionHandler()).uncaughtException(Thread.currentThread(), e);
        }
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "Exception");
        assertTrue(tracked.get(QueryParams.EVENT_ACTION).startsWith("org.piwik.sdk.extra.TrackHelperTest/testPiwikExceptionHandler:"));
        assertEquals(tracked.get(QueryParams.EVENT_NAME), "/ by zero");
        assertEquals(tracked.get(QueryParams.EVENT_VALUE), "1");

        boolean exception = false;
        try {
            track().uncaughtExceptions().with(mTracker);
        } catch (RuntimeException e) {
            exception = true;
        }
        assertTrue(exception);
    }
}
