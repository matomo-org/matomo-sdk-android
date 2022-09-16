/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk;

import android.content.SharedPreferences;

import org.matomo.sdk.dispatcher.DispatchMode;
import org.matomo.sdk.dispatcher.Dispatcher;
import org.matomo.sdk.dispatcher.Packet;
import org.matomo.sdk.tools.DeviceHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;


/**
 * Main tracking class
 * This class is threadsafe.
 */
@SuppressWarnings("WeakerAccess")
public class Tracker {
    private static final String TAG = Matomo.tag(Tracker.class);

    // Matomo default parameter values
    private static final String DEFAULT_UNKNOWN_VALUE = "unknown";
    private static final String DEFAULT_TRUE_VALUE = "1";
    private static final String DEFAULT_RECORD_VALUE = DEFAULT_TRUE_VALUE;
    private static final String DEFAULT_API_VERSION_VALUE = "1";

    // Sharedpreference keys for persisted values
    protected static final String PREF_KEY_TRACKER_OPTOUT = "tracker.optout";
    protected static final String PREF_KEY_TRACKER_USERID = "tracker.userid";
    protected static final String PREF_KEY_TRACKER_VISITORID = "tracker.visitorid";
    protected static final String PREF_KEY_TRACKER_FIRSTVISIT = "tracker.firstvisit";
    protected static final String PREF_KEY_TRACKER_VISITCOUNT = "tracker.visitcount";
    protected static final String PREF_KEY_TRACKER_PREVIOUSVISIT = "tracker.previousvisit";
    protected static final String PREF_KEY_OFFLINE_CACHE_AGE = "tracker.cache.age";
    protected static final String PREF_KEY_OFFLINE_CACHE_SIZE = "tracker.cache.size";
    protected static final String PREF_KEY_DISPATCHER_MODE = "tracker.dispatcher.mode";

    private static final Pattern VALID_URLS = Pattern.compile("^(\\w+)(?:://)(.+?)$");

    private final Matomo mMatomo;
    private final String mApiUrl;
    private final int mSiteId;
    private final String mDefaultApplicationBaseUrl;
    private final Object mTrackingLock = new Object();
    private final Dispatcher mDispatcher;
    private final String mName;
    private final Random mRandomAntiCachingValue = new Random(new Date().getTime());
    private final TrackMe mDefaultTrackMe = new TrackMe();

    private TrackMe mLastEvent;
    private long mSessionTimeout = 30 * 60 * 1000;
    private long mSessionStartTime = 0;
    private boolean mOptOut;
    private SharedPreferences mPreferences;

    private final LinkedHashSet<Callback> mTrackingCallbacks = new LinkedHashSet<>();
    private DispatchMode mDispatchMode;

    protected Tracker(Matomo matomo, TrackerBuilder config) {
        mMatomo = matomo;
        mApiUrl = config.getApiUrl();
        mSiteId = config.getSiteId();
        mName = config.getTrackerName();
        mDefaultApplicationBaseUrl = config.getApplicationBaseUrl();

        new LegacySettingsPorter(mMatomo).port(this);

        mOptOut = getPreferences().getBoolean(PREF_KEY_TRACKER_OPTOUT, false);

        mDispatcher = mMatomo.getDispatcherFactory().build(this);
        mDispatcher.setDispatchMode(getDispatchMode());

        String userId = getPreferences().getString(PREF_KEY_TRACKER_USERID, null);
        mDefaultTrackMe.set(QueryParams.USER_ID, userId);

        String visitorId = getPreferences().getString(PREF_KEY_TRACKER_VISITORID, null);
        if (visitorId == null) {
            visitorId = makeRandomVisitorId();
            getPreferences().edit().putString(PREF_KEY_TRACKER_VISITORID, visitorId).apply();
        }
        mDefaultTrackMe.set(QueryParams.VISITOR_ID, visitorId);

        mDefaultTrackMe.set(QueryParams.SESSION_START, DEFAULT_TRUE_VALUE);

        DeviceHelper deviceHelper = mMatomo.getDeviceHelper();

        String resolution = DEFAULT_UNKNOWN_VALUE;
        int[] res = deviceHelper.getResolution();
        if (res != null) resolution = String.format("%sx%s", res[0], res[1]);
        mDefaultTrackMe.set(QueryParams.SCREEN_RESOLUTION, resolution);

        mDefaultTrackMe.set(QueryParams.USER_AGENT, deviceHelper.getUserAgent());
        mDefaultTrackMe.set(QueryParams.LANGUAGE, deviceHelper.getUserLanguage());
        mDefaultTrackMe.set(QueryParams.URL_PATH, config.getApplicationBaseUrl());
    }

