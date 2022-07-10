package org.matomo.sdk.extra;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.TrackMe;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CustomDimensionTest extends BaseTest {

    @Test
    public void testSetCustomDimensions() {
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
    public void testSet_truncate() {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 1, new String(new char[1000]));
        assertEquals(255, trackMe.get("dimension1").length());
    }

    @Test
    public void testSet_badId() {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 0, UUID.randomUUID().toString());
        assertTrue(trackMe.isEmpty());
    }

    @Test
    public void testSet_removal() {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 1, UUID.randomUUID().toString());
        assertFalse(trackMe.isEmpty());
        CustomDimension.setDimension(trackMe, 1, null);
        assertTrue(trackMe.isEmpty());
    }

    @Test
    public void testSet_empty() {
        TrackMe trackMe = new TrackMe();
        CustomDimension.setDimension(trackMe, 1, UUID.randomUUID().toString());
        assertFalse(trackMe.isEmpty());
        CustomDimension.setDimension(trackMe, 1, "");
        assertTrue(trackMe.isEmpty());
    }
}
