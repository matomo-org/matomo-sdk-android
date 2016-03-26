package org.piwik.sdk;

import android.util.Log;

import org.apache.maven.artifact.ant.shaded.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertEquals(null, cv.toString());
        assertNull(cv.put("test", new JSONArray(Arrays.asList("1", "2"))));
        assertEquals(
                "{\"test\":[\"1\",\"2\"]}",
                cv.toString()
        );

    }

    @Test
    public void testConcurrency() throws Exception {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "1", "1");
        launchTestThreads(cv, 10);
    }

    private static void launchTestThreads(final CustomVariables cv, final int threadCount) {
        Log.d("launchTestThreads", "Launching " + threadCount + " threads");
        final JSONArray jsonArray = new JSONArray(Arrays.asList("1", "2"));
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < threadCount * 10000; j++) {
                            cv.toString();
                            cv.put("test" + j, jsonArray);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        assertFalse(true);
                    }
                }
            }).start();
        }
        Log.d("launchTestThreads", "All launched.");
    }

}
