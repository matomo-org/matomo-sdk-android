package org.matomo.sdk.extra;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class CustomVariablesTest extends BaseTest {

    @Test
    public void testPutAll() {
        CustomVariables target = new CustomVariables();
        target.put(1, "name1", "value1");
        target.put(2, "name2", "value2");

        CustomVariables toPut = new CustomVariables();
        target.put(2, "name2X", "value2X");
        target.put(3, "name3", "value3");

        target.putAll(toPut);

        assertTrue(target.toString().contains("\"1\":[\"name1\",\"value1\"]"));
        assertTrue(target.toString().contains("\"2\":[\"name2X\",\"value2X\"]"));
        assertTrue(target.toString().contains("\"3\":[\"name3\",\"value3\"]"));
    }

    @Test
    public void testInherit() throws Exception {
        CustomVariables ancestor = new CustomVariables();
        ancestor.put(1, "name", "value");
        ancestor.put(2, "name2", "uńicódę");

        CustomVariables cv = new CustomVariables(ancestor);
        String cvJson = cv.toString();
        new JSONObject(cvJson); //Will throw exception if not valid json
        assertTrue(cvJson.contains("\"2\":[\"name2\",\"uńicódę\"]"));
        assertTrue(cvJson.contains("\"1\":[\"name\",\"value\"]"));
    }

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
    public void testToStringJSON() {
        CustomVariables cv = new CustomVariables();
        cv.put(5, "name 1", "\"@<& '");

        assertEquals(
                "{\"5\":[\"name 1\",\"\\\"@<& '\"]}",
                cv.toString()
        );
    }

    @Test
    public void testTrimLongValue() {
        CustomVariables cv = new CustomVariables();
        String multipleA = String.join("", Collections.nCopies(CustomVariables.MAX_LENGTH + 41, "a"));
        String multipleB = String.join("", Collections.nCopies(CustomVariables.MAX_LENGTH + 100, "B"));

        cv.put(1, multipleA, multipleB);

        assertEquals(cv.toString().length(), 13 + CustomVariables.MAX_LENGTH * 2); // 13 + 200x2
    }

    @Test
    public void testWrongIndex() {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "name", "value");
        cv.put(-1, "name-1", "value");

        assertEquals(
                "{\"1\":[\"name\",\"value\"]}",
                cv.toString()
        );
    }

    @Test
    public void testWrongValueSize() {
        CustomVariables cv = new CustomVariables();
        cv.put("test", new JSONArray(Arrays.asList("1", "2", "3")));
        assertEquals(0, cv.size());
        assertNull(cv.toString());
        cv.put("test", new JSONArray(Arrays.asList("1", "2")));
        assertEquals("{\"test\":[\"1\",\"2\"]}", cv.toString());
    }

    @Test
    public void testInject() {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "name", "value");
        TrackMe trackMe = new TrackMe();
        cv.injectVisitVariables(trackMe);
        assertEquals(cv.toString(), trackMe.get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
    }

    @Test
    public void testToTrackMe() {
        CustomVariables cv = new CustomVariables();
        cv.put(1, "name", "value");
        TrackMe trackMe = cv.toVisitVariables();
        assertEquals(cv.toString(), trackMe.get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
    }

    @Test
    public void testVisitCustomVariables() {
        CustomVariables visitVars = new CustomVariables();
        visitVars.put(1, "visit", "valueX");

        CustomVariables _screen = new CustomVariables();
        _screen.put(1, "screen", "valueY");

        final TrackMe trackMe = TrackHelper.track(visitVars.toVisitVariables())
                .screen("/path")
                .variable(1, "screen", "valueY")
                .build();

        assertEquals(visitVars.toString(), trackMe.get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
        assertEquals(_screen.toString(), trackMe.get(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES));
        assertEquals("/path", trackMe.get(QueryParams.URL_PATH));
    }

}
