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

import org.piwik.sdk.dispatcher.DispatchMode;
import org.piwik.sdk.dispatcher.Dispatcher;
import org.piwik.sdk.dispatcher.Packet;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import timber.log.Timber;

import static android.content.ContentValues.TAG;


/**
 * Main tracking class
 * This class is threadsafe.
 */
public class Tracker {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "Tracker";

    // Piwik default parameter values
    private static final String DEFAULT_UNKNOWN_VALUE = "unknown";
    private static final String DEFAULT_TRUE_VALUE = "1";
    private static final String DEFAULT_RECORD_VALUE = DEFAULT_TRUE_VALUE;
    private static final String DEFAULT_API_VERSION_VALUE = "1";

    // Sharedpreference keys for persisted values
    protected static final String PREF_KEY_TRACKER_OPTOUT = "tracker.optout";
    protected static final String PREF_KEY_TRACKER_USERID = "tracker.userid";
    protected static final String PREF_KEY_TRACKER_FIRSTVISIT = "tracker.firstvisit";
    protected static final String PREF_KEY_TRACKER_VISITCOUNT = "tracker.visitcount";
    protected static final String PREF_KEY_TRACKER_PREVIOUSVISIT = "tracker.previousvisit";
    protected static final String PREF_KEY_OFFLINE_CACHE_AGE = "tracker.cache.age";
    protected static final String PREF_KEY_OFFLINE_CACHE_SIZE = "tracker.cache.size";
    protected static final String PREF_KEY_DISPATCHER_MODE = "tracker.dispatcher.mode";

    private final Piwik mPiwik;

    /**
     * Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     */
    private final URL mApiUrl;

    /**
     * The ID of the website we're tracking a visit/action for.
     */
    private final int mSiteId;
    private final Object mSessionLock = new Object();
    private final Dispatcher mDispatcher;
    private final String mName;
    private final Random mRandomAntiCachingValue = new Random(new Date().getTime());
    private final TrackMe mDefaultTrackMe = new TrackMe();

    private TrackMe mLastEvent;
    private String mApplicationDomain;
    private long mSessionTimeout = 30 * 60 * 1000;
    private long mSessionStartTime;
    private boolean mOptOut = false;
    private SharedPreferences mPreferences;

    /**
     * Use Piwik.newTracker() method to create new trackers
     *
     * @param piwik         piwik object used to gain access to application params such as name, resolution or lang
     * @param trackerConfig configuration for this Tracker.
     * @throws RuntimeException if the supplied Piwik-Tracker URL is incompatible
     */
    public Tracker(@NonNull Piwik piwik, @NonNull TrackerConfig trackerConfig) {
        mPiwik = piwik;
        mApiUrl = trackerConfig.getApiUrl();
        mSiteId = trackerConfig.getSiteId();
        mName = trackerConfig.getTrackerName();

        new LegacySettingsPorter(piwik).port(this);

        mOptOut = getPreferences().getBoolean(PREF_KEY_TRACKER_OPTOUT, false);

        mDispatcher = piwik.getDispatcherFactory().build(this);
        String userId = getPreferences().getString(PREF_KEY_TRACKER_USERID, null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            getPreferences().edit().putString(PREF_KEY_TRACKER_USERID, userId).apply();
        }
        mDefaultTrackMe.set(QueryParams.USER_ID, userId);

        mDefaultTrackMe.set(QueryParams.SESSION_START, DEFAULT_TRUE_VALUE);

        String resolution = DEFAULT_UNKNOWN_VALUE;
        int[] res = mPiwik.getDeviceHelper().getResolution();
        if (res != null) resolution = String.format("%sx%s", res[0], res[1]);
        mDefaultTrackMe.set(QueryParams.SCREEN_RESOLUTION, resolution);

        mDefaultTrackMe.set(QueryParams.USER_AGENT, mPiwik.getDeviceHelper().getUserAgent());
        mDefaultTrackMe.set(QueryParams.LANGUAGE, mPiwik.getDeviceHelper().getUserLanguage());
        mDefaultTrackMe.set(QueryParams.VISITOR_ID, makeRandomVisitorId());
        mDefaultTrackMe.set(QueryParams.URL_PATH, fixUrl(null, getApplicationBaseURL()));
    }

    /**
     * Use this to disable this Tracker, e.g. if the user opted out of tracking.
     * The Tracker will persist the choice and remain disable on next instance creation.<p>
     *
     * @param optOut true to disable reporting
     */
    public void setOptOut(boolean optOut) {
        mOptOut = optOut;
        getPreferences().edit().putBoolean(PREF_KEY_TRACKER_OPTOUT, optOut).apply();
        mDispatcher.clear();
    }