    public void addTrackingCallback(Callback callback) {
        this.mTrackingCallbacks.add(callback);
    }

    public void removeTrackingCallback(Callback callback) {
        this.mTrackingCallbacks.remove(callback);
    }

    public void reset() {
        dispatch();

        String visitorId = makeRandomVisitorId();

        SharedPreferences prefs = getPreferences();

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (prefs) {
            SharedPreferences.Editor editor = mPreferences.edit();

            editor.remove(PREF_KEY_TRACKER_VISITCOUNT);
            editor.remove(PREF_KEY_TRACKER_PREVIOUSVISIT);
            editor.remove(PREF_KEY_TRACKER_FIRSTVISIT);
            editor.remove(PREF_KEY_TRACKER_USERID);
            editor.remove(PREF_KEY_TRACKER_OPTOUT);

            editor.putString(PREF_KEY_TRACKER_VISITORID, visitorId);

            editor.apply();
        }

        mDefaultTrackMe.set(QueryParams.VISITOR_ID, visitorId);
        mDefaultTrackMe.set(QueryParams.USER_ID, null);
        mDefaultTrackMe.set(QueryParams.FIRST_VISIT_TIMESTAMP, null);
        mDefaultTrackMe.set(QueryParams.TOTAL_NUMBER_OF_VISITS, null);
        mDefaultTrackMe.set(QueryParams.PREVIOUS_VISIT_TIMESTAMP, null);
        mDefaultTrackMe.set(QueryParams.SESSION_START, DEFAULT_TRUE_VALUE);
        mDefaultTrackMe.set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, null);
        mDefaultTrackMe.set(QueryParams.CAMPAIGN_NAME, null);
        mDefaultTrackMe.set(QueryParams.CAMPAIGN_KEYWORD, null);

        startNewSession();
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
     * @return true if Matomo is currently disabled
     */
    public boolean isOptOut() {
        return mOptOut;
    }

    public String getName() {
        return mName;
    }

    public Matomo getMatomo() {
        return mMatomo;
    }

    public String getAPIUrl() {
        return mApiUrl;
    }

    protected int getSiteId() {
        return mSiteId;
    }

    /**
     * Matomo will use the content of this object to fill in missing values before any transmission.
     * While you can modify it's values, you can also just set them in your {@link TrackMe} object as already set values will not be overwritten.
     *
     * @return the default TrackMe object
     */
    public TrackMe getDefaultTrackMe() {
        return mDefaultTrackMe;
    }

    public void startNewSession() {
        synchronized (mTrackingLock) {
            mSessionStartTime = 0;
        }
    }

    public void setSessionTimeout(int milliseconds) {
        synchronized (mTrackingLock) {
            mSessionTimeout = milliseconds;
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
     * Process all queued events and block until processing is complete
     */
    public void dispatchBlocking() {
        if (mOptOut) return;
        mDispatcher.forceDispatchBlocking();
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
     * The Matomo backend accepts backdated events for up to 24 hours by default.
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
     * The Matomo backend accepts backdated events for up to 24 hours by default.
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
        if (mDispatchMode == null) {
            String raw = getPreferences().getString(PREF_KEY_DISPATCHER_MODE, null);
            mDispatchMode = DispatchMode.fromString(raw);
            if (mDispatchMode == null) mDispatchMode = DispatchMode.ALWAYS;
        }
        return mDispatchMode;
    }

    /**
     * Sets the dispatch mode.
     *
     * @see DispatchMode
     */
    public void setDispatchMode(DispatchMode mode) {
        mDispatchMode = mode;
        if (mode != DispatchMode.EXCEPTION) {
            getPreferences().edit().putString(PREF_KEY_DISPATCHER_MODE, mode.toString()).apply();
        }
        mDispatcher.setDispatchMode(mode);
    }

    /**
     * Defines the User ID for this request.
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can
     * fetch this user ID from your system, and pass it to Matomo.
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
     * @return a user-id string, either the one you set or the one Matomo generated for you.
     */
    public String getUserId() {
        return mDefaultTrackMe.get(QueryParams.USER_ID);
    }

    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Matomo will still track visits, but the unique visitors metric might be less accurate.
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
     * There parameters are only interesting for the very first query.
     */
    private void injectInitialParams(TrackMe trackMe) {
        long firstVisitTime;
        long visitCount;
        long previousVisit;

        SharedPreferences prefs = getPreferences();
        // Protected against Trackers on other threads trying to do the same thing.
        // This works because they would use the same preference object.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (prefs) {
            SharedPreferences.Editor editor = prefs.edit();
            visitCount = 1 + getPreferences().getLong(PREF_KEY_TRACKER_VISITCOUNT, 0);
            editor.putLong(PREF_KEY_TRACKER_VISITCOUNT, visitCount);

            firstVisitTime = prefs.getLong(PREF_KEY_TRACKER_FIRSTVISIT, -1);
            if (firstVisitTime == -1) {
                firstVisitTime = System.currentTimeMillis() / 1000;
                editor.putLong(PREF_KEY_TRACKER_FIRSTVISIT, firstVisitTime);
            }

            previousVisit = prefs.getLong(PREF_KEY_TRACKER_PREVIOUSVISIT, -1);
            editor.putLong(PREF_KEY_TRACKER_PREVIOUSVISIT, System.currentTimeMillis() / 1000);

            editor.apply();
        }

        // trySet because the developer could have modded these after creating the Tracker
        mDefaultTrackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, firstVisitTime);
        mDefaultTrackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, visitCount);

