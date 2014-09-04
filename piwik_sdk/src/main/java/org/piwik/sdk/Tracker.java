package org.piwik.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Main tracking class
 */
public class Tracker implements Dispatchable<Integer> {

    // Piwik default parameter values
    private static final String defaultTrueValue = "1";
    private static final String defaultRecordValue = defaultTrueValue;
    private static final String defaultAPIVersionValue = "1";

    // Default dispatcher values
    private static final int piwikDefaultSessionTimeout = 30 * 60;
    private static final int piwikDefaultDispatchTimer = 120;
    private static final int piwikHTTPRequestTimeout = 5;
    private static final int piwikQueryDefaultCapacity = 14;

    // @todo: doc
    private Piwik piwik;
    private boolean isDispatching = false;
    private int dispatchInterval = piwikDefaultDispatchTimer;
    private DispatchingHandler dispatchingHandler;
    private static String realScreenResolution;
    private static String userAgent;
    private static String userLanguage;
    private static String userCountry;
    private String lastEvent;

    private int siteId;
    private URL apiUrl;
    private String userId;
    private String authToken;
    private long sessionTimeoutMillis;
    private long sessionStartedMillis;

    private ArrayList<String> queue = new ArrayList<String>();
    private HashMap<String, String> queryParams;
    private HashMap<String, CustomVariables> customVariables = new HashMap<String, CustomVariables>(2);
    protected static final String LOGGER_TAG = Piwik.class.getName().toUpperCase();

    /**
     * Random object used for the request URl.
     */
    private Random randomObject = new Random(new Date().getTime());


