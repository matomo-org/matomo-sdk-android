/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import java.util.HashMap;

/**
 * Created by darken on 04.04.2015.
 */
public class TrackMe {
    private static final int DEFAULT_QUERY_CAPACITY = 14;
    private final HashMap<String, String> mQueryParams = new HashMap<String, String>(DEFAULT_QUERY_CAPACITY);
    private final CustomVariables mScreenCustomVariable = new CustomVariables();

    /**
     * You can set any additional Tracking API Parameters within the SDK.
     * This includes for example the local time (parameters h, m and s).
     * <pre>
     * tracker.set(QueryParams.HOURS, "10");
     * tracker.set(QueryParams.MINUTES, "45");
     * tracker.set(QueryParams.SECONDS, "30");
     * </pre>
     *
     * @param key   query params name
     * @param value value
     * @return tracker instance
     */
    public TrackMe set(QueryParams key, String value) {
        if (value != null && value.length() > 0)
            mQueryParams.put(key.toString(), value);
        return this;
    }

    public TrackMe set(QueryParams key, Integer value) {
        if (value != null)
            set(key, Integer.toString(value));
        return this;
    }

    public String build() {
        set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, mScreenCustomVariable.toString());
        return Dispatcher.urlEncodeUTF8(mQueryParams);
    }

    public String get(QueryParams queryParams) {
        return mQueryParams.get(queryParams.toString());
    }

    /**
     * A custom variable is a custom name-value pair that you can assign to your users or screen views,
     * and then visualize the reports of how many visits, conversions, etc. for each custom variable.
     * A custom variable is defined by a name — for example,
     * "User status" — and a value – for example, "LoggedIn" or "Anonymous".
     * You can track up to 5 custom variables for each user to your app.
     *  @param index this Integer accepts values from 1 to 5.
     *              A given custom variable name must always be stored in the same "index" per session.
     *              For example, if you choose to store the variable name = "Gender" in
     *              index = 1 and you record another custom variable in index = 1, then the
     *              "Gender" variable will be deleted and replaced with the new custom variable stored in index 1.
     * @param name  String defines the name of a specific Custom Variable such as "User type".
     * @param value String defines the value of a specific Custom Variable such as "Customer".
     */
    public TrackMe setScreenCustomVariable(int index, String name, String value) {
        mScreenCustomVariable.put(index, name, value);
        return this;
    }

    public CustomVariables getScreenCustomVariable() {
        return mScreenCustomVariable;
    }

}
