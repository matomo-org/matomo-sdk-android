/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.piwik.sdk.dispatcher.Dispatcher;
import org.piwik.sdk.tools.DeviceHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Main tracking class
 * This class is threadsafe.
 */
public class Tracker {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "Tracker";

    // Piwik default parameter values
    private static final String DEFAULT_UNKNOWN_VALUE = "unknown";
    private static final String DEFAULT_TRUE_VALUE = "1";
    private static final String DEFAULT_RECORD_VALUE = DEFAULT_TRUE_VALUE;
    private static final String DEFAULT_API_VERSION_VALUE = "1";

    // Sharedpreference keys for persisted values
    protected static final String PREF_KEY_TRACKER_USERID = "tracker.userid";
    protected static final String PREF_KEY_TRACKER_FIRSTVISIT = "tracker.firstvisit";
    protected static final String PREF_KEY_TRACKER_VISITCOUNT = "tracker.visitcount";
    protected static final String PREF_KEY_TRACKER_PREVIOUSVISIT = "tracker.previousvisit";

    private final Piwik mPiwik;

    /**
     * Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     */
    private final URL mApiUrl;

    /**
     * The ID of the website we're tracking a visit/action for.
     */
    private final int mSiteId;
    private final String mAuthToken;
    private final Object mSessionLock = new Object();
    private final CustomVariables mVisitCustomVariable = new CustomVariables();
    private final Dispatcher mDispatcher;
    private final Random mRandomAntiCachingValue = new Random(new Date().getTime());
    private final TrackMe mDefaultTrackMe = new TrackMe();

    private String mLastEvent;
    private String mApplicationDomain;
    private long mSessionTimeout = 30 * 60 * 1000;
    private long mSessionStartTime;

    /**
     * Use Piwik.newTracker() method to create new trackers
     *
     * @param url       (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId    (required) id of site
     * @param authToken (optional) could be null
     * @param piwik     piwik object used to gain access to application params such as name, resolution or lang
     * @throws MalformedURLException
     */
    protected Tracker(@NonNull final String url, int siteId, String authToken, @NonNull Piwik piwik) throws MalformedURLException {

        String checkUrl = url;
        if (checkUrl.endsWith("piwik.php") || checkUrl.endsWith("piwik-proxy.php")) {
            mApiUrl = new URL(checkUrl);
        } else {
            if (!checkUrl.endsWith("/")) {
                checkUrl += "/";
            }
            mApiUrl = new URL(checkUrl + "piwik.php");
        }
        mPiwik = piwik;
        mSiteId = siteId;
        mAuthToken = authToken;

        mDispatcher = new Dispatcher(mPiwik, mApiUrl, authToken);

        String userId = getSharedPreferences().getString(PREF_KEY_TRACKER_USERID, null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            getSharedPreferences().edit().putString(PREF_KEY_TRACKER_USERID, userId).apply();
        }
        mDefaultTrackMe.set(QueryParams.USER_ID, userId);

        mDefaultTrackMe.set(QueryParams.SESSION_START, DEFAULT_TRUE_VALUE);

        String resolution = DEFAULT_UNKNOWN_VALUE;
        int[] res = DeviceHelper.getResolution(mPiwik.getContext());
        if (res != null)
            resolution = String.format("%sx%s", res[0], res[1]);
        mDefaultTrackMe.set(QueryParams.SCREEN_RESOLUTION, resolution);

        mDefaultTrackMe.set(QueryParams.USER_AGENT, DeviceHelper.getUserAgent());
        mDefaultTrackMe.set(QueryParams.LANGUAGE, DeviceHelper.getUserLanguage());
        mDefaultTrackMe.set(QueryParams.COUNTRY, DeviceHelper.getUserCountry());
        mDefaultTrackMe.set(QueryParams.VISITOR_ID, makeRandomVisitorId());
        mDefaultTrackMe.set(QueryParams.URL_PATH, fixUrl(null, getApplicationBaseURL()));
    }

    public Piwik getPiwik() {
        return mPiwik;
    }

