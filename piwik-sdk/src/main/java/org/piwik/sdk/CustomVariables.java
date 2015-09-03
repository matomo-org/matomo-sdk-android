/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import org.json.JSONArray;
import org.json.JSONObject;
import org.piwik.sdk.tools.Logy;

import java.util.Arrays;
import java.util.HashMap;


public class CustomVariables extends HashMap<String, JSONArray> {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "CustomVariables";
    /**
     * You can track up to 5 custom variables for each user to your app,
     * and up to 5 custom variables for each screen view.
     * <p/>
     * Desired json output:
     * {
     * "1":["OS","iphone 5.0"],
     * "2":["Piwik Mobile Version","1.6.2"],
     * "3":["Locale","en::en"],
     * "4":["Num Accounts","2"],
     * "5":["Level","over9k"]
     * }
     */
    private static final int MAX_VARIABLES = 5;
    protected static final int MAX_LENGTH = 200;

    public CustomVariables() {
        super(MAX_VARIABLES);
    }

    /**
     * Custom variable names and values are limited to 200 characters in length each.
     *
     * @param index this Integer accepts values from 1 to 5.
     *              A given custom variable name must always be stored in the same "index" per session.
     *              For example, if you choose to store the variable name = "Gender" in index = 1
     *              and you record another custom variable in index = 1, then the "Gender" variable
     *              will be deleted and replaced with the new custom variable stored in index 1.
     * @param name  of a specific Custom Variable such as "User type".
     * @param value of a specific Custom Variable such as "Customer".
     * @return super.put result if index in right range and name/value pair aren't null
     */
    public JSONArray put(int index, String name, String value) {
        if (index > 0 && index <= MAX_VARIABLES && name != null & value != null) {

            if (name.length() > MAX_LENGTH) {
                Logy.w(LOGGER_TAG, String.format("Name is too long %s", name));
                name = name.substring(0, MAX_LENGTH);
            }

            if (value.length() > MAX_LENGTH) {
                Logy.w(LOGGER_TAG, String.format("Value is too long %s", value));
                value = value.substring(0, MAX_LENGTH);
            }

            return put(Integer.toString(index), new JSONArray(Arrays.asList(name, value)));
        }
        Logy.d(LOGGER_TAG, "Index is out of range or name/value is null");
        return null;
    }

    /**
     * @param index  index accepts values from 1 to 5.
     * @param values packed key/value pair
     * @return super.put result or null if key is null or value length is not equals 2
     */
    @Override
    public JSONArray put(String index, JSONArray values) {
        if (values.length() == 2 && index != null) {
            return super.put(index, values);
        }
        Logy.d(LOGGER_TAG, "value length should be equal 2");
        return null;
    }

    @Override
    public String toString() {
        if (size() == 0) {
            return null;
        }
        return new JSONObject(this).toString();
    }

}
