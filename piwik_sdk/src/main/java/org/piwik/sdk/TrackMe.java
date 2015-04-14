/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.support.annotation.NonNull;

import java.util.HashMap;

/**
 * This objects represents one query to Piwik.
 * For each event send to Piwik a TrackMe gets created, either explicitly by you or implicitly by the Tracker.
 */
public class TrackMe {
    private static final int DEFAULT_QUERY_CAPACITY = 14;
    private final HashMap<String, String> mQueryParams = new HashMap<>(DEFAULT_QUERY_CAPACITY);
    private final CustomVariables mScreenCustomVariable = new CustomVariables();

    /**
     * You can set any additional Tracking API Parameters within the SDK.
     * This includes for example the local time (parameters h, m and s).
     * <pre>
     * set(QueryParams.HOURS, "10");
     * set(QueryParams.MINUTES, "45");
     * set(QueryParams.SECONDS, "30");
     * </pre>
     *
     * @param key   query params name
     * @param value value
     * @return tracker instance
     */
    public synchronized TrackMe set(@NonNull QueryParams key, String value) {
        if (value == null)
            mQueryParams.remove(key.toString());
        else if (value.length() > 0)
            mQueryParams.put(key.toString(), value);
        return this;
    }

    public synchronized TrackMe set(@NonNull QueryParams key, int value) {
        set(key, Integer.toString(value));
        return this;
    }

    public synchronized TrackMe set(@NonNull QueryParams key, float value) {
        set(key, Float.toString(value));
        return this;
    }

    public synchronized TrackMe set(@NonNull QueryParams key, long value) {
        set(key, Long.toString(value));
        return this;
    }

    public synchronized boolean has(@NonNull QueryParams queryParams) {
        return mQueryParams.containsKey(queryParams.toString());
    }

    /**
     * Only sets the value if it doesn't exist.
     *
     * @param key   type
     * @param value value
     * @return this (for chaining)
     */
    public synchronized TrackMe trySet(@NonNull QueryParams key, int value) {
        return trySet(key, String.valueOf(value));
    }

    /**
     * Only sets the value if it doesn't exist.
     *
     * @param key   type
     * @param value value
     * @return this (for chaining)
     */
    public synchronized TrackMe trySet(@NonNull QueryParams key, float value) {
        return trySet(key, String.valueOf(value));
    }

    /**
     * Only sets the value if it doesn't exist.
     *
     * @param key   type
     * @param value value
     * @return this (for chaining)
     */
    public synchronized TrackMe trySet(@NonNull QueryParams key, String value) {
        if (!has(key))
            set(key, value);
        return this;
    }

    /**
     * The tracker calls this to build the final query to be sent via HTTP
     *
     * @return the query, but without the base URL
     */
    public synchronized String build() {
        set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, mScreenCustomVariable.toString());
        return Dispatcher.urlEncodeUTF8(mQueryParams);
    }

    public synchronized String get(@NonNull QueryParams queryParams) {
        return mQueryParams.get(queryParams.toString());
    }

    /**
     * A custom variable is a custom name-value pair that you can assign to your users or screen views,
     * and then visualize the reports of how many visits, conversions, etc. for each custom variable.
     * A custom variable is defined by a name — for example,
     * "User status" — and a value – for example, "LoggedIn" or "Anonymous".
     * You can track up to 5 custom variables for each user to your app.
     *
     * @param index this Integer accepts values from 1 to 5.
     *              A given custom variable name must always be stored in the same "index" per session.
     *              For example, if you choose to store the variable name = "Gender" in
     *              index = 1 and you record another custom variable in index = 1, then the
     *              "Gender" variable will be deleted and replaced with the new custom variable stored in index 1.
     * @param name  String defines the name of a specific Custom Variable such as "User type".
     * @param value String defines the value of a specific Custom Variable such as "Customer".
     */
    public synchronized TrackMe setScreenCustomVariable(int index, String name, String value) {
        mScreenCustomVariable.put(index, name, value);
        return this;
    }

    public synchronized CustomVariables getScreenCustomVariable() {
        return mScreenCustomVariable;
    }

}
