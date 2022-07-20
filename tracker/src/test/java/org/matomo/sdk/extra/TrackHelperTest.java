package org.matomo.sdk.extra;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.matomo.sdk.Matomo;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.Tracker;
import org.matomo.sdk.dispatcher.DispatchMode;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.matomo.sdk.extra.TrackHelper.track;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("deprecation")
public class TrackHelperTest {
    ArgumentCaptor<TrackMe> mCaptor = ArgumentCaptor.forClass(TrackMe.class);
    @Mock Tracker mTracker;
    @Mock Matomo mMatomo;
    @Mock Context mContext;
    @Mock PackageManager mPackageManager;
    @Mock MatomoApplication mMatomoApplication;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        when(mTracker.getMatomo()).thenReturn(mMatomo);
        when(mMatomo.getContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("packageName");
        when(mMatomoApplication.getTracker()).thenReturn(mTracker);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = 123;
        //noinspection WrongConstant
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
    }

    @Test
    public void testBaseEvent() {
        track().screen("/path").with(mMatomoApplication);
        verify(mMatomoApplication).getTracker();
        verify(mTracker).track(any(TrackMe.class));
    }

    @Test
    public void testBaseEvent_track_safely() {
        final TrackHelper.BaseEvent badTrackMe = new TrackHelper.BaseEvent(null) {
            @Override
            public TrackMe build() {
                throw new IllegalArgumentException();
            }
        };
        assertThat(badTrackMe.safelyWith(mTracker), is(false));
        assertThat(badTrackMe.safelyWith(mMatomoApplication), is(false));
        verify(mTracker, never()).track(any(TrackMe.class));

        final TrackHelper.BaseEvent goodTrackMe = new TrackHelper.BaseEvent(null) {
            @Override
            public TrackMe build() {
                return new TrackMe();
            }
        };
        assertThat(goodTrackMe.safelyWith(mTracker), is(true));
        verify(mTracker, times(1)).track(any(TrackMe.class));
        assertThat(goodTrackMe.safelyWith(mMatomoApplication), is(true));
        verify(mTracker, times(2)).track(any(TrackMe.class));
    }

    @Test
    public void testOutlink() throws Exception {
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

    @Test(expected = IllegalArgumentException.class)
    public void testOutlink_invalid_url() throws MalformedURLException {
        track().outlink(new URL("file://mount/sdcard/something")).build();
    }

    @Test
    public void testDownloadTrackChecksum() {
        DownloadTracker downloadTracker = mock(DownloadTracker.class);
        track().download(downloadTracker).identifier(new DownloadTracker.Extra.ApkChecksum(mContext)).with(mTracker);
        verify(downloadTracker).trackOnce(any(TrackMe.class), any(DownloadTracker.Extra.ApkChecksum.class));
    }

    @Test
    public void testDownloadTrackForced() {
        DownloadTracker downloadTracker = mock(DownloadTracker.class);
        track().download(downloadTracker).force().with(mTracker);
        verify(downloadTracker).trackNewAppDownload(any(TrackMe.class), any(DownloadTracker.Extra.None.class));
    }

    @Test
    public void testDownloadCustomVersion() {
        DownloadTracker downloadTracker = mock(DownloadTracker.class);
        String version = UUID.randomUUID().toString();

        track().download(downloadTracker).version(version).with(mTracker);
        verify(downloadTracker).setVersion(version);
        verify(downloadTracker).trackOnce(any(TrackMe.class), any(DownloadTracker.Extra.class));
    }

    @Test
    public void testVisitCustomVariables_merge_base() {
        CustomVariables varsA = new CustomVariables().put(1, "visit1", "A");
        CustomVariables varsB = new CustomVariables().put(2, "visit2", "B");
        CustomVariables combined = new CustomVariables().put(1, "visit1", "A").put(2, "visit2", "B");

        TrackHelper.track(varsA.toVisitVariables())
                .visitVariables(varsB)
                .screen("/path")
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals(combined.toString(), mCaptor.getValue().get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
        assertEquals("/path", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testVisitCustomVariables_merge_singles() {
        CustomVariables varsA = new CustomVariables().put(1, "visit1", "A");
        CustomVariables varsB = new CustomVariables().put(2, "visit2", "B");
        CustomVariables combined = new CustomVariables().put(1, "visit1", "A").put(2, "visit2", "B");

        TrackHelper.track()
                .visitVariables(varsA)
                .visitVariables(varsB)
                .screen("/path")
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals(combined.toString(), mCaptor.getValue().get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
        assertEquals("/path", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testVisitCustomVariables_add() {
        CustomVariables _vars = new CustomVariables();
        _vars.put(1, "visit1", "A");
        _vars.put(2, "visit2", "B");

        TrackHelper.track()
                .visitVariables(1, "visit1", "A")
                .visitVariables(2, "visit2", "B")
                .screen("/path")
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals(_vars.toString(), mCaptor.getValue().get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
        assertEquals("/path", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testSetScreenCustomVariable() {
        track()
                .screen("")
                .variable(1, "2", "3")
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals("{'1':['2','3']}".replaceAll("'", "\""), mCaptor.getValue().get(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES));
    }

    @Test
    public void testSetScreenCustomDimension() {
        track()
                .screen("")
                .dimension(1, "dim1")
                .dimension(2, "dim2")
                .dimension(3, "dim3")
                .dimension(3, null)
                .dimension(4, null)
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals("dim1", CustomDimension.getDimension(mCaptor.getValue(), 1));
        assertEquals("dim2", CustomDimension.getDimension(mCaptor.getValue(), 2));
        assertNull(CustomDimension.getDimension(mCaptor.getValue(), 3));
        assertNull(CustomDimension.getDimension(mCaptor.getValue(), 4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetScreem_empty_path() {
        TrackHelper.track().screen((String) null).build();
    }

    @Test
    public void testCustomDimension_trackHelperAny() {
        TrackHelper.track()
                .dimension(1, "visit")
                .dimension(2, "screen")
                .event("category", "action")
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertEquals("visit", CustomDimension.getDimension(mCaptor.getValue(), 1));
        assertEquals("screen", CustomDimension.getDimension(mCaptor.getValue(), 2));
        assertEquals("category", mCaptor.getValue().get(QueryParams.EVENT_CATEGORY));
        assertEquals("action", mCaptor.getValue().get(QueryParams.EVENT_ACTION));
    }

    @Test
    public void testCustomDimension_override() {
        TrackHelper.track()
                .dimension(1, "visit")
                .dimension(2, "screen")
                .screen("/path")
                .dimension(1, null)
                .with(mTracker);

        verify(mTracker).track(mCaptor.capture());
        assertNull(CustomDimension.getDimension(mCaptor.getValue(), 1));
        assertEquals("screen", CustomDimension.getDimension(mCaptor.getValue(), 2));
        assertEquals("/path", mCaptor.getValue().get(QueryParams.URL_PATH));
    }

    @Test
    public void testTrackScreenView() {
        track().screen("/test/test").title("title").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertTrue(mCaptor.getValue().get(QueryParams.URL_PATH).endsWith("/test/test"));
    }

    @Test
    public void testTrackScreenWithTitleView() {
        track().screen("/test/test").title("Test title").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertTrue(mCaptor.getValue().get(QueryParams.URL_PATH).endsWith("/test/test"));
        assertEquals(mCaptor.getValue().get(QueryParams.ACTION_NAME), "Test title");
    }

    @Test
    public void testTrackScreenWithCampaignView() {
        track().screen("/test/test").campaign("campaign_name", "campaign_keyword").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        assertTrue(mCaptor.getValue().get(QueryParams.URL_PATH).endsWith("/test/test"));
        assertEquals(mCaptor.getValue().get(QueryParams.CAMPAIGN_NAME), "campaign_name");
        assertEquals(mCaptor.getValue().get(QueryParams.CAMPAIGN_KEYWORD), "campaign_keyword");
    }

    @Test
    public void testTrackEvent() {
        track().event("category", "test action").with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(tracked.get(QueryParams.EVENT_ACTION), "test action");
    }

    @Test
    public void testTrackEventName() {
        String name = "test name2";
        track().event("category", "test action").name(name).with(mTracker);
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(tracked.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(tracked.get(QueryParams.EVENT_NAME), name);
    }

    @Test
    public void testTrackEventNameAndValue() {
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
    public void testTrackEventNameAndValueWithpath() {
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
    public void testTrackGoal() {
        track().goal(1).with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertNull(mCaptor.getValue().get(QueryParams.REVENUE));
        assertEquals(mCaptor.getValue().get(QueryParams.GOAL_ID), "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrackGoal_invalid_id() {
        track().goal(-1).revenue(100f).build();
    }

    @Test
    public void testTrackSiteSearch() {
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
    public void testTrackGoalRevenue() {
        track().goal(1).revenue(100f).with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals("1", mCaptor.getValue().get(QueryParams.GOAL_ID));
        assertEquals(100f, Float.parseFloat(mCaptor.getValue().get(QueryParams.REVENUE)), 0.0);
    }

    @Test
    public void testTrackContentImpression() {
        String name = "test name2";
        track().impression(name).piece("test").target("test2").with(mTracker);
        verify(mTracker).track(mCaptor.capture());

        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_NAME), name);
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(mCaptor.getValue().get(QueryParams.CONTENT_TARGET), "test2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrackContentImpression_invalid_name_empty() {
        track().impression("").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrackContentImpression_invalid_name_null() {
        track().impression(null).build();
    }

    @Test
    public void testTrackContentInteraction_invalid_name_empty() {
        int errorCount = 0;
        try {
            track().interaction("", "test").piece("test").target("test2").build();
        } catch (IllegalArgumentException e) { errorCount++; }
        try {
            track().interaction("test", "").piece("test").target("test2").build();
        } catch (IllegalArgumentException e) { errorCount++; }
        try {
            track().interaction("", "").piece("test").target("test2").build();
        } catch (IllegalArgumentException e) { errorCount++; }
        assertThat(errorCount, is(3));
    }

    @Test
    public void testTrackContentInteraction_invalid_name_null() {
        int errorCount = 0;
        try {
            track().interaction(null, "test").piece("test").target("test2").build();
        } catch (IllegalArgumentException e) { errorCount++; }
        try {
            track().interaction("test", null).piece("test").target("test2").build();
        } catch (IllegalArgumentException e) { errorCount++; }
        try {
            track().interaction(null, null).piece("test").target("test2").build();
        } catch (IllegalArgumentException e) { errorCount++; }
        assertThat(errorCount, is(3));
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
    public void testTrackException() {
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
        assertEquals(mCaptor.getValue().get(QueryParams.EVENT_ACTION), "org.matomo.sdk.extra.TrackHelperTest" + "/" + "testTrackException" + ":" + traceElement.getLineNumber());
        assertEquals(mCaptor.getValue().get(QueryParams.EVENT_NAME), "<Null> exception");
    }

    @SuppressWarnings({"divzero", "NumericOverflow"})
    @Test
    public void testExceptionHandler() {
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof MatomoExceptionHandler);
        track().uncaughtExceptions().with(mTracker);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof MatomoExceptionHandler);
        try {
            int i = 1 / 0;
            assertNotEquals(i, 0);
        } catch (Exception e) {
            (Thread.getDefaultUncaughtExceptionHandler()).uncaughtException(Thread.currentThread(), e);
        }
        verify(mTracker).track(mCaptor.capture());
        TrackMe tracked = mCaptor.getValue();
        assertEquals(tracked.get(QueryParams.EVENT_CATEGORY), "Exception");
        assertTrue(tracked.get(QueryParams.EVENT_ACTION).startsWith("org.matomo.sdk.extra.TrackHelperTest/testExceptionHandler:"));
        assertEquals(tracked.get(QueryParams.EVENT_NAME), "/ by zero");
        assertEquals(tracked.get(QueryParams.EVENT_VALUE), "1");

        verify(mTracker).setDispatchMode(DispatchMode.EXCEPTION);
        verify(mTracker).dispatchBlocking();

        boolean exception = false;
        try {
            track().uncaughtExceptions().with(mTracker);
        } catch (RuntimeException e) {
            exception = true;
        }
        assertTrue(exception);
    }
}