    public URL getAPIUrl() {
        return mApiUrl;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    protected int getSiteId() {
        return mSiteId;
    }

    /**
     * Piwik will use the content of this object to fill in missing values before any transmission.
     * While you can modify it's values, you can also just set them in your {@link TrackMe} object as already set values will not be overwritten.
     *
     * @return the default TrackMe object
     */
    public TrackMe getDefaultTrackMe() {
        return mDefaultTrackMe;
    }

    public void startNewSession() {
        synchronized (mSessionLock) {
            mSessionStartTime = 0;
        }
    }

    public void setSessionTimeout(int milliseconds) {
        synchronized (mSessionLock) {
            mSessionTimeout = milliseconds;
        }
    }

    protected boolean tryNewSession() {
        synchronized (mSessionLock) {
            boolean expired = System.currentTimeMillis() - mSessionStartTime > mSessionTimeout;
            // Update the session timer
            mSessionStartTime = System.currentTimeMillis();
            return expired;
        }
    }

    /**
     * Default is 30min (30*60*1000).
     *
     * @return session timeout value in miliseconds
     */
    public long getSessionTimeout() {
        return mSessionTimeout;
    }

    /**
     * {@link Dispatcher#getConnectionTimeOut()}
     */
    public int getDispatchTimeout() {
        return mDispatcher.getConnectionTimeOut();
    }

    /**
     * {@link Dispatcher#setConnectionTimeOut(int)}
     */
    public void setDispatchTimeout(int timeout) {
        mDispatcher.setConnectionTimeOut(timeout);
    }

    /**
     * Processes all queued events in background thread
     *
     * @return true if there are any queued events and opt out is inactive
     */
    public boolean dispatch() {
        if (!mPiwik.isOptOut()) {
            mDispatcher.forceDispatch();
            return true;
        }
        return false;
    }

    /**
     * Set the interval to 0 to dispatch events as soon as they are queued.
     * If a negative value is used the dispatch timer will never run, a manual dispatch must be used.
     *
     * @param dispatchInterval in milliseconds
     */
    public Tracker setDispatchInterval(long dispatchInterval) {
        mDispatcher.setDispatchInterval(dispatchInterval);
        return this;
    }

    /**
     * @return in milliseconds
     */
    public long getDispatchInterval() {
        return mDispatcher.getDispatchInterval();
    }

    /**
     * Defines the User ID for this request.
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can
     * fetch this user ID from your system, and pass it to Piwik.
     * <p/>
     * When specified, the User ID will be "enforced".
     * This means that if there is no recent visit with this User ID, a new one will be created.
     * If a visit is found in the last 30 minutes with your specified User ID,
     * then the new action will be recorded to this existing visit.
     *
     * @param userId passing null will delete the current user-id.
     */
    public Tracker setUserId(String userId) {
        mDefaultTrackMe.set(QueryParams.USER_ID, userId);
        getSharedPreferences().edit().putString(PREF_KEY_TRACKER_USERID, userId).apply();
        return this;
    }

    /**
     * @return a user-id string, either the one you set or the one Piwik generated for you.
     */
    public String getUserId() {
        return mDefaultTrackMe.get(QueryParams.USER_ID);
    }

    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Piwik will still track visits, but the unique visitors metric might be less accurate.
     */
    public Tracker setVisitorId(String visitorId) throws IllegalArgumentException {
        if (confirmVisitorIdFormat(visitorId))
            mDefaultTrackMe.set(QueryParams.VISITOR_ID, visitorId);
        return this;
    }

    public String getVisitorId() {
        return mDefaultTrackMe.get(QueryParams.VISITOR_ID);
    }

    private static final Pattern PATTERN_VISITOR_ID = Pattern.compile("^[0-9a-f]{16}$");

    private boolean confirmVisitorIdFormat(String visitorId) throws IllegalArgumentException {
        Matcher visitorIdMatcher = PATTERN_VISITOR_ID.matcher(visitorId);
        if (visitorIdMatcher.matches()) {
            return true;
        }
        throw new IllegalArgumentException("VisitorId: " + visitorId + " is not of valid format, " +
                " the format must match the regular expression: " + PATTERN_VISITOR_ID.pattern());
    }

    /**
     * Domain used to build required parameter url (http://developer.piwik.org/api-reference/tracking-api)
     * If domain wasn't set `Application.getPackageName()` method will be used
     *
     * @param domain your-domain.com
     */
    public Tracker setApplicationDomain(String domain) {
        mApplicationDomain = domain;
        mDefaultTrackMe.set(QueryParams.URL_PATH, fixUrl(null, getApplicationBaseURL()));
        return this;
    }

    protected String getApplicationDomain() {
        return mApplicationDomain != null ? mApplicationDomain : mPiwik.getApplicationDomain();
    }

    /**
     * There parameters are only interesting for the very first query.
     */
    private void injectInitialParams(TrackMe trackMe) {
        long firstVisitTime;
        int visitCount;
        long previousVisit;

        // Protected against Trackers on other threads trying to do the same thing.
        // This works because they would use the same preference object.
        synchronized (getSharedPreferences()) {
            visitCount = 1 + getSharedPreferences().getInt(PREF_KEY_TRACKER_VISITCOUNT, 0);
            getSharedPreferences().edit().putInt(PREF_KEY_TRACKER_VISITCOUNT, visitCount).apply();
        }

        synchronized (getSharedPreferences()) {
            firstVisitTime = getSharedPreferences().getLong(PREF_KEY_TRACKER_FIRSTVISIT, -1);
            if (firstVisitTime == -1) {
                firstVisitTime = System.currentTimeMillis() / 1000;
                getSharedPreferences().edit().putLong(PREF_KEY_TRACKER_FIRSTVISIT, firstVisitTime).apply();
            }
        }

        synchronized (getSharedPreferences()) {
            previousVisit = getSharedPreferences().getLong(PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
            getSharedPreferences().edit().putLong(PREF_KEY_TRACKER_PREVIOUSVISIT, System.currentTimeMillis() / 1000).apply();
        }

        // trySet because the developer could have modded these after creating the Tracker
        mDefaultTrackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, firstVisitTime);
        mDefaultTrackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, visitCount);
        if (previousVisit != -1)
            mDefaultTrackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, previousVisit);

