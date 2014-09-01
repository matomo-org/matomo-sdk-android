package org.piwik.sdk;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URL;
import java.util.Arrays;
import static org.junit.Assert.*;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TrackerBulkURLWrapperTest {

    private TrackerBulkURLWrapper createWrapper(String url, String... events) throws Exception{
        if (url == null){
            url = "http://example.com/";
        }
        URL _url = new URL(url);
        TrackerBulkURLWrapper wrapper = new TrackerBulkURLWrapper(_url, Arrays.asList(events), "test_token");
        return wrapper;
    }

    @Test
    public void testIterator() throws Exception {

    }

    @Test
    public void testGetApiUrl() throws Exception {
        String url = "http://www.com/java.htm";
        TrackerBulkURLWrapper wrapper = createWrapper(url, "");
        assertEquals(wrapper.getApiUrl().toString(), url);
    }

    @Test
    public void testGetEvents() throws Exception {
        TrackerBulkURLWrapper wrapper = createWrapper(null, "?one=1", "?two=2");
        TrackerBulkURLWrapper.Page page = wrapper.iterator().next();

        assertEquals(wrapper.getEvents(page).getJSONArray("requests").length(), 2);
        assertEquals(wrapper.getEvents(page).getJSONArray("requests").get(0), "?one=1");
        assertEquals(wrapper.getEvents(page).getJSONArray("requests").get(1), "?two=2");
        assertEquals(wrapper.getEvents(page).getString("token_auth"), "test_token");
    }

    @Test
    public void testGetEventUrl() throws Exception {

    }
}