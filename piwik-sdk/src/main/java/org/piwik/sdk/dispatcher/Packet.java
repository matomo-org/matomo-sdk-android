/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

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
    public Packet(@NonNull URL targetURL) {
        this(targetURL, null, 1);
    }

    /**
     * Constructor for POST requests
     */
    public Packet(@NonNull URL targetURL, @Nullable JSONObject JSONObject, int eventCount) {
        mTargetURL = targetURL;
        mPostData = JSONObject;
        mEventCount = eventCount;
        mTimeStamp = System.currentTimeMillis();
    }

    @NonNull
    protected URL getTargetURL() {
        return mTargetURL;
    }

    @NonNull
    URLConnection openConnection() throws IOException {
        return mTargetURL.openConnection();
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

    public int getEventCount() {
        return mEventCount;
    }
}
