package org.piwik.sdk;

import org.apache.maven.artifact.ant.shaded.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class CustomVariablesTest {

    @Test
    public void testToString() throws Exception {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "name", "value");
        cv.put(2, "name2", "uńicódę");

        String cvJson = cv.toString();
        new JSONObject(cvJson); //Will throw exception if not valid json

        assertTrue(cvJson.contains("\"2\":[\"name2\",\"uńicódę\"]"));
        assertTrue(cvJson.contains("\"1\":[\"name\",\"value\"]"));
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
    public void testTrimLongValue() throws Exception {
        CustomVariables cv = new CustomVariables();

        cv.put(1, StringUtils.repeat("a", CustomVariables.MAX_LENGTH + 41),
                StringUtils.repeat("b", CustomVariables.MAX_LENGTH + 100));

        assertEquals(cv.toString().length(), 13 + CustomVariables.MAX_LENGTH * 2); // 13 + 200x2
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