        trackMe.trySet(QueryParams.SESSION_START, mDefaultTrackMe.get(QueryParams.SESSION_START));
        trackMe.trySet(QueryParams.SCREEN_RESOLUTION, mDefaultTrackMe.get(QueryParams.SCREEN_RESOLUTION));
        trackMe.trySet(QueryParams.USER_AGENT, mDefaultTrackMe.get(QueryParams.USER_AGENT));
        trackMe.trySet(QueryParams.LANGUAGE, mDefaultTrackMe.get(QueryParams.LANGUAGE));
        trackMe.trySet(QueryParams.COUNTRY, mDefaultTrackMe.get(QueryParams.COUNTRY));
        trackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, mDefaultTrackMe.get(QueryParams.FIRST_VISIT_TIMESTAMP));
        trackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, mDefaultTrackMe.get(QueryParams.TOTAL_NUMBER_OF_VISITS));
        trackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, mDefaultTrackMe.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP));
    }

    /**
     * These parameters are required for all queries.
     */
    private void injectBaseParams(TrackMe trackMe) {
        trackMe.trySet(QueryParams.SITE_ID, mSiteId);
        trackMe.trySet(QueryParams.RECORD, DEFAULT_RECORD_VALUE);
        trackMe.trySet(QueryParams.API_VERSION, DEFAULT_API_VERSION_VALUE);
        trackMe.trySet(QueryParams.RANDOM_NUMBER, mRandomAntiCachingValue.nextInt(100000));
        trackMe.trySet(QueryParams.DATETIME_OF_REQUEST, new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(new Date()));
        trackMe.trySet(QueryParams.SEND_IMAGE, "0");

        trackMe.trySet(QueryParams.VISITOR_ID, mDefaultTrackMe.get(QueryParams.VISITOR_ID));
        trackMe.trySet(QueryParams.USER_ID, mDefaultTrackMe.get(QueryParams.USER_ID));

        trackMe.trySet(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, mVisitCustomVariable.toString());

        String urlPath = trackMe.get(QueryParams.URL_PATH);
        if (urlPath == null) {
            urlPath = mDefaultTrackMe.get(QueryParams.URL_PATH);
        } else {
            urlPath = fixUrl(urlPath, getApplicationBaseURL());
            mDefaultTrackMe.set(QueryParams.URL_PATH, urlPath);
        }
        trackMe.set(QueryParams.URL_PATH, urlPath);
    }

    private static String fixUrl(String url, String baseUrl) {
        if (url == null) url = baseUrl + "/";

        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://")) {
            url = baseUrl + (url.startsWith("/") ? "" : "/") + url;
        }
        return url;
    }

    private CountDownLatch mSessionStartLatch = new CountDownLatch(0);

    public Tracker track(TrackMe trackMe) {
        boolean newSession;
        synchronized (mSessionLock) {
            newSession = tryNewSession();
            if (newSession)
                mSessionStartLatch = new CountDownLatch(1);
        }
        if (newSession) {
            injectInitialParams(trackMe);
        } else {
            try {
                // Another thread is currently creating a sessions first transmission, wait until it's done.
                mSessionStartLatch.await(mDispatcher.getConnectionTimeOut(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        injectBaseParams(trackMe);
        String event = Dispatcher.urlEncodeUTF8(trackMe.toMap());
        if (mPiwik.isOptOut()) {
            mLastEvent = event;
            Timber.tag(LOGGER_TAG).d("URL omitted due to opt out: %s", event);
        } else {
            mDispatcher.submit(event);
            Timber.tag(LOGGER_TAG).d("URL added to the queue: %s", event);
        }

        // we did a first transmission, let the other through.
        if (newSession)
            mSessionStartLatch.countDown();

        return this;
    }

    public static String makeRandomVisitorId() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
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
    public Tracker setVisitCustomVariable(int index, String name, String value) {
        mVisitCustomVariable.put(index, name, value);
        return this;
    }

    public SharedPreferences getSharedPreferences() {
        return mPiwik.getSharedPreferences();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tracker tracker = (Tracker) o;
        return mSiteId == tracker.mSiteId && mApiUrl.equals(tracker.mApiUrl);
    }

    @Override
    public int hashCode() {
        int result = mSiteId;
        result = 31 * result + mApiUrl.hashCode();
        return result;
    }

    protected String getApplicationBaseURL() {
        return String.format("http://%s", getApplicationDomain());
    }

    /**
     * For testing purposes
     *
     * @return query of the event ?r=1&sideId=1..
     */
    @VisibleForTesting
    public String getLastEvent() {
        return mLastEvent;
    }

    @VisibleForTesting
    public void clearLastEvent() {
        mLastEvent = null;
    }

    @VisibleForTesting
    public Dispatcher getDispatcher() {
        return mDispatcher;
    }
}