    /**
     * @return true if Piwik is currently disabled
     */
    public boolean isOptOut() {
        return mOptOut;
    }

    public String getName() {
        return mName;
    }

    public Piwik getPiwik() {
        return mPiwik;
    }

    public URL getAPIUrl() {
        return mApiUrl;
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
     */
    public void dispatch() {
        if (mOptOut) return;
        mDispatcher.forceDispatch();
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
     * Defines if when dispatched, posted JSON must be Gzipped.
     * Need to be handle from web server side with mod_deflate/APACHE lua_zlib/NGINX.
     *
     * @param dispatchGzipped boolean
     */
    public Tracker setDispatchGzipped(boolean dispatchGzipped) {
        mDispatcher.setDispatchGzipped(dispatchGzipped);
        return this;
    }

    /**
     * @return in milliseconds
     */
    public long getDispatchInterval() {
        return mDispatcher.getDispatchInterval();
    }

    /**
     * For how long events should be stored if they could not be send.
     * Events older than the set limit will be discarded on the next dispatch attempt.<br>
     * The Piwik backend accepts backdated events for up to 24 hours by default.
     * <p>
     * &gt;0 = limit in ms<br>
     * 0 = unlimited<br>
     * -1 = disabled offline cache<br>
     *
     * @param age in milliseconds
     */
    public void setOfflineCacheAge(long age) {
        getPreferences().edit().putLong(PREF_KEY_OFFLINE_CACHE_AGE, age).apply();
    }

    /**
     * See {@link #setOfflineCacheAge(long)}
     *
     * @return maximum cache age in milliseconds
     */
    public long getOfflineCacheAge() {
        return getPreferences().getLong(PREF_KEY_OFFLINE_CACHE_AGE, 24 * 60 * 60 * 1000);
    }

    /**
     * How large the offline cache may be.
     * If the limit is reached the oldest files will be deleted first.
     * Events older than the set limit will be discarded on the next dispatch attempt.<br>
     * The Piwik backend accepts backdated events for up to 24 hours by default.
     * <p>
     * &gt;0 = limit in byte<br>
     * 0 = unlimited<br>
     *
     * @param size in byte
     */
    public void setOfflineCacheSize(long size) {
        getPreferences().edit().putLong(PREF_KEY_OFFLINE_CACHE_SIZE, size).apply();
    }

    /**
     * Maximum size the offline cache is allowed to grow to.
     *
     * @return size in byte
     */
    public long getOfflineCacheSize() {
        return getPreferences().getLong(PREF_KEY_OFFLINE_CACHE_SIZE, 4 * 1024 * 1024);
    }

    /**
     * The current dispatch behavior.
     *
     * @see DispatchMode
     */
    public DispatchMode getDispatchMode() {
        String raw = getPreferences().getString(PREF_KEY_DISPATCHER_MODE, null);
        DispatchMode mode = DispatchMode.fromString(raw);
        if (mode == null) {
            mode = DispatchMode.ALWAYS;
            setDispatchMode(mode);
        }
        return mode;
    }

    /**
     * Sets the dispatch mode.
     *
     * @see DispatchMode
     */
    public void setDispatchMode(DispatchMode mode) {
        getPreferences().edit().putString(PREF_KEY_DISPATCHER_MODE, mode.toString()).apply();
        mDispatcher.setDispatchMode(mode);
    }

    /**
     * Defines the User ID for this request.
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can
     * fetch this user ID from your system, and pass it to Piwik.
     * <p>
     * When specified, the User ID will be "enforced".
     * This means that if there is no recent visit with this User ID, a new one will be created.
     * If a visit is found in the last 30 minutes with your specified User ID,
     * then the new action will be recorded to this existing visit.
     *
     * @param userId passing null will delete the current user-id.
     */
    public Tracker setUserId(String userId) {
        mDefaultTrackMe.set(QueryParams.USER_ID, userId);
        getPreferences().edit().putString(PREF_KEY_TRACKER_USERID, userId).apply();
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
        if (confirmVisitorIdFormat(visitorId)) mDefaultTrackMe.set(QueryParams.VISITOR_ID, visitorId);
        return this;
    }

    public String getVisitorId() {
        return mDefaultTrackMe.get(QueryParams.VISITOR_ID);
    }

    private static final Pattern PATTERN_VISITOR_ID = Pattern.compile("^[0-9a-f]{16}$");

    private boolean confirmVisitorIdFormat(String visitorId) throws IllegalArgumentException {
        if (PATTERN_VISITOR_ID.matcher(visitorId).matches()) return true;

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
        long visitCount;
        long previousVisit;

        // Protected against Trackers on other threads trying to do the same thing.
        // This works because they would use the same preference object.
        synchronized (getPreferences()) {
            visitCount = 1 + getPreferences().getLong(PREF_KEY_TRACKER_VISITCOUNT, 0);
            getPreferences().edit().putLong(PREF_KEY_TRACKER_VISITCOUNT, visitCount).apply();
        }

        synchronized (getPreferences()) {
            firstVisitTime = getPreferences().getLong(PREF_KEY_TRACKER_FIRSTVISIT, -1);
            if (firstVisitTime == -1) {
                firstVisitTime = System.currentTimeMillis() / 1000;
                getPreferences().edit().putLong(PREF_KEY_TRACKER_FIRSTVISIT, firstVisitTime).apply();
            }
        }

        synchronized (getPreferences()) {
            previousVisit = getPreferences().getLong(PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
            getPreferences().edit().putLong(PREF_KEY_TRACKER_PREVIOUSVISIT, System.currentTimeMillis() / 1000).apply();
        }

        // trySet because the developer could have modded these after creating the Tracker
        mDefaultTrackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, firstVisitTime);
        mDefaultTrackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, visitCount);

        if (previousVisit != -1) mDefaultTrackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, previousVisit);

        trackMe.trySet(QueryParams.SESSION_START, mDefaultTrackMe.get(QueryParams.SESSION_START));
        trackMe.trySet(QueryParams.SCREEN_RESOLUTION, mDefaultTrackMe.get(QueryParams.SCREEN_RESOLUTION));
        trackMe.trySet(QueryParams.USER_AGENT, mDefaultTrackMe.get(QueryParams.USER_AGENT));
        trackMe.trySet(QueryParams.LANGUAGE, mDefaultTrackMe.get(QueryParams.LANGUAGE));
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
        trackMe.trySet(QueryParams.DATETIME_OF_REQUEST, new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US).format(new Date()));
        trackMe.trySet(QueryParams.SEND_IMAGE, "0");

