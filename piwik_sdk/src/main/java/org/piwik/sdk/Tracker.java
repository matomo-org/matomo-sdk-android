/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.piwik.sdk.tools.DeviceHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main tracking class
 * This class is not Thread safe and should be externally synchronized or multiple instances used.
 */
public class Tracker implements Dispatchable<Integer> {

    // Piwik default parameter values
    private static final String DEFAULT_UNKNOWN_VALUE = "unknown";
    private static final String defaultTrueValue = "1";
    private static final String defaultRecordValue = defaultTrueValue;
    private static final String defaultAPIVersionValue = "1";

    // Default dispatcher values
    private static final int piwikDefaultSessionTimeout = 30 * 60;
    private static final int piwikDefaultDispatchTimer = 120;
    private static final int piwikHTTPRequestTimeout = 5;
    private static final int piwikQueryDefaultCapacity = 14;

    /**
     * The ID of the website we're tracking a visit/action for.
     */
    private int siteId;

    /**
     * Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     */
    private URL apiUrl;

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
     */
    private String userId;

    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Piwik will still track visits, but the unique visitors metric might be less accurate.
     */
    private String visitorId;

    /**
     * 32 character authorization key used to authenticate the API request.
     * Should be equals `token_auth` value of the Super User
     * or a user with admin access to the website visits are being tracked for.
     */
    private String authToken;


    private Piwik piwik;
    private String lastEvent;
    private boolean isDispatching = false;
    private int dispatchInterval = piwikDefaultDispatchTimer;
    private DispatchingHandler dispatchingHandler;

    private String applicationDomain;
    private static String mScreenResolution;
    private static String userAgent;
    private static String userLanguage;
    private static String userCountry;
    private long sessionTimeoutMillis;
    private long sessionStartedMillis;

    private final ArrayList<String> queue = new ArrayList<String>(20);
    private final HashMap<String, String> queryParams = new HashMap<String, String>(piwikQueryDefaultCapacity);
    private final HashMap<String, CustomVariables> customVariables = new HashMap<String, CustomVariables>(2);

    protected static final String LOGGER_TAG = Piwik.class.getName().toUpperCase();
    private final Random randomObject = new Random(new Date().getTime());


    private Tracker(String url, int siteId) throws MalformedURLException {
        setAPIUrl(url);
        setNewSession();
        setSessionTimeout(piwikDefaultSessionTimeout);
        visitorId = getRandomVisitorId();
        reportUncaughtExceptions(true);
        this.siteId = siteId;
    }

    /**
     * Use Piwik.newTracker() method to create new trackers
     *
     * @param url       (required) Tracking HTTP API endpoint, for example, http://your-piwik-domain.tld/piwik.php
     * @param siteId    (required) id of site
     * @param authToken (optional) could be null
     * @param piwik     piwik object used to gain access to application params such as name, resolution or lang
     * @throws MalformedURLException
     */
    protected Tracker(String url, int siteId, String authToken, Piwik piwik) throws MalformedURLException {
        this(url, siteId);
        this.authToken = authToken;
        this.piwik = piwik;
    }

    /**
     * Processes all queued events in background thread
     *
     * @return true if there are any queued events and opt out is inactive
     */
    public boolean dispatch() {
        if (!piwik.isOptOut() && queue.size() > 0) {

            ArrayList<String> events = new ArrayList<String>(queue);
            queue.clear();

            TrackerBulkURLProcessor worker =
                    new TrackerBulkURLProcessor(this, piwikHTTPRequestTimeout, piwik.isDryRun());
            worker.processBulkURLs(apiUrl, events, authToken);

            return true;
        }
        return false;
    }

    /**
     * Does dispatch immediately if dispatchInterval == 0
     * if dispatchInterval is greater than zero auto dispatching will be launched
     */
    private void tryDispatch() {
        if (dispatchInterval == 0) {
            dispatch();
        } else if (dispatchInterval > 0) {
            ensureAutoDispatching();
        }
    }

