/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.net.URL;

/**
 * Data that can be send to the backend API via the Dispatcher
 */
@VisibleForTesting
public class Packet {
    private final URL mTargetURL;
    private final JSONObject mJSONObject;
    private final long mTimeStamp;

    /**
     * Constructor for GET requests
     */
    public Packet(@NonNull URL targetURL) {
        this(targetURL, null);
    }

    /**
     * Constructor for POST requests
     */
    public Packet(@NonNull URL targetURL, @Nullable JSONObject JSONObject) {
        mTargetURL = targetURL;
        mJSONObject = JSONObject;
        mTimeStamp = System.currentTimeMillis();
    }

    @NonNull
    public URL getTargetURL() {
        return mTargetURL;
    }

    /**
     * @return may be null if it is a GET request
     */
    @Nullable
    public JSONObject getJSONObject() {
        return mJSONObject;
    }

    /**
     * A timestamp to use when replaying offline data
     */
    public long getTimeStamp() {
        return mTimeStamp;
    }
}
