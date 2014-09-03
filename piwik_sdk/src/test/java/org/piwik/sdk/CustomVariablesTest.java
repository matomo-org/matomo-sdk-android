package org.piwik.sdk;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.*;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class CustomVariablesTest {

    @Test
    public void testToString() throws Exception {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "name", "value");
        cv.put(2, "name2", "uńicódę");

        assertEquals(
                "{\"2\":[\"name2\",\"uńicódę\"],\"1\":[\"name\",\"value\"]}",
                cv.toString()
        );
    }

     @Test
    public void testToStringJSON() throws Exception {
        CustomVariables cv = new CustomVariables();
        cv.put(5, "name 1", "\"@<& '");

        assertEquals(
                "{\"5\":[\"name 1\",\"\\\"@<& '\"]}",
                cv.toString()
        );
    }

    @Test
    public void testWrongIndex() throws Exception {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "name", "value");
        cv.put(10, "name2", "value");
        cv.put(-1, "name-1", "value");

        assertEquals(
                "{\"1\":[\"name\",\"value\"]}",
                cv.toString()
        );
    }

    @Test
    public void testWrongValueSize() throws Exception {
        CustomVariables cv = new CustomVariables();

        assertNull(cv.put("test", new JSONArray(Arrays.asList("1", "2", "3"))));
        assertNull(cv.put("test", new JSONArray(Arrays.asList("1", "2"))));
        assertEquals(
                cv.get("test"),
                cv.put("test", new JSONArray(Arrays.asList("4", "5")))
        );

    }
}