        trackMe.trySet(QueryParams.VISITOR_ID, mDefaultTrackMe.get(QueryParams.VISITOR_ID));
        trackMe.trySet(QueryParams.USER_ID, mDefaultTrackMe.get(QueryParams.USER_ID));

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
            if (newSession) mSessionStartLatch = new CountDownLatch(1);
        }
        if (newSession) {
            injectInitialParams(trackMe);
        } else {
            try {
                // Another thread might be creating a sessions first transmission.
                mSessionStartLatch.await(getDispatchTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) { Timber.tag(TAG).e(e, null); }
        }

        injectBaseParams(trackMe);

        mLastEvent = trackMe;
        if (!mOptOut) {
            mDispatcher.submit(trackMe);
            Timber.tag(LOGGER_TAG).d("Event added to the queue: %s", trackMe);
        } else Timber.tag(LOGGER_TAG).d("Event omitted due to opt out: %s", trackMe);

        // we did a first transmission, let the other through.
        if (newSession) mSessionStartLatch.countDown();

        return this;
    }

    public static String makeRandomVisitorId() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }


    public SharedPreferences getPreferences() {
        if (mPreferences == null) mPreferences = mPiwik.getTrackerPreferences(this);
        return mPreferences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tracker tracker = (Tracker) o;

        if (mSiteId != tracker.mSiteId) return false;
        if (!mApiUrl.equals(tracker.mApiUrl)) return false;
        return mName.equals(tracker.mName);

    }

    @Override
    public int hashCode() {
        int result = mApiUrl.hashCode();
        result = 31 * result + mSiteId;
        result = 31 * result + mName.hashCode();
        return result;
    }

    protected String getApplicationBaseURL() {
        return String.format("http://%s", getApplicationDomain());
    }

    /**
     * For testing purposes
     *
     * @return query of the event
     */
    @VisibleForTesting
    public TrackMe getLastEventX() {
        return mLastEvent;
    }

    /**
     * Set a data structure here to put the Dispatcher into dry-run-mode.
     * Data will be processed but at the last step just stored instead of transmitted.
     * Set it to null to disable it.
     *
     * @param dryRunTarget a data structure the data should be passed into
     */
    public void setDryRunTarget(List<Packet> dryRunTarget) {
        mDispatcher.setDryRunTarget(dryRunTarget);
    }

    /**
     * If we are in dry-run mode then this will return a datastructure.
     *
     * @return a datastructure or null
     */
    public List<Packet> getDryRunTarget() {
        return mDispatcher.getDryRunTarget();
    }
}

