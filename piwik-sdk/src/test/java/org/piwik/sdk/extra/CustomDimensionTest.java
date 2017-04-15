package org.piwik.sdk.extra;

import org.junit.Test;
import org.piwik.sdk.TrackMe;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CustomDimensionTest {

    @Test
    public void testSetCustomDimensions() throws Exception {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 0, "foo");
        CustomDimension.setDimension(trackMe, 1, "foo");
        CustomDimension.setDimension(trackMe, 2, "bar");
        CustomDimension.setDimension(trackMe, 3, "empty");
        CustomDimension.setDimension(trackMe, 3, null);
        CustomDimension.setDimension(trackMe, 4, "");


        assertEquals("foo", trackMe.get("dimension1"));
        assertEquals("bar", trackMe.get("dimension2"));
        assertNull(trackMe.get("dimension0"));
        assertNull(trackMe.get("dimension3"));
        assertNull(trackMe.get("dimension4"));
    }

    @Test
    public void testSet_truncate() throws Exception {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 1, new String(new char[1000]));
        assertEquals(255, trackMe.get("dimension1").length());
    }

    @Test
    public void testSet_badId() throws Exception {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 0, UUID.randomUUID().toString());
        assertTrue(trackMe.isEmpty());
    }

    @Test
    public void testSet_removal() throws Exception {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 1, UUID.randomUUID().toString());
        assertFalse(trackMe.isEmpty());
        CustomDimension.setDimension(trackMe, 1, null);
        assertTrue(trackMe.isEmpty());
    }

    @Test
    public void testSet_empty() throws Exception {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 1, UUID.randomUUID().toString());
        assertFalse(trackMe.isEmpty());
        CustomDimension.setDimension(trackMe, 1, "");
        assertTrue(trackMe.isEmpty());
    }
}
