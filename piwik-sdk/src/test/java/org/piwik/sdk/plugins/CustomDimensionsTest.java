package org.piwik.sdk.plugins;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CustomDimensionsTest {

    @Test
    public void testSetCustomDimensions() throws Exception {
        CustomDimensions customDimensions = new CustomDimensions();
        customDimensions.set(0, "foo");
        customDimensions.set(1, "foo");
        customDimensions.set(2, "bar");
        customDimensions.set(3, "empty").set(3, null);
        customDimensions.set(4, "");

        Map<String, String> params = customDimensions.toMap();

        assertEquals("foo", params.get("dimension1"));
        assertEquals("bar", params.get("dimension2"));
        assertFalse(params.containsKey("dimension0"));
        assertFalse(params.containsKey("dimension3"));
        assertFalse(params.containsKey("dimension4"));
    }

    @Test
    public void testSetCustomDimensionsMaxLength() throws Exception {
        CustomDimensions customDimensions = new CustomDimensions();
        customDimensions.set(1, new String(new char[1000]));

        Map<String, String> queryParams = customDimensions.toMap();
        assertEquals(queryParams.get("dimension1").length(), 255);
    }
}
