package org.piwik.sdk;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.util.Pair;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.ecommerce.EcommerceItems;
import org.piwik.sdk.testhelper.DefaultTestCase;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.piwik.sdk.testhelper.TestActivity;
import org.piwik.sdk.tools.UrlHelper;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class TrackHelperTest extends DefaultTestCase {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void testPiwikAutoBindActivities() throws Exception {
        Application app = Robolectric.application;
        Piwik piwik = Piwik.getInstance(app);
        piwik.setDryRun(true);
        piwik.setOptOut(true);
        Tracker tracker = createTracker();
        //auto attach tracking screen view
        final Application.ActivityLifecycleCallbacks callbacks = TrackHelper.track().screens(app).with(tracker);

        // emulate default trackScreenView
        Robolectric.buildActivity(TestActivity.class).create().start().resume().visible().get();

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        validateDefaultQuery(queryParams);
        assertEquals(queryParams.get(QueryParams.ACTION_NAME), TestActivity.getTestTitle());

        app.unregisterActivityLifecycleCallbacks(callbacks);
        tracker.clearLastEvent();
        assertNull(tracker.getLastEvent());
        // emulate default trackScreenView
        Robolectric.buildActivity(TestActivity.class).create().start().resume().visible().get();
        assertNull(tracker.getLastEvent());
    }

    @Test
    public void testPiwikApplicationGetTracker() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getTracker(), piwikApplication.getTracker());
    }

    @Test
    public void testPiwikApplicationgetPiwik() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getPiwik(), Piwik.getInstance(piwikApplication));
    }

    @Test
    public void testEmptyQueueDispatch() throws Exception {
        assertFalse(createTracker().dispatch());
    }

    @Test
    public void testSetDispatchInterval() throws Exception {
        Tracker tracker = createTracker();
        tracker.setDispatchInterval(1);
        assertEquals(tracker.getDispatchInterval(), 1);
    }

    @Test
    public void testSetDispatchTimeout() throws Exception {
        Tracker tracker = createTracker();
        tracker.setDispatchTimeout(1337);

        assertEquals(1337, tracker.getDispatcher().getConnectionTimeOut());
        assertEquals(1337, tracker.getDispatchTimeout());
    }

    @Test
    public void testGetSiteId() throws Exception {
        assertEquals(createTracker().getSiteId(), 1);
    }

    @Test
    public void testGetPiwik() throws Exception {
        PiwikApplication piwikApplication = (PiwikApplication) Robolectric.application;
        assertEquals(piwikApplication.getPiwik(), Piwik.getInstance(piwikApplication));
    }

    @Test
    public void testSetURL() throws Exception {
        Tracker tracker = createTracker();
        tracker.setApplicationDomain("test.com");
        assertEquals(tracker.getApplicationDomain(), "test.com");
        assertEquals(tracker.getApplicationBaseURL(), "http://test.com");
        TrackMe trackMe = new TrackMe();
        tracker.track(trackMe);
        assertEquals("http://test.com/", trackMe.get(QueryParams.URL_PATH));

        trackMe.set(QueryParams.URL_PATH, "me");
        tracker.track(trackMe);
        assertEquals("http://test.com/me", trackMe.get(QueryParams.URL_PATH));

        // override protocol
        trackMe.set(QueryParams.URL_PATH, "https://my.com/secure");
        tracker.track(trackMe);
        assertEquals("https://my.com/secure", trackMe.get(QueryParams.URL_PATH));
    }

    @Test
    public void testOutlink() throws Exception {
        Tracker tracker = createTracker();
        assertNull(tracker.getLastEvent());

        TrackHelper.track().outlink(new URL("file://mount/sdcard/something")).with(tracker);
        assertNull(tracker.getLastEvent());

        URL valid = new URL("https://foo.bar");
        TrackHelper.track().outlink(valid).with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.URL_PATH));

        valid = new URL("https://foo.bar");
        TrackHelper.track().outlink(valid).with(tracker);
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.URL_PATH));

        valid = new URL("ftp://foo.bar");
        TrackHelper.track().outlink(valid).with(tracker);
        queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.LINK));
        assertEquals(valid.toExternalForm(), queryParams.get(QueryParams.URL_PATH));
    }

    @Test
    public void testDownloadTrackForced() throws Exception {
        Tracker tracker = createTracker();
        assertNull(tracker.getLastEvent());

        TrackHelper.track().download().with(tracker);
        assertNotNull(tracker.getLastEvent());

        tracker.clearLastEvent();

        TrackHelper.track().download().with(tracker);
        assertNull(tracker.getLastEvent());

        TrackHelper.track().download().force().with(tracker);
        assertNotNull(tracker.getLastEvent());
    }

    @Test
    public void testDownloadCustomVersion() throws Exception {
        Tracker tracker = createTracker();
        assertNull(tracker.getLastEvent());

        String version = UUID.randomUUID().toString();
        TrackHelper.track().download().version(version).with(tracker);
        assertNotNull(tracker.getLastEvent());
        QueryHashMap<String, String> map = parseEventUrl(tracker.getLastEvent());
        assertTrue(map.get(QueryParams.DOWNLOAD).endsWith(version));

        tracker.clearLastEvent();
        TrackHelper.track().download().version(version).with(tracker);
        assertNull(tracker.getLastEvent());
    }

    @Test
    public void testSetScreenCustomVariable() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track()
                .screen("")
                .variable(1, "2", "3")
                .with(tracker);

        String event = tracker.getLastEvent();
        Map<String, String> queryParams = parseEventUrl(event);

        assertEquals("{'1':['2','3']}".replaceAll("'", "\""), queryParams.get("cvar"));
    }

    @Test
    public void testTrackScreenView() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().screen("/test/test").title("title").with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertTrue(queryParams.get(QueryParams.URL_PATH).endsWith("/test/test"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackScreenWithTitleView() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().screen("test/test").title("Test title").with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertTrue(queryParams.get(QueryParams.URL_PATH).endsWith("/test/test"));
        assertEquals(queryParams.get(QueryParams.ACTION_NAME), "Test title");
        validateDefaultQuery(queryParams);
    }

    private void checkEvent(QueryHashMap<String, String> queryParams, String name, Float value) {
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "category");
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "test action");
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), name);
        if (value == null) {
            assertNull(queryParams.get(QueryParams.EVENT_VALUE));
        } else {
            assertEquals(String.valueOf(queryParams.get(QueryParams.EVENT_VALUE)), String.valueOf(value));
        }
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackEvent() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().event("category", "test action").with(tracker);
        checkEvent(parseEventUrl(tracker.getLastEvent()), null, null);
    }

    @Test
    public void testTrackEventName() throws Exception {
        Tracker tracker = createTracker();
        String name = "test name2";
        TrackHelper.track().event("category", "test action").name(name).with(tracker);
        checkEvent(parseEventUrl(tracker.getLastEvent()), name, null);
    }

    @Test
    public void testTrackEventNameAndValue() throws Exception {
        Tracker tracker = createTracker();
        String name = "test name3";
        TrackHelper.track().event("category", "test action").name(name).value(1f).with(tracker);
        checkEvent(parseEventUrl(tracker.getLastEvent()), name, 1f);
    }

    @Test
    public void testTrackGoal() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().goal(1).with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertNull(queryParams.get(QueryParams.REVENUE));
        assertEquals(queryParams.get(QueryParams.GOAL_ID), "1");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackSiteSearch() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().search("keyword").category("category").count(1337).with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.SEARCH_KEYWORD), "keyword");
        assertEquals(queryParams.get(QueryParams.SEARCH_CATEGORY), "category");
        assertEquals(queryParams.get(QueryParams.SEARCH_NUMBER_OF_HITS), String.valueOf(1337));
        validateDefaultQuery(queryParams);

        TrackHelper.track().search("keyword2").with(tracker);
        queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.SEARCH_KEYWORD), "keyword2");
        assertNull(queryParams.get(QueryParams.SEARCH_CATEGORY));
        assertNull(queryParams.get(QueryParams.SEARCH_NUMBER_OF_HITS));
    }

    @Test
    public void testTrackGoalRevenue() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().goal(1).revenue(100f).with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals("1", queryParams.get(QueryParams.GOAL_ID));
        assertTrue(100f == Float.valueOf(queryParams.get(QueryParams.REVENUE)));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackGoalInvalidId() throws Exception {
        Tracker tracker = createTracker();
        TrackHelper.track().goal(-1).revenue(100f).with(tracker);
        assertNull(tracker.getLastEvent());
    }

    @Test
    public void testTrackContentImpression() throws Exception {
        Tracker tracker = createTracker();
        String name = "test name2";
        TrackHelper.track().impression(name).piece("test").target("test2").with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.CONTENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(queryParams.get(QueryParams.CONTENT_TARGET), "test2");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackContentInteraction() throws Exception {
        Tracker tracker = createTracker();
        String interaction = "interaction";
        String name = "test name2";
        TrackHelper.track().interaction(name, interaction).piece("test").target("test2").with(tracker);

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.CONTENT_INTERACTION), interaction);
        assertEquals(queryParams.get(QueryParams.CONTENT_NAME), name);
        assertEquals(queryParams.get(QueryParams.CONTENT_PIECE), "test");
        assertEquals(queryParams.get(QueryParams.CONTENT_TARGET), "test2");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackEcommerceCartUpdate() throws Exception {
        Tracker tracker = createTracker();
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        items.addItem(new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3));
        TrackHelper.track().cartUpdate(50000).items(items).with(tracker);

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());

        assertEquals(queryParams.get(QueryParams.GOAL_ID), "0");
        assertEquals(queryParams.get(QueryParams.REVENUE), "500.00");

        String ecommerceItemsJson = queryParams.get(QueryParams.ECOMMERCE_ITEMS);

        new JSONArray(ecommerceItemsJson); // will throw exception if not valid json

        assertTrue(ecommerceItemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(ecommerceItemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackEcommerceOrder() throws Exception {
        Tracker tracker = createTracker();
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        items.addItem(new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3));
        TrackHelper.track().order("orderId", 10020).subTotal(7002).tax(2000).shipping(1000).discount(0).items(items).with(tracker);

        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(queryParams.get(QueryParams.GOAL_ID), "0");
        assertEquals(queryParams.get(QueryParams.ORDER_ID), "orderId");
        assertEquals(queryParams.get(QueryParams.REVENUE), "100.20");
        assertEquals(queryParams.get(QueryParams.SUBTOTAL), "70.02");
        assertEquals(queryParams.get(QueryParams.TAX), "20.00");
        assertEquals(queryParams.get(QueryParams.SHIPPING), "10.00");
        assertEquals(queryParams.get(QueryParams.DISCOUNT), "0.00");

        String ecommerceItemsJson = queryParams.get(QueryParams.ECOMMERCE_ITEMS);

        new JSONArray(ecommerceItemsJson); // will throw exception if not valid json

        assertTrue(ecommerceItemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(ecommerceItemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testTrackException() throws Exception {
        Tracker tracker = createTracker();
        Exception catchedException;
        try {
            throw new Exception("Test");
        } catch (Exception e) {
            catchedException = e;
        }
        assertNotNull(catchedException);
        TrackHelper.track().exception(catchedException).description("<Null> exception").fatal(false).with(tracker);
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Exception");
        StackTraceElement traceElement = catchedException.getStackTrace()[0];
        assertNotNull(traceElement);
        assertEquals(queryParams.get(QueryParams.EVENT_ACTION), "org.piwik.sdk.TrackHelperTest" + "/" + "testTrackException" + ":" + traceElement.getLineNumber());
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), "<Null> exception");
        validateDefaultQuery(queryParams);
    }

    @Test
    public void testPiwikExceptionHandler() throws Exception {
        Tracker tracker = createTracker();
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler);
        TrackHelper.track().uncaughtExceptions().with(tracker);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler);
        try {
            //noinspection NumericOverflow
            int i = 1 / 0;
            assertNotEquals(i, 0);
        } catch (Exception e) {
            (Thread.getDefaultUncaughtExceptionHandler()).uncaughtException(Thread.currentThread(), e);
        }
        QueryHashMap<String, String> queryParams = parseEventUrl(tracker.getLastEvent());
        validateDefaultQuery(queryParams);
        assertEquals(queryParams.get(QueryParams.EVENT_CATEGORY), "Exception");
        assertTrue(queryParams.get(QueryParams.EVENT_ACTION).startsWith("org.piwik.sdk.TrackHelperTest/testPiwikExceptionHandler:"));
        assertEquals(queryParams.get(QueryParams.EVENT_NAME), "/ by zero");
        assertEquals(queryParams.get(QueryParams.EVENT_VALUE), "1");

        boolean exception = false;
        try {
            TrackHelper.track().uncaughtExceptions().with(tracker);
        } catch (RuntimeException e) {
            exception = true;
        }
        assertTrue(exception);
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
