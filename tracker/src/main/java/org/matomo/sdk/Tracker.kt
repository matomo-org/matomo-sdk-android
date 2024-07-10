/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import org.matomo.sdk.dispatcher.DispatchMode
import org.matomo.sdk.dispatcher.Dispatcher
import org.matomo.sdk.dispatcher.Packet
import org.matomo.sdk.tools.DeviceHelper
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.UUID
import java.util.regex.Pattern

/**
 * Main tracking class
 * This class is threadsafe.
 */
class Tracker(val matomo: Matomo, config: TrackerBuilder) {
    val aPIUrl: String = config.apiUrl
    val siteId: Int = config.siteId
    private val defaultApplicationBaseUrl: String = config.applicationBaseUrl
    private val trackingLock = Any()
    private val localDispatcher: Dispatcher
    val name: String = config.trackerName
    private val mRandomAntiCachingValue = Random(Date().time)

    /**
     * Matomo will use the content of this object to fill in missing values before any transmission.
     * While you can modify it's values, you can also just set them in your [TrackMe] object as already set values will not be overwritten.
     *
     * @return the default TrackMe object
     */
    val defaultTrackMe: TrackMe = TrackMe()

    /**
     * For testing purposes
     *
     * @return query of the event
     */
    @get:VisibleForTesting
    var lastEventX: TrackMe? = null
        private set

    /**
     * Default is 30min (30*60*1000).
     *
     * @return session timeout value in miliseconds
     */
    var sessionTimeout: Long = (30 * 60 * 1000).toLong()
        private set
    private var sessionStartTime: Long = 0
    private var localOptOut: Boolean
    private var mPreferences: SharedPreferences? = null

    private val mTrackingCallbacks = LinkedHashSet<Callback>()
    private var localDispatchMode: DispatchMode? = null

    fun addTrackingCallback(callback: Callback) {
        mTrackingCallbacks.add(callback)
    }

    fun removeTrackingCallback(callback: Callback) {
        mTrackingCallbacks.remove(callback)
    }

