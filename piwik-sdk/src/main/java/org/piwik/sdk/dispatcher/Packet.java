/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.net.URL;

/**
 * Data that can be send to the backend API via the Dispatcher
 */
public class Packet {
    private final URL mTargetURL;
    private final JSONObject mPostData;
    private final long mTimeStamp;
    private final int mEventCount;

    /**
     * Constructor for GET requests
     */
    public Packet(URL targetURL) {
        this(targetURL, null, 1);
    }

    /**
     * Constructor for POST requests
     *
     * @param targetURL  server
     * @param JSONObject non null if HTTP POST packet
     * @param eventCount number of events in this packet
     */
    public Packet(URL targetURL, @Nullable JSONObject JSONObject, int eventCount) {
        mTargetURL = targetURL;
        mPostData = JSONObject;
        mEventCount = eventCount;
        mTimeStamp = System.currentTimeMillis();
    }

    public URL getTargetURL() {
        return mTargetURL;
    }

    /**
     * @return may be null if it is a GET request
     */
    @Nullable
    public JSONObject getPostData() {
        return mPostData;
    }

    /**
     * A timestamp to use when replaying offline data
     */
    public long getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * Used to determine the event cache queue positions.
     *
     * @return how many events this packet contains
     */
    public int getEventCount() {
        return mEventCount;
    }
}