    /**
     * Starts infinity loop of dispatching process
     * Auto invoked when any Tracker.track* method is called and dispatchInterval > 0
     */
    private void ensureAutoDispatching() {
        if (dispatchingHandler == null) {
            dispatchingHandler = new DispatchingHandler(this);
        }
        dispatchingHandler.start();
    }

    /**
     * Auto invoked when negative interval passed in setDispatchInterval
     * or Activity is paused
     */
    private void stopAutoDispatching() {
        if (dispatchingHandler != null) {
            dispatchingHandler.stop();
        }
    }

    /**
     * Set the interval to 0 to dispatch events as soon as they are queued.
     * If a negative value is used the dispatch timer will never run, a manual dispatch must be used.
     *
     * @param dispatchInterval in seconds
     */
    public Tracker setDispatchInterval(int dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
        if (dispatchInterval < 1) {
            stopAutoDispatching();
        }
        return this;
    }

    public int getDispatchInterval() {
        return dispatchInterval;
    }

    @Override
    public long getDispatchIntervalMillis() {
        if (dispatchInterval > 0) {
            return dispatchInterval * 1000;
        }
        return -1;
    }

    @Override
    public void dispatchingCompleted(Integer count) {
        isDispatching = false;
        Log.d(Tracker.LOGGER_TAG, String.format("dispatched %s url(s)", count));
    }

    @Override
    public void dispatchingStarted() {
        isDispatching = true;
    }

    @Override
    public boolean isDispatching() {
        return isDispatching;
    }

    /**
     * You can set any additional Tracking API Parameters within the SDK.
     * This includes for example the local time (parameters h, m and s).
     * <pre>
     * tracker.set(QueryParams.HOURS, "10");
     * tracker.set(QueryParams.MINUTES, "45");
     * tracker.set(QueryParams.SECONDS, "30");
     * </pre>
     *
     * @param key   query params name
     * @param value value
     * @return tracker instance
     */
    public Tracker set(QueryParams key, String value) {
        if (value != null && value.length() > 0) {
            queryParams.put(key.toString(), value);
        }
        return this;
    }

    public Tracker set(QueryParams key, Integer value) {
        if (value != null) {
            set(key, Integer.toString(value));
        }
        return this;
    }

    /**
     * Sets a User ID to this user (such as an email address or a username)
     *
     * @param userId this parameter can be set to any string.
     *               The string will be hashed, and used as "User ID".
     */
    public Tracker setUserId(String userId) {
        if (userId != null && userId.length() > 0) {
            this.userId = userId;
        }
        return this;
    }

    public Tracker setUserId(long userId) {
        return setUserId(Long.toString(userId));
    }

    public Tracker clearUserId() {
        userId = null;
        return this;
    }

