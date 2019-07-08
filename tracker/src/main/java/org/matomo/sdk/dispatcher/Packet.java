/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * Data that can be send to the backend API via the Dispatcher
 */
public class Packet {
    private final String mTargetURL;
    private final JSONObject mPostData;
    private final long mTimeStamp;
    private final int mEventCount;
    private final String mTargetCookie;

    /**
     * Constructor for GET requests
     */
    public Packet(String targetURL, @Nullable String targetCookie) {
        this(targetURL, targetCookie, null, 1);
    }

    /**
     * Constructor for POST requests
     *
     * @param targetURL  server
     * @param JSONObject non null if HTTP POST packet
     * @param eventCount number of events in this packet
     */
    public Packet(String targetURL, @Nullable String targetCookie, @Nullable JSONObject JSONObject, int eventCount) {
        mTargetURL = targetURL;
        mTargetCookie = targetCookie;
        mPostData = JSONObject;
        mEventCount = eventCount;
        mTimeStamp = System.currentTimeMillis();
    }

    public String getTargetURL() {
        return mTargetURL;
    }

    @Nullable
    public String getTargetCookie() {
        return mTargetCookie;
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

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Packet(");
        if (mPostData != null) sb.append("type=POST, data=").append(mPostData);
        else sb.append("type=GET, data=").append(mTargetURL);
        return sb.append(")").toString();
    }
}
