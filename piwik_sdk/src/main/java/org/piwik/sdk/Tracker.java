package org.piwik.sdk;

import android.app.Activity;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


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
    private static final int piwikQueryDefaultCapacity = 12;

    // private
    // @todo: doc
    private Piwik piwik;
    private boolean isDispatching = false;
    private int dispatchInterval = piwikDefaultDispatchTimer;
    private DispatchingHandler dispatchingHandler;

    private int siteId;
    private URL apiUrl;
    private String userId;
    private String authToken;
    private String defaultScreenResolution;
    private long sessionTimeoutMillis;
    private long sessionStartedMillis;

    private ArrayList<String> queue = new ArrayList<String>();
    private HashMap<String, String> queryParams;
    private HashMap<String, CustomVariables> customVariables = new HashMap<String, CustomVariables>(2);
    private static final Logger LOGGER = Logger.getLogger(Piwik.class.getName());

    /**
     * Random object used for the request URl.
     */
    private Random randomObject = new Random(new Date().getTime());


    private Tracker(String url, int siteId) throws MalformedURLException {
        setAPIUrl(url);
        setNewSession();
        setSessionTimeout(piwikDefaultSessionTimeout);
        clearQueryParams();
        setUserId(getRandomVisitorId());
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
    public void setDispatchInterval(int dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
        if (dispatchInterval < 1) {
            stopAutoDispatching();
        }
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
        LOGGER.log(Level.ALL, String.format("dispatched %s url(s)", count));
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
     * @return if value is not null
     */
    public boolean set(String key, String value) {
        if (value != null) {
            queryParams.put(key, value);
            return true;
        }
        return false;
    }

    public boolean set(String key, Integer value) {
        if (value != null) {
            set(key, Integer.toString(value));
            return true;
        }
        return false;
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
    public void setUserId(String userId) {
        if (userId != null) {
            if (userId.length() == 16 && userId.matches("^[0-9a-fA-F]{16}$")) {
                this.userId = userId;
            } else {
                this.userId = md5(userId).substring(0, 16);
            }
        }
    }

    public void setUserId(long userId) {
        setUserId(Long.toString(userId));
    }

    /**
     * Sets the screen resolution of the browser which sends the request.
     *
     * @param width  the screen width as an int value
     * @param height the screen height as an int value
     */
    public void setResolution(final int width, final int height) {
        set(QueryParams.SCREEN_RESOLOUTION, String.format("%sx%s", width, height));
    }

    /**
     * todo return real screen size if QueryParams.SCREEN_RESOLOUTION is empty
     * http://stackoverflow.com/a/25215912
     *
     * @return formatted string  WxH
     */
    public String getResolution() {
        if (queryParams.containsKey(QueryParams.SCREEN_RESOLOUTION)) {
            return queryParams.get(QueryParams.SCREEN_RESOLOUTION);
        }
        return defaultScreenResolution;
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
    public void setUserCustomVariable(int index, String name, String value) {
        this.setCustomVariable(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    /**
     * Does exactly the same as setUserCustomVariable but use screen scope
     * You can track up to 5 custom variables for each screen view.
     */
    public void setScreenCustomVariable(int index, String name, String value) {
        this.setCustomVariable(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    /**
     * Correspondents to action_name of Piwik Tracking API
     *
     * @param title string The title of the action being tracked. It is possible to use
     *              slashes / to set one or several categories for this action.
     *              For example, Help / Feedback will create the Action Feedback in the category Help.
     */
    public void setScreenTitle(String title) {
        set(QueryParams.ACTION_NAME, title);
    }

    /**
     * todo return well formatted user agent with android version and local
     *
     * @return
     */
    public String getUserAgent() {
        return "Android";
    }

    /**
     * Set action_name param from activity's title and track view
     *
     * @param activity Current Activity instance
     */
    public void activityStart(Activity activity) {
        String breadcrumbs = getBreadcrumbs(activity);
        trackScreenView(breadcrumbsToPath(breadcrumbs), breadcrumbs);
    }

    /**
     * Force dispatching events if main activity is about to stop
     *
     * @param activity Current Activity instance
     */
    public void activityStop(Activity activity) {
        if (activity.isTaskRoot()) {
            this.dispatch();
        }
    }

    /**
     * @param activity current activity
     */
    public void activityPaused(Activity activity) {
        stopAutoDispatching();
    }

    /**
     * Don't need to start auto dispatching
     * due this will be started when any track event occurred
     *
     * @param activity current activity
     */
    public void activityResumed(Activity activity) {
    }

    /**
     * Session handling
     */
    public void setNewSession() {
        this.touchSession();
        this.set(QueryParams.SESSION_START, defaultTrueValue);
    }

    private void touchSession() {
        sessionStartedMillis = System.currentTimeMillis();
    }

    public void setSessionTimeout(int seconds) {
        sessionTimeoutMillis = seconds * 1000;
    }

    private void checkSessionTimeout() {
        if (System.currentTimeMillis() - sessionStartedMillis > sessionTimeoutMillis) {
            setNewSession();
        }
    }

    /**
     * Tracking methods
     *
     * @param path required tracking param, for example: "/user/settings/billing"
     */
    public void trackScreenView(String path) {
        set(QueryParams.URL_PATH, path);
        doTrack();
    }

    /**
     * @param path  view path for example: "/user/settings/billing" or just empty string ""
     * @param title string The title of the action being tracked. It is possible to use
     *              slashes / to set one or several categories for this action.
     *              For example, Help / Feedback will create the Action Feedback in the category Help.
     */
    public void trackScreenView(String path, String title) {
        setScreenTitle(title);
        trackScreenView(path);
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
    public void trackEvent(String category, String action, String label, Integer value) {
        if (set(QueryParams.EVENT_CATEGORY, category) && set(QueryParams.EVENT_ACTION, action)) {
            set(QueryParams.EVENT_NAME, label);
            set(QueryParams.EVENT_VALUE, value);
            doTrack();
        }
    }

    public void trackEvent(String category, String action, String label) {
        trackEvent(category, action, label, null);
    }

    public void trackEvent(String category, String action) {
        trackEvent(category, action, null, null);
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
    public void trackGoal(Integer idGoal) {
        set(QueryParams.GOAL_ID, idGoal);
        doTrack();
    }

    /**
     * Set up required params
     */
    protected void beforeTracking() {
        // obligatory params
        this.set(QueryParams.API_VERSION, defaultAPIVersionValue);
        this.set(QueryParams.SITE_ID, siteId);
        this.set(QueryParams.RECORD, defaultRecordValue);
        this.set(QueryParams.RANDOM_NUMBER, randomObject.nextInt(100000));
        this.set(QueryParams.SCREEN_RESOLOUTION, this.getResolution());
        this.set(QueryParams.URL_PATH, this.getParamUlr());
        this.set(QueryParams.USER_AGENT, this.getUserAgent());
        this.set(QueryParams.VISITOR_ID, this.userId);
        this.set(QueryParams.ENFORCED_VISITOR_ID, this.userId);
        this.set(QueryParams.DATETIME_OF_REQUEST, this.getCurrentDatetime());
        this.set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, this.getCustomVariables(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES).toString());
        this.set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, this.getCustomVariables(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES).toString());
        this.checkSessionTimeout();
        this.touchSession();
    }

    /**
     * Builds URL, adds event to queue, clean all params after url was added
     */
    protected void doTrack() {
        beforeTracking();

        String event = getQuery();
        if (piwik.isOptOut()) {
            LOGGER.log(Level.ALL, String.format("URL omitted due to opt out: %s", event));
        } else {
            LOGGER.log(Level.ALL, String.format("URL added to the queue: %s", event));
            queue.add(event);

            tryDispatch();
        }

        afterTracking();
    }

    /**
     * Clean up params
     */
    protected void afterTracking() {
        this.clearQueryParams();
        this.clearAllCustomVariables();
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

    private void setCustomVariable(String namespace, int index, String name, String value) {
        getCustomVariables(namespace).put(index, name, value);
    }

    private void clearQueryParams() {
        if (queryParams != null) {
            queryParams.clear();
        }
        queryParams = new HashMap<String, String>(piwikQueryDefaultCapacity);

    }

    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyyMMdd HH:mm:ssZ").format(new Date());
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

    private String getQuery() {
        return TrackerBulkURLProcessor.urlEncodeUTF8(queryParams);
    }

    protected String getParamUlr() {
        String url = queryParams.get(QueryParams.URL_PATH);
        url = (url == null) ? "" : url;
        return String.format("http://%s/%s", piwik.getApplicationDomain(), url);
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
            throw new MalformedURLException("You must provide the Piwik Tracker URL! e.g. http://your-website.org/piwik/\"");
        }

        URL url = new URL(APIUrl);

        if (url.getPath().endsWith("piwik.php") || url.getPath().endsWith("piwik-proxy.php")) {
            this.apiUrl = url;
        } else {
            this.apiUrl = new URL(url, url.getPath() + "/piwik.php");
        }
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
            LOGGER.log(Level.WARNING, s, e);
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.WARNING, s, e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * CONSTANTS
     */
    public static class QueryParams {
        public static final String SITE_ID = "idsite";
        public static final String AUTHENTICATION_TOKEN = "token_auth";
        public static final String RECORD = "rec";
        public static final String API_VERSION = "apiv";
        public static final String SCREEN_RESOLOUTION = "res";
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
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "long";
        public static final String SEARCH_KEYWORD = "search";
        public static final String SEARCH_CATEGORY = "search_cat";
        public static final String SEARCH_NUMBER_OF_HITS = "search_count";
        public static final String REFERRER = "urlref";
        public static final String DATETIME_OF_REQUEST = "cdt";

        // Campaign
        static final String CAMPAIGN_NAME = "_rcn";
        static final String CAMPAIGN_KEYWORD = "_rck";

        // Events
        public static final String EVENT_CATEGORY = "e_c";
        public static final String EVENT_ACTION = "e_a";
        public static final String EVENT_NAME = "e_n";
        public static final String EVENT_VALUE = "e_v";
    }

}