    public Tracker setVisitorId(String visitorId) throws IllegalArgumentException {
        if (confirmVisitorIdFormat(visitorId)) {
            this.visitorId = visitorId;
        }
        return this;
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
     * @param domain your-domain.com
     */
    public Tracker setApplicationDomain(String domain) {
        applicationDomain = domain;
        return this;
    }

    protected String getApplicationDomain() {
        return applicationDomain != null ? applicationDomain : piwik.getApplicationDomain();
    }

    /**
     * Sets the screen resolution of the browser which sends the request.
     *
     * @param width  the screen width as an int value
     * @param height the screen height as an int value
     */
    public Tracker setResolution(final int width, final int height) {
        return set(QueryParams.SCREEN_RESOLUTION, formatResolution(width, height));
    }

    private String formatResolution(final int width, final int height) {
        return String.format("%sx%s", width, height);
    }

    /**
     * Returns real screen size if QueryParams.SCREEN_RESOLUTION is empty
     * Note that the results also depend on the current device orientation.
     * http://stackoverflow.com/a/9316553
     *
     * @return formatted string: WxH
     */
    public String getResolution() {
        if (queryParams.containsKey(QueryParams.SCREEN_RESOLUTION.toString())) {
            return queryParams.get(QueryParams.SCREEN_RESOLUTION.toString());
        } else {
            if (mScreenResolution == null) {
                int[] resolution = DeviceHelper.getResolution(piwik.getApplicationContext());
                if (resolution != null) {
                    mScreenResolution = formatResolution(resolution[0], resolution[1]);
                } else {
                    mScreenResolution = DEFAULT_UNKNOWN_VALUE;
                }
            }
            return mScreenResolution;
        }
    }

    /**
     * This methods have to be called before a call to trackScreenView.
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
     *              Custom variable names and values are limited to 200 characters in length each.
     */
    public Tracker setUserCustomVariable(int index, String name, String value) {
        return setCustomVariable(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    /**
     * Does exactly the same as setUserCustomVariable but use screen scope
     * You can track up to 5 custom variables for each screen view.
     */
    public Tracker setScreenCustomVariable(int index, String name, String value) {
        return setCustomVariable(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    /**
     * Correspondents to action_name of Piwik Tracking API
     *
     * @param title string The title of the action being tracked. It is possible to use
     *              slashes / to set one or several categories for this action.
     *              For example, Help / Feedback will create the Action Feedback in the category Help.
     */
    public Tracker setScreenTitle(String title) {
        return set(QueryParams.ACTION_NAME, title);
    }

    /**
     * Returns android system user agent
     *
     * @return well formatted user agent
     */
    public String getUserAgent() {
        if (userAgent == null) {
            userAgent = System.getProperty("http.agent");
        }
        return userAgent;
    }

    /**
     * Sets custom UserAgent
     *
     * @param userAgent your custom UserAgent String
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Returns user language
     *
     * @return language
     */
    public String getLanguage() {
        if (userLanguage == null) {
            userLanguage = Locale.getDefault().getLanguage();
        }
        return userLanguage;
    }

    /**
     * Returns user country
     *
     * @return country
     */
    public String getCountry() {
        if (userCountry == null) {
            userCountry = Locale.getDefault().getCountry();
        }
        return userCountry;
    }

    /**
     * Session handling
     */
    public void setNewSession() {
        touchSession();
        set(QueryParams.SESSION_START, defaultTrueValue);
    }

    private void touchSession() {
        sessionStartedMillis = System.currentTimeMillis();
    }

    public void setSessionTimeout(int seconds) {
        sessionTimeoutMillis = seconds * 1000;
    }

    protected void checkSessionTimeout() {
        if (isExpired()) {
            setNewSession();
        }
    }

    protected boolean isExpired() {
        return System.currentTimeMillis() - sessionStartedMillis > sessionTimeoutMillis;
    }

    public int getSessionTimeout() {
        return (int) sessionTimeoutMillis / 1000;
    }

    /**
     * Tracking methods
     *
     * @param path required tracking param, for example: "/user/settings/billing"
     */
    public Tracker trackScreenView(String path) {
        set(QueryParams.URL_PATH, path);
        return doTrack();
    }

    /**
     * @param path  view path for example: "/user/settings/billing" or just empty string ""
     * @param title string The title of the action being tracked. It is possible to use
     *              slashes / to set one or several categories for this action.
     *              For example, Help / Feedback will create the Action Feedback in the category Help.
     */
    public Tracker trackScreenView(String path, String title) {
        setScreenTitle(title);
        return trackScreenView(path);
    }

    /**
     * Events are a useful way to collect data about a user's interaction with interactive components of your app,
     * like button presses or the use of a particular item in a game.
     *
     * @param category (required) – this String defines the event category.
     *                 You might define event categories based on the class of user actions,
     *                 like clicks or gestures or voice commands, or you might define them based upon the
     *                 features available in your application (play, pause, fast forward, etc.).
     * @param action   (required) this String defines the specific event action within the category specified.
     *                 In the example, we are basically saying that the category of the event is user clicks,
     *                 and the action is a button click.
     * @param label    defines a label associated with the event. For example, if you have multiple Button controls on a
     *                 screen, you might use the label to specify the specific View control identifier that was clicked.
     * @param value    defines a numeric value associated with the event. For example, if you were tracking "Buy"
     *                 button clicks, you might log the number of items being purchased, or their total cost.
     */
    public Tracker trackEvent(String category, String action, String label, Integer value) {
        if (category != null && action != null) {
            set(QueryParams.EVENT_ACTION, action);
            set(QueryParams.EVENT_CATEGORY, category);
            set(QueryParams.EVENT_NAME, label);
            set(QueryParams.EVENT_VALUE, value);
            doTrack();
        }
        return this;
    }

    public Tracker trackEvent(String category, String action, String label) {
        return trackEvent(category, action, label, null);
    }

    public Tracker trackEvent(String category, String action) {
        return trackEvent(category, action, null, null);
    }

    /**
     * By default, Goals in Piwik are defined as "matching" parts of the screen path or screen title.
     * In this case a conversion is logged automatically. In some situations, you may want to trigger
     * a conversion manually on other types of actions, for example:
     * when a user submits a form
     * when a user has stayed more than a given amount of time on the page
     * when a user does some interaction in your Android application
     *
     * @param idGoal id of goal as defined in piwik goal settings
     */
    public Tracker trackGoal(Integer idGoal) {
        if (idGoal != null && idGoal > 0) {
            set(QueryParams.GOAL_ID, idGoal);
            return doTrack();
        }
        return this;
    }

    /**
     * Tracking request will trigger a conversion for the goal of the website being tracked with this ID
     *
     * @param idGoal  id of goal as defined in piwik goal settings
     * @param revenue a monetary value that was generated as revenue by this goal conversion.
     */
    public Tracker trackGoal(Integer idGoal, int revenue) {
        set(QueryParams.REVENUE, revenue);
        return trackGoal(idGoal);
    }

    /**
     * Ensures that tracking application downloading will be fired only once
     * by using SharedPreferences as flag storage
     */
    public Tracker trackAppDownload() {
        SharedPreferences prefs = piwik.getSharedPreferences(
                Piwik.class.getPackage().getName(), Context.MODE_PRIVATE);

        if (!prefs.getBoolean("downloaded", false)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("downloaded", true);
            editor.commit();
            trackNewAppDownload();
        }
        return this;
    }

    /**
     * Make sure to call this method only once per user
     */
    public Tracker trackNewAppDownload() {
        set(QueryParams.DOWNLOAD, getApplicationBaseURL());
        set(QueryParams.ACTION_NAME, "application/downloaded");
        set(QueryParams.URL_PATH, "/application/downloaded");
        return trackEvent("Application", "downloaded");
    }

    /**
     * Tracking the impressions
     *
     * @param contentName   The name of the content. For instance 'Ad Foo Bar'
     * @param contentPiece  The actual content. For instance the path to an image, video, audio, any text
     * @param contentTarget (optional) The target of the content. For instance the URL of a landing page.
     */
    public Tracker trackContentImpression(String contentName, String contentPiece, String contentTarget) {
        if (contentName != null && contentName.length() > 0) {
            set(QueryParams.CONTENT_NAME, contentName);
            set(QueryParams.CONTENT_PIECE, contentPiece);
            set(QueryParams.CONTENT_TARGET, contentTarget);
            return doTrack();
        }
        return this;
    }

    /**
     * Tracking the interactions
     *
     * @param interaction   The name of the interaction with the content. For instance a 'click'
     * @param contentName   The name of the content. For instance 'Ad Foo Bar'
     * @param contentPiece  The actual content. For instance the path to an image, video, audio, any text
     * @param contentTarget (optional) The target the content leading to when an interaction occurs. For instance the URL of a landing page.
     */
    public Tracker trackContentInteraction(String interaction, String contentName, String contentPiece, String contentTarget) {
        if (interaction != null && interaction.length() > 0) {
            set(QueryParams.CONTENT_INTERACTION, interaction);
            return trackContentImpression(contentName, contentPiece, contentTarget);
        }
        return this;
    }

    /**
     * Caught exceptions are errors in your app for which you've defined exception handling code,
     * such as the occasional timeout of a network connection during a request for data.
     *
     * @param className   $ClassName:$lineNumber
     * @param description exception message
     * @param isFatal     true if it's RunTimeException
     */
    public void trackException(String className, String description, boolean isFatal) {
        className = className != null && className.length() > 0 ? className : DEFAULT_UNKNOWN_VALUE;
        String actionName = "exception/" +
                (isFatal ? "fatal/" : "") +
                (className + "/") + description;

        set(QueryParams.ACTION_NAME, actionName);
        trackEvent("Exception", className, description, isFatal ? 1 : 0);
    }

    /**
     * Caught exceptions are errors in your app for which you've defined exception handling code,
     * such as the occasional timeout of a network connection during a request for data.
     *
     * @param ex          exception instance
     * @param description exception message
     * @param isFatal     true if it's fatal exeption
     */
    public void trackException(Throwable ex, String description, boolean isFatal) {
        String className;
        try {
            StackTraceElement trace = ex.getStackTrace()[0];
            className = trace.getClassName() + "/" + trace.getMethodName() + ":" + trace.getLineNumber();
        } catch (Exception e) {
            Log.w(Tracker.LOGGER_TAG, "Couldn't get stack info", e);
            className = ex.getClass().getName();
        }

        trackException(className, description, isFatal);
    }

    protected final Thread.UncaughtExceptionHandler customUEH =
            new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    try {
                        String excInfo = ex.getMessage();

                        // track
                        trackException(ex, excInfo, true);

                        // dispatch immediately
                        dispatch();
                    } catch (Exception e) {
                        // fail silently
                        Log.e(Tracker.LOGGER_TAG, "Couldn't track uncaught exception", e);
                    } finally {
                        // re-throw critical exception further to the os (important)
                        if (Piwik.defaultUEH != null && Piwik.defaultUEH != customUEH) {
                            Piwik.defaultUEH.uncaughtException(thread, ex);
                        }
                    }

                }
            };

    /**
     * Uncaught exceptions are sent to Piwik automatically by default
     *
     * @param toggle true if reporting should be enabled
     */
    public Tracker reportUncaughtExceptions(boolean toggle) {
        if (toggle) {
            // Setup handler for uncaught exception
            Thread.setDefaultUncaughtExceptionHandler(customUEH);
        } else {
            Thread.setDefaultUncaughtExceptionHandler(Piwik.defaultUEH);
        }
        return this;
    }


    /**
     * Set up required params
     */
    protected void beforeTracking() {
        set(QueryParams.API_VERSION, defaultAPIVersionValue);
        set(QueryParams.SEND_IMAGE, "0");
        set(QueryParams.SITE_ID, siteId);
        set(QueryParams.RECORD, defaultRecordValue);
        set(QueryParams.RANDOM_NUMBER, randomObject.nextInt(100000));
        set(QueryParams.SCREEN_RESOLUTION, getResolution());
        set(QueryParams.URL_PATH, getParamURL());
        set(QueryParams.USER_AGENT, getUserAgent());
        set(QueryParams.LANGUAGE, getLanguage());
        set(QueryParams.COUNTRY, getCountry());
        set(QueryParams.VISITOR_ID, visitorId);
        set(QueryParams.USER_ID, getUserId());
        set(QueryParams.DATETIME_OF_REQUEST, getCurrentDatetime());
        set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, getCustomVariables(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES).toString());
        set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, getCustomVariables(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES).toString());
        checkSessionTimeout();
        touchSession();
    }

    /**
     * Builds URL, adds event to queue, clean all params after url was added
     */
    protected Tracker doTrack() {
        beforeTracking();

        String event = getQuery();
        if (piwik.isOptOut()) {
            lastEvent = event;
            Log.d(Tracker.LOGGER_TAG, String.format("URL omitted due to opt out: %s", event));
        } else {
            Log.d(Tracker.LOGGER_TAG, String.format("URL added to the queue: %s", event));
            queue.add(event);

            tryDispatch();
        }

        afterTracking();
        return this;
    }

    /**
     * Clean up params
     */
    protected void afterTracking() {
        clearQueryParams();
        clearAllCustomVariables();
    }

    /**
     *
     * HELPERS
     *
     */

    /**
     * Gets all custom vars from screen or visit scope
     *
     * @param namespace `_cvar` or `cvar` stored in
     *                  QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES and
     *                  QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES
     * @return CustomVariables HashMap
     */
    private CustomVariables getCustomVariables(String namespace) {
        if (namespace == null) {
            return null;
        }
        CustomVariables vars = customVariables.get(namespace);
        if (vars == null) {
            vars = new CustomVariables();
            customVariables.put(namespace, vars);
        }
        return vars;
    }

    private CustomVariables getCustomVariables(QueryParams namespace) {
        if (namespace == null) {
            return null;
        }

        return getCustomVariables(namespace.toString());
    }

    private void clearAllCustomVariables() {
        getCustomVariables(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES).clear();
        getCustomVariables(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES).clear();
    }

    private Tracker setCustomVariable(QueryParams namespace, int index, String name, String value) {
        getCustomVariables(namespace.toString()).put(index, name, value);
        return this;
    }

    private void clearQueryParams() {
        // To avoid useless shrinking and resizing of the Map
        // the capacity is held the same when clear() is called.
        queryParams.clear();
    }

    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(new Date());
    }