    fun reset() {
        dispatch()

        val visitorId = makeRandomVisitorId()

        preferences?.let {
            val prefs: SharedPreferences = it

            synchronized(prefs) {
                val editor: SharedPreferences.Editor = it.edit()
                editor.remove(PREF_KEY_TRACKER_VISITCOUNT)
                editor.remove(PREF_KEY_TRACKER_PREVIOUSVISIT)
                editor.remove(PREF_KEY_TRACKER_FIRSTVISIT)
                editor.remove(PREF_KEY_TRACKER_USERID)
                editor.remove(PREF_KEY_TRACKER_OPTOUT)

                editor.putString(PREF_KEY_TRACKER_VISITORID, visitorId)
                editor.apply()
            }

        }
        defaultTrackMe[QueryParams.VISITOR_ID] = visitorId
        defaultTrackMe[QueryParams.USER_ID] = null
        defaultTrackMe[QueryParams.FIRST_VISIT_TIMESTAMP] = null
        defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS] = null
        defaultTrackMe[QueryParams.PREVIOUS_VISIT_TIMESTAMP] = null
        defaultTrackMe[QueryParams.SESSION_START] = DEFAULT_TRUE_VALUE
        defaultTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES] = null
        defaultTrackMe[QueryParams.CAMPAIGN_NAME] = null
        defaultTrackMe[QueryParams.CAMPAIGN_KEYWORD] = null
        startNewSession()
    }

    var isOptOut: Boolean
        /**
         * @return true if Matomo is currently disabled
         */
        get() = localOptOut
        /**
         * Use this to disable this Tracker, e.g. if the user opted out of tracking.
         * The Tracker will persist the choice and remain disable on next instance creation.
         *
         *
         *
         * @param optOut true to disable reporting
         */
        set(optOut) {
            localOptOut = optOut
            preferences?.edit()?.putBoolean(PREF_KEY_TRACKER_OPTOUT, optOut)?.apply()
        }

    fun startNewSession() {
        synchronized(trackingLock) {
            sessionStartTime = 0
        }
    }

    fun setSessionTimeout(milliseconds: Int) {
        synchronized(trackingLock) {
            sessionTimeout = milliseconds.toLong()
        }
    }

    var dispatchTimeout: Int
        /**
         * [Dispatcher.getConnectionTimeOut]
         */
        get() = localDispatcher.connectionTimeOut
        /**
         * [Dispatcher.setConnectionTimeOut]
         */
        set(timeout) {
            localDispatcher.connectionTimeOut = timeout
        }

    /**
     * Processes all queued events in background thread
     */
    fun dispatch() {
        if (localOptOut) return
        localDispatcher.forceDispatch()
    }

    /**
     * Process all queued events and block until processing is complete
     */
    fun dispatchBlocking() {
        if (localOptOut) return
        localDispatcher.forceDispatchBlocking()
    }

    /**
     * Set the interval to 0 to dispatch events as soon as they are queued.
     * If a negative value is used the dispatch timer will never run, a manual dispatch must be used.
     *
     * @param dispatchInterval in milliseconds
     */
    fun setDispatchInterval(dispatchInterval: Long): Tracker {
        localDispatcher.dispatchInterval = dispatchInterval
        return this
    }

    /**
     * Defines if when dispatched, posted JSON must be Gzipped.
     * Need to be handle from web server side with mod_deflate/APACHE lua_zlib/NGINX.
     *
     * @param dispatchGzipped boolean
     */
    fun setDispatchGzipped(dispatchGzipped: Boolean): Tracker {
        localDispatcher.dispatchGzipped = dispatchGzipped
        return this
    }

    val dispatchInterval: Long
        /**
         * @return in milliseconds
         */
        get() = localDispatcher.dispatchInterval

    var offlineCacheAge: Long
        /**
         * See [.setOfflineCacheAge]
         *
         * @return maximum cache age in milliseconds
         */
        get() = preferences?.getLong(PREF_KEY_OFFLINE_CACHE_AGE, (24 * 60 * 60 * 1000).toLong()) ?: -1L
        /**
         * For how long events should be stored if they could not be send.
         * Events older than the set limit will be discarded on the next dispatch attempt.<br></br>
         * The Matomo backend accepts backdated events for up to 24 hours by default.
         *
         *
         * &gt;0 = limit in ms<br></br>
         * 0 = unlimited<br></br>
         * -1 = disabled offline cache<br></br>
         *
         * @param age in milliseconds
         */
        set(age) {
            preferences?.edit()?.putLong(PREF_KEY_OFFLINE_CACHE_AGE, age)?.apply()
        }

    var offlineCacheSize: Long
        /**
         * Maximum size the offline cache is allowed to grow to.
         *
         * @return size in byte
         */
        get() = preferences?.getLong(PREF_KEY_OFFLINE_CACHE_SIZE, (4 * 1024 * 1024).toLong()) ?: -1L
        /**
         * How large the offline cache may be.
         * If the limit is reached the oldest files will be deleted first.
         * Events older than the set limit will be discarded on the next dispatch attempt.<br></br>
         * The Matomo backend accepts backdated events for up to 24 hours by default.
         *
         *
         * &gt;0 = limit in byte<br></br>
         * 0 = unlimited<br></br>
         *
         * @param size in byte
         */
        set(size) {
            preferences?.edit()?.putLong(PREF_KEY_OFFLINE_CACHE_SIZE, size)?.apply()
        }

    var dispatchMode: DispatchMode?
        /**
         * The current dispatch behavior.
         *
         * @see DispatchMode
         */
        get() {
            if (localDispatchMode == null) {
                val raw: String? = preferences?.getString(PREF_KEY_DISPATCHER_MODE, null)
                localDispatchMode = DispatchMode.fromString(raw)
                if (localDispatchMode == null) localDispatchMode = DispatchMode.ALWAYS
            }
            return localDispatchMode
        }
        /**
         * Sets the dispatch mode.
         *
         * @see DispatchMode
         */
        set(mode) {
            localDispatchMode = mode
            if (mode != DispatchMode.EXCEPTION) {
                preferences?.edit()?.putString(PREF_KEY_DISPATCHER_MODE, mode.toString())?.apply()
            }
            localDispatcher.dispatchMode = mode
        }

    /**
     * Defines the User ID for this request.
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can
     * fetch this user ID from your system, and pass it to Matomo.
     *
     *
     * When specified, the User ID will be "enforced".
     * This means that if there is no recent visit with this User ID, a new one will be created.
     * If a visit is found in the last 30 minutes with your specified User ID,
     * then the new action will be recorded to this existing visit.
     *
     * @param userId passing null will delete the current user-id.
     */
    fun setUserId(userId: String?): Tracker {
        defaultTrackMe[QueryParams.USER_ID] = userId
        preferences?.edit()?.putString(PREF_KEY_TRACKER_USERID, userId)?.apply()
        return this
    }

    val userId: String
        /**
         * @return a user-id string, either the one you set or the one Matomo generated for you.
         */
        get() = defaultTrackMe[QueryParams.USER_ID]

    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Matomo will still track visits, but the unique visitors metric might be less accurate.
     */
    @Throws(IllegalArgumentException::class)
    fun setVisitorId(visitorId: String): Tracker {
        if (confirmVisitorIdFormat(visitorId)) defaultTrackMe[QueryParams.VISITOR_ID] = visitorId
        return this
    }

    val visitorId: String
        get() = defaultTrackMe[QueryParams.VISITOR_ID]

    init {
        LegacySettingsPorter(matomo).port(this)

        localOptOut = preferences?.getBoolean(PREF_KEY_TRACKER_OPTOUT, false) ?: false

        localDispatcher = matomo.dispatcherFactory.build(this)
        localDispatcher.dispatchMode = dispatchMode

        val userId: String? = preferences?.getString(PREF_KEY_TRACKER_USERID, null)
        defaultTrackMe[QueryParams.USER_ID] = userId

        var visitorId: String? = preferences?.getString(PREF_KEY_TRACKER_VISITORID, null)
        if (visitorId == null) {
            visitorId = makeRandomVisitorId()
            preferences?.edit()?.putString(PREF_KEY_TRACKER_VISITORID, visitorId)?.apply()
        }
        defaultTrackMe[QueryParams.VISITOR_ID] = visitorId

        defaultTrackMe[QueryParams.SESSION_START] = DEFAULT_TRUE_VALUE

        val deviceHelper: DeviceHelper = matomo.deviceHelper

        val resolution: String
        val res: IntArray = deviceHelper.getResolution()
        resolution = String.format("%sx%s", res[0], res[1])
        defaultTrackMe[QueryParams.SCREEN_RESOLUTION] = resolution

        defaultTrackMe.set(QueryParams.USER_AGENT, deviceHelper.getUserAgent())
        defaultTrackMe.set(QueryParams.LANGUAGE, deviceHelper.getUserLanguage())
        defaultTrackMe[QueryParams.URL_PATH] = config.applicationBaseUrl
    }

    @Throws(IllegalArgumentException::class)
    private fun confirmVisitorIdFormat(visitorId: String): Boolean {
        if (PATTERN_VISITOR_ID.matcher(visitorId).matches()) return true

        throw IllegalArgumentException(
            "VisitorId: " + visitorId + " is not of valid format, " +
                    " the format must match the regular expression: " + PATTERN_VISITOR_ID.pattern()
        )
    }

    /**
     * There parameters are only interesting for the very first query.
     */
    private fun injectInitialParams(trackMe: TrackMe?) {
        var firstVisitTime = -1L
        var visitCount: Long = 0
        var previousVisit = -1L

        preferences?.let {
            val prefs: SharedPreferences = it
            // Protected against Trackers on other threads trying to do the same thing.
            // This works because they would use the same preference object.
            synchronized(prefs) {
                val editor: SharedPreferences.Editor = prefs.edit()
                visitCount = 1 + it.getLong(PREF_KEY_TRACKER_VISITCOUNT, 0)
                editor.putLong(PREF_KEY_TRACKER_VISITCOUNT, visitCount)

                firstVisitTime = prefs.getLong(PREF_KEY_TRACKER_FIRSTVISIT, -1)
                if (firstVisitTime == -1L) {
                    firstVisitTime = System.currentTimeMillis() / 1000
                    editor.putLong(PREF_KEY_TRACKER_FIRSTVISIT, firstVisitTime)
                }

                previousVisit = prefs.getLong(PREF_KEY_TRACKER_PREVIOUSVISIT, -1)
                editor.putLong(PREF_KEY_TRACKER_PREVIOUSVISIT, System.currentTimeMillis() / 1000)
                editor.apply()
            }
        }

        // trySet because the developer could have modded these after creating the Tracker
        defaultTrackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, firstVisitTime)
        defaultTrackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, visitCount)

        if (previousVisit != -1L)
            defaultTrackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, previousVisit)

        trackMe!!.trySet(QueryParams.SESSION_START, defaultTrackMe[QueryParams.SESSION_START])
        trackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, defaultTrackMe[QueryParams.FIRST_VISIT_TIMESTAMP])
        trackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS])
        trackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, defaultTrackMe[QueryParams.PREVIOUS_VISIT_TIMESTAMP])
    }

    /**
     * These parameters are required for all queries.
     */
    private fun injectBaseParams(trackMe: TrackMe?) {
        trackMe!!.trySet(QueryParams.SITE_ID, siteId)
        trackMe.trySet(QueryParams.RECORD, DEFAULT_RECORD_VALUE)
        trackMe.trySet(QueryParams.API_VERSION, DEFAULT_API_VERSION_VALUE)
        trackMe.trySet(QueryParams.RANDOM_NUMBER, mRandomAntiCachingValue.nextInt(100000))
        trackMe.trySet(QueryParams.DATETIME_OF_REQUEST, SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US).format(Date()))
        trackMe.trySet(QueryParams.SEND_IMAGE, "0")

        trackMe.trySet(QueryParams.VISITOR_ID, defaultTrackMe[QueryParams.VISITOR_ID])
        trackMe.trySet(QueryParams.USER_ID, defaultTrackMe[QueryParams.USER_ID])

        trackMe.trySet(QueryParams.SCREEN_RESOLUTION, defaultTrackMe[QueryParams.SCREEN_RESOLUTION])
        trackMe.trySet(QueryParams.USER_AGENT, defaultTrackMe[QueryParams.USER_AGENT])
        trackMe.trySet(QueryParams.LANGUAGE, defaultTrackMe[QueryParams.LANGUAGE])

        var urlPath = trackMe[QueryParams.URL_PATH]
        if (urlPath == null) {
            urlPath = defaultTrackMe[QueryParams.URL_PATH]
        } else if (!VALID_URLS.matcher(urlPath).matches()) {
            val urlBuilder = StringBuilder(defaultApplicationBaseUrl)
            if (!defaultApplicationBaseUrl.endsWith("/") && !urlPath.startsWith("/")) {
                urlBuilder.append("/")
            } else if (defaultApplicationBaseUrl.endsWith("/") && urlPath.startsWith("/")) {
                urlPath = urlPath.substring(1)
            }
            urlPath = urlBuilder.append(urlPath).toString()
        }

        // https://github.com/matomo-org/matomo-sdk-android/issues/92
        defaultTrackMe[QueryParams.URL_PATH] = urlPath
        trackMe[QueryParams.URL_PATH] = urlPath
    }

    fun track(givenTrackMe: TrackMe?): Tracker {
        var trackMe = givenTrackMe
        synchronized(trackingLock) {
            val newSession = System.currentTimeMillis() - sessionStartTime > sessionTimeout
            if (newSession) {
                sessionStartTime = System.currentTimeMillis()
                injectInitialParams(trackMe)
            }

            injectBaseParams(trackMe)

            for (callback in mTrackingCallbacks) {
                trackMe = callback.onTrack(trackMe)
                if (trackMe == null) {
                    Timber.tag(TAG).d("Tracking aborted by %s", callback)
                    return this
                }
            }

            lastEventX = trackMe
            if (!localOptOut) {
                localDispatcher.submit(trackMe)
                Timber.tag(TAG).d("Event added to the queue: %s", trackMe)
            } else {
                Timber.tag(TAG).d("Event omitted due to opt out: %s", trackMe)
            }
            return this
        }
    }

    val preferences: SharedPreferences?
        get() {
            if (mPreferences == null)
                mPreferences = matomo.getTrackerPreferences(this)
            return mPreferences
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val tracker = other as Tracker

        if (siteId != tracker.siteId) return false
        if (aPIUrl != tracker.aPIUrl) return false
        return name == tracker.name
    }

    override fun hashCode(): Int {
        var result = aPIUrl.hashCode()
        result = 31 * result + siteId
        result = 31 * result + name.hashCode()
        return result
    }

    var dryRunTarget: List<Packet?>?
        /**
         * If we are in dry-run mode then this will return a datastructure.
         *
         * @return a datastructure or null
         */
        get() = localDispatcher.dryRunTarget
        /**
         * Set a data structure here to put the Dispatcher into dry-run-mode.
         * Data will be processed but at the last step just stored instead of transmitted.
         * Set it to null to disable it.
         *
         * @param dryRunTarget a data structure the data should be passed into
         */
        set(dryRunTarget) {
            localDispatcher.dryRunTarget = dryRunTarget
        }

    interface Callback {
        /**
         * This method will be called after parameter injection and before transmission within [Tracker.track].
         * Blocking within this method will block tracking.
         *
         * @param trackMe The `TrackMe` that was passed to [Tracker.track] after all data has been injected.
         * @return The `TrackMe` that will be send, returning NULL here will abort transmission.
         */
        fun onTrack(trackMe: TrackMe?): TrackMe?
    }

    companion object {
        private val TAG = Matomo.tag(Tracker::class.java)

        // Matomo default parameter values
        private const val DEFAULT_UNKNOWN_VALUE = "unknown"
        private const val DEFAULT_TRUE_VALUE = "1"
        private const val DEFAULT_RECORD_VALUE = DEFAULT_TRUE_VALUE
        private const val DEFAULT_API_VERSION_VALUE = "1"

        // Sharedpreference keys for persisted values
        const val PREF_KEY_TRACKER_OPTOUT: String = "tracker.optout"
        const val PREF_KEY_TRACKER_USERID: String = "tracker.userid"
        const val PREF_KEY_TRACKER_VISITORID: String = "tracker.visitorid"
        const val PREF_KEY_TRACKER_FIRSTVISIT: String = "tracker.firstvisit"
        const val PREF_KEY_TRACKER_VISITCOUNT: String = "tracker.visitcount"
        const val PREF_KEY_TRACKER_PREVIOUSVISIT: String = "tracker.previousvisit"
        protected const val PREF_KEY_OFFLINE_CACHE_AGE: String = "tracker.cache.age"
        protected const val PREF_KEY_OFFLINE_CACHE_SIZE: String = "tracker.cache.size"
        const val PREF_KEY_DISPATCHER_MODE: String = "tracker.dispatcher.mode"

        private val VALID_URLS: Pattern = Pattern.compile("^(\\w+)(?:://)(.+?)$")

        private val PATTERN_VISITOR_ID: Pattern = Pattern.compile("^[0-9a-f]{16}$")

        fun makeRandomVisitorId(): String {
            return UUID.randomUUID().toString().replace("-".toRegex(), "").substring(0, 16)
        }
    }
}