    private Tracker(String url, int siteId) throws MalformedURLException {
        clearQueryParams();
        setAPIUrl(url);
        setNewSession();
        setSessionTimeout(piwikDefaultSessionTimeout);
        setUserId(getRandomVisitorId());
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
    synchronized public void dispatchingCompleted(Integer count) {
        isDispatching = false;
        Log.d(Tracker.LOGGER_TAG, String.format("dispatched %s url(s)", count));
    }

    @Override
    synchronized public void dispatchingStarted() {
        isDispatching = true;
    }

    @Override
    synchronized public boolean isDispatching() {
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
     * @param key   name
     * @param value value
     * @return tracker instance
     */
    public Tracker set(String key, String value) {
        if (value != null) {
            queryParams.put(key, value);
        }
        return this;
    }

    public Tracker set(String key, Integer value) {
        if (value != null) {
            set(key, Integer.toString(value));
        }
        return this;
    }

    /**
     * Defines the visitor ID for this request. You must set this value to exactly a 16 character
     * hexadecimal string (containing only characters 01234567890abcdefABCDEF).
     * When specified, the Visitor ID will be "enforced". This means that if there is no recent visit with
     * this visitor ID, a new one will be created. If a visit is found in the last 30 minutes with your
     * specified Visitor Id, then the new action will be recorded to this existing visit.
     *
     * @param userId any not 16 character hexadecimal string will be converted to md5 hash
     */
    public Tracker setUserId(String userId) {
        if (userId != null) {
            if (userId.length() == 16 && userId.matches("^[0-9a-fA-F]{16}$")) {
                this.userId = userId;
            } else {
                this.userId = md5(userId).substring(0, 16);
            }
        }
        return this;
    }

    public Tracker setUserId(long userId) {
        return setUserId(Long.toString(userId));
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
        if (!queryParams.containsKey(QueryParams.SCREEN_RESOLUTION)) {
            if (realScreenResolution == null) {
                try {
                    DisplayMetrics dm = new DisplayMetrics();
                    WindowManager wm = (WindowManager) piwik.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                    wm.getDefaultDisplay().getMetrics(dm);

                    realScreenResolution = formatResolution(dm.widthPixels, dm.heightPixels);
                } catch (Exception e) {
                    Log.w(Tracker.LOGGER_TAG, "Cannot grab resolution", e);

                    realScreenResolution = "unknown";
                }
            }
            return realScreenResolution;
        }

        return queryParams.get(QueryParams.SCREEN_RESOLUTION);
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
     * Set action_name param from activity's title and track view
     *
     * @param activity Current Activity instance
     */
    public void activityStart(final Activity activity) {
        if (activity != null) {
            String breadcrumbs = getBreadcrumbs(activity);
            trackScreenView(breadcrumbsToPath(breadcrumbs), breadcrumbs);
        }
    }

    /**
     * Force dispatching events if main activity is about to stop
     *
     * @param activity Current Activity instance
     */
    public void activityStop(final Activity activity) {
        if (activity != null && activity.isTaskRoot()) {
            dispatch();
        }
    }

    /**
     * @param activity current activity
     */
    public void activityPaused(final Activity activity) {
        activityStop(activity);
    }

    /**
     * Don't need to start auto dispatching
     * due this will be started when any track event occurred
     *
     * @param activity current activity
     */
    public void activityResumed(final Activity activity) {
        activityStart(activity);
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
        set(QueryParams.DOWNLOAD, getParamUlr());
        set(QueryParams.ACTION_NAME, "application/downloaded");
        set(QueryParams.URL_PATH, "/application/downloaded");
        return trackEvent("Application", "downloaded");
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
        className = className != null && className.length() > 0 ? className : "Unknown";
        String actionName = "exception/" +
                (isFatal ? "fatal/" : "") +
                (className + "/") + description;

        set(QueryParams.ACTION_NAME, actionName);
        trackEvent("Exception", className, description, isFatal ? 1 : 0);
    }

    protected final Thread.UncaughtExceptionHandler customUEH =
            new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    String className;

                    try {
                        try {
                            StackTraceElement trace = ex.getStackTrace()[0];
                            className = trace.getClassName() + "/" + trace.getMethodName() + ":" + trace.getLineNumber();
                        } catch (Exception e) {
                            Log.w(Tracker.LOGGER_TAG, "Couldn't get stack info", e);
                            className = ex.getClass().getName();
                        }

                        boolean isFatal = className.startsWith("RuntimeException");
                        String excInfo = ex.getClass().getName() + " [" + ex.getMessage() + "]";

                        // track
                        trackException(className, excInfo, isFatal);

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
        set(QueryParams.SITE_ID, siteId);
        set(QueryParams.RECORD, defaultRecordValue);
        set(QueryParams.RANDOM_NUMBER, randomObject.nextInt(100000));
        set(QueryParams.SCREEN_RESOLUTION, getResolution());
        set(QueryParams.URL_PATH, getParamUlr());
        set(QueryParams.USER_AGENT, getUserAgent());
        set(QueryParams.LANGUAGE, getLanguage());
        set(QueryParams.COUNTRY, getCountry());
        set(QueryParams.VISITOR_ID, userId);
        set(QueryParams.ENFORCED_VISITOR_ID, userId);
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

    private void clearAllCustomVariables() {
        getCustomVariables(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES).clear();
        getCustomVariables(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES).clear();
    }

    private Tracker setCustomVariable(String namespace, int index, String name, String value) {
        getCustomVariables(namespace).put(index, name, value);
        return this;
    }

    private void clearQueryParams() {
        if (queryParams != null) {
            queryParams.clear();
        }
        queryParams = new HashMap<String, String>(piwikQueryDefaultCapacity);

    }

    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(new Date());
    }

    private String getBreadcrumbs(final Activity activity) {
        Activity currentActivity = activity;
        ArrayList<String> breadcrumbs = new ArrayList<String>();

        while (currentActivity != null) {
            breadcrumbs.add(currentActivity.getTitle().toString());
            currentActivity = currentActivity.getParent();
        }
        return joinSlash(breadcrumbs);
    }

    private String joinSlash(List<String> sequence) {
        if (sequence != null && sequence.size() > 0) {
            return TextUtils.join("/", sequence);
        }
        return "";
    }

    private String breadcrumbsToPath(String breadcrumbs) {
        return breadcrumbs.replaceAll("\\s", "");
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

    protected String getParamUlr() {
        String url = queryParams.get(QueryParams.URL_PATH);
        if (url == null) {
            url = "/";
        } else if (!url.startsWith("/")) {
            url = "/" + url;
        }
        return String.format("http://%s%s", piwik.getApplicationDomain(), url);
    }

    protected String getUserId() {
        return userId;
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

    public static String md5(String s) {
        if (s == null) {
            return null;
        }
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes("UTF-8"), 0, s.length());
            BigInteger i = new BigInteger(1, m.digest());

            return String.format("%1$032x", i);

        } catch (UnsupportedEncodingException e) {
            Log.w(Tracker.LOGGER_TAG, s, e);
        } catch (NoSuchAlgorithmException e) {
            Log.w(Tracker.LOGGER_TAG, s, e);
        }
        return null;
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

    /**
     * CONSTANTS
     */
    public static class QueryParams {
        public static final String SITE_ID = "idsite";
        public static final String AUTHENTICATION_TOKEN = "token_auth";
        public static final String RECORD = "rec";
        public static final String API_VERSION = "apiv";
        public static final String SCREEN_RESOLUTION = "res";
        public static final String HOURS = "h";
        public static final String MINUTES = "m";
        public static final String SECONDS = "s";
        public static final String ACTION_NAME = "action_name";
        public static final String URL_PATH = "url";
        public static final String USER_AGENT = "ua";
        public static final String VISITOR_ID = "_id";
        public static final String ENFORCED_VISITOR_ID = "cid";

        public static final String VISIT_SCOPE_CUSTOM_VARIABLES = "_cvar";
        public static final String SCREEN_SCOPE_CUSTOM_VARIABLES = "cvar";
        public static final String RANDOM_NUMBER = "r";
        public static final String FIRST_VISIT_TIMESTAMP = "_idts";
        public static final String PREVIOUS_VISIT_TIMESTAMP = "_viewts";
        public static final String TOTAL_NUMBER_OF_VISITS = "_idvc";
        public static final String GOAL_ID = "idgoal";
        public static final String REVENUE = "revenue";
        public static final String SESSION_START = "new_visit";
        public static final String LANGUAGE = "lang";
        public static final String COUNTRY = "country";
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "long";
        public static final String SEARCH_KEYWORD = "search";
        public static final String SEARCH_CATEGORY = "search_cat";
        public static final String SEARCH_NUMBER_OF_HITS = "search_count";
        public static final String REFERRER = "urlref";
        public static final String DATETIME_OF_REQUEST = "cdt";
        public static final String DOWNLOAD = "download";

        // Campaign
        static final String CAMPAIGN_NAME = "_rcn";
        static final String CAMPAIGN_KEYWORD = "_rck";

        // Events
        static final String EVENT_CATEGORY = "e_c";
        static final String EVENT_ACTION = "e_a";
        static final String EVENT_NAME = "e_n";
        static final String EVENT_VALUE = "e_v";
    }

}