        if (previousVisit != -1) mDefaultTrackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, previousVisit);

        trackMe.trySet(QueryParams.SESSION_START, mDefaultTrackMe.get(QueryParams.SESSION_START));
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

        trackMe.trySet(QueryParams.SCREEN_RESOLUTION, mDefaultTrackMe.get(QueryParams.SCREEN_RESOLUTION));
        trackMe.trySet(QueryParams.USER_AGENT, mDefaultTrackMe.get(QueryParams.USER_AGENT));
        trackMe.trySet(QueryParams.LANGUAGE, mDefaultTrackMe.get(QueryParams.LANGUAGE));

        String urlPath = trackMe.get(QueryParams.URL_PATH);
        if (urlPath == null) {
            urlPath = mDefaultTrackMe.get(QueryParams.URL_PATH);
        } else if (!VALID_URLS.matcher(urlPath).matches()) {
            StringBuilder urlBuilder = new StringBuilder(mDefaultApplicationBaseUrl);
            if (!mDefaultApplicationBaseUrl.endsWith("/") && !urlPath.startsWith("/")) {
                urlBuilder.append("/");
            } else if (mDefaultApplicationBaseUrl.endsWith("/") && urlPath.startsWith("/")) {
                urlPath = urlPath.substring(1);
            }
            urlPath = urlBuilder.append(urlPath).toString();
        }

        // https://github.com/matomo-org/matomo-sdk-android/issues/92
        mDefaultTrackMe.set(QueryParams.URL_PATH, urlPath);
        trackMe.set(QueryParams.URL_PATH, urlPath);
    }

    public Tracker track(TrackMe trackMe) {
        synchronized (mTrackingLock) {
            final boolean newSession = System.currentTimeMillis() - mSessionStartTime > mSessionTimeout;

            if (newSession) {
                mSessionStartTime = System.currentTimeMillis();
                injectInitialParams(trackMe);
            }

            injectBaseParams(trackMe);

            for (Callback callback : mTrackingCallbacks) {
                trackMe = callback.onTrack(trackMe);
                if (trackMe == null) {
                    Timber.tag(TAG).d("Tracking aborted by %s", callback);
                    return this;
                }
            }

            mLastEvent = trackMe;
            if (!mOptOut) {
                mDispatcher.submit(trackMe);
                Timber.tag(TAG).d("Event added to the queue: %s", trackMe);
            } else {
                Timber.tag(TAG).d("Event omitted due to opt out: %s", trackMe);
            }

            return this;
        }
    }

    public static String makeRandomVisitorId() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }


    public SharedPreferences getPreferences() {
        if (mPreferences == null) mPreferences = mMatomo.getTrackerPreferences(this);
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

    public interface Callback {
        /**
         * This method will be called after parameter injection and before transmission within {@link Tracker#track(TrackMe)}.
         * Blocking within this method will block tracking.
         *
         * @param trackMe The `TrackMe` that was passed to {@link Tracker#track(TrackMe)} after all data has been injected.
         * @return The `TrackMe` that will be send, returning NULL here will abort transmission.
         */
        @Nullable
        TrackMe onTrack(TrackMe trackMe);
    }
}