    protected String getQuery() {
        return TrackerBulkURLProcessor.urlEncodeUTF8(queryParams);
    }

    /**
     * For testing purposes
     *
     * @return query of the event ?r=1&sideId=1..
     */
    protected String getLastEvent() {
        return lastEvent;
    }

    protected void clearLastEvent() {
        lastEvent = null;
    }

    protected String getApplicationBaseURL() {
        return String.format("http://%s", getApplicationDomain());
    }

    protected String getParamURL() {
        String url = queryParams.get(QueryParams.URL_PATH.toString());

        if (url == null) {
            url = "/";
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else if (!url.startsWith("/")) {
            url = "/" + url;
        }

        return getApplicationBaseURL() + url;
    }

    protected String getUserId() {
        return userId;
    }

    protected String getVisitorId() {
        return visitorId;
    }

    /**
     * Sets the url of the piwik installation the dispatchable will track to.
     * <p/>
     * The given string should be in the format of RFC2396. The string will be converted to an url with no other url as
     * its context.
     *
     * @param APIUrl as a string object
     * @throws MalformedURLException
     */
    protected final void setAPIUrl(final String APIUrl) throws MalformedURLException {
        if (APIUrl == null) {
            throw new MalformedURLException("You must provide the Piwik Tracker URL! e.g. http://piwik.website.org/piwik.php");
        }

        URL url = new URL(APIUrl);
        String path = url.getPath();

        if (path.endsWith("piwik.php") || path.endsWith("piwik-proxy.php")) {
            this.apiUrl = url;
        } else {
            if (!path.endsWith("/")) {
                path += "/";
            }
            this.apiUrl = new URL(url, path + "piwik.php");
        }
    }

    protected String getAPIUrl() {
        return apiUrl.toString();
    }

    private String getRandomVisitorId() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tracker tracker = (Tracker) o;

        return siteId == tracker.siteId && apiUrl.equals(tracker.apiUrl);
    }

    @Override
    public int hashCode() {
        int result = siteId;
        result = 31 * result + apiUrl.hashCode();
        return result;
    }

}

