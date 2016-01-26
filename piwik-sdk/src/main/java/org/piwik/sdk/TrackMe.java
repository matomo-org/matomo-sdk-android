/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.support.annotation.NonNull;

import org.piwik.sdk.dispatcher.Dispatcher;

import java.util.HashMap;

/**
 * This objects represents one query to Piwik.
 * For each event send to Piwik a TrackMe gets created, either explicitly by you or implicitly by the Tracker.
 */
public class TrackMe {
    private static final int DEFAULT_QUERY_CAPACITY = 14;
    private final HashMap<String, String> mQueryParams = new HashMap<>(DEFAULT_QUERY_CAPACITY);
    private final CustomVariables mScreenCustomVariable = new CustomVariables();

    protected synchronized TrackMe set(@NonNull String key, String value) {
        if (value == null)
            mQueryParams.remove(key);
        else if (value.length() > 0)
            mQueryParams.put(key, value);
        return this;
    }

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
        set(key.toString(), value);
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

    public synchronized TrackMe trySet(@NonNull QueryParams key, long value) {
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
     * Just like {@link Tracker#setVisitCustomVariable(int, String, String)} but only valid per screen.
     * Only takes effect when setting prior to tracking the screen view.
     */
    public synchronized TrackMe setScreenCustomVariable(int index, String name, String value) {
        mScreenCustomVariable.put(index, name, value);
        return this;
    }

    public synchronized CustomVariables getScreenCustomVariable() {
        return mScreenCustomVariable;
    }

}
