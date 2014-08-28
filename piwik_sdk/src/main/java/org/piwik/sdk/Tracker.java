package org.piwik.sdk;

import android.app.Activity;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


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


    private Tracker(String url, int siteId) throws MalformedURLException {
        setAPIUrl(url);
        setNewSession();
        setSessionTimeout(piwikDefaultSessionTimeout);
        clearQueryParams();
        setUserId(getRandomVisitorId());
        this.siteId = siteId;
    }

    public Tracker(String url, int siteId, String authToken, Piwik piwik) throws MalformedURLException {
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
        if (!piwik.isOptOut() && queue.size() > 0){

            ArrayList<String> events = new ArrayList<String>(queue);
            queue.clear();

            TrackerBulkURLProcessor worker = new TrackerBulkURLProcessor(this, piwikHTTPRequestTimeout);
            worker.processBulkURLs(apiUrl, events, authToken);

            return true;
        }
        return false;
    }

    @Override
    synchronized public void dispatchingCompleted(Integer count){
        isDispatching = false;
        LOGGER.log(Level.ALL, String.format("dispatched %s url(s)", count));
    }

    @Override
    synchronized public void startDispatching() {
        isDispatching = true;
    }

    @Override
    synchronized public boolean isDispatching() {
        return isDispatching;
    }

    /**
     * Public setter
     * @param key name
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
            if(userId.toLowerCase().matches("[0-9a-f]{16}")) {
                this.userId = userId;
            } else {
                this.userId = md5(userId).substring(0, 16);
            }
        }
    }

    public void setUserId(long userId) {
        String hash = md5(Long.toString(userId));
        setUserId(hash);
    }

    public void setDispatchInterval(int dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
        // TODO stop dispatching loop when dispatchInterval < 1
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
     * @return formatted string  WxH
     */
    public String getResolution(){
        if(queryParams.containsKey(QueryParams.SCREEN_RESOLOUTION)){
            return queryParams.get(QueryParams.SCREEN_RESOLOUTION);
        }
        return defaultScreenResolution;
    }

    public void setUserCustomVariable(int index, String name, String value) {
        this.setCustomVariable(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    public void setScreenCustomVariable(int index, String name, String value) {
        this.setCustomVariable(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    public void setScreenTitle(String title) {
        set(QueryParams.ACTION_NAME, title);
    }

    /**
     * todo return well formatted user agent with android version and local
     * @return
     */
    public String getUserAgent(){
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
     * @todo pause handler when activity is paused
     * @param activity
     */
    public void activityPaused(Activity activity) {

    }

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
     * @param path required query param
     */
    public void trackScreenView(String path) {
        set(QueryParams.URL_PATH, path);
        doTrack();
    }

    public void trackScreenView(String url, String title) {
        setScreenTitle(title);
        trackScreenView(url);
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

    public void trackGoal(Integer idGoal) {
        set(QueryParams.GOAL_ID, idGoal);
        doTrack();
    }

    /**
     * Set up requierd values
     */
    protected void beforeTracking() {
        // obligatory params
        this.set(QueryParams.API_VERSION, defaultAPIVersionValue);
        this.set(QueryParams.SITE_ID, siteId);
        this.set(QueryParams.RECORD, defaultRecordValue);
        this.set(QueryParams.RANDOM_NUMBER, new Random().nextInt(100000));
        this.set(QueryParams.SCREEN_RESOLOUTION, this.getResolution());
        this.set(QueryParams.URL_PATH, this.getParamUlr());
        this.set(QueryParams.USER_AGENT, this.getUserAgent());
        this.set(QueryParams.VISITOR_ID, this.userId);
        this.set(QueryParams.ENFORCED_VISITOR_ID, this.userId);
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
        } else if (piwik.isDryRun()) {
            LOGGER.log(Level.INFO, String.format("dry run URL: %s", event));
        } else {
            LOGGER.log(Level.ALL, String.format("URL added to the queue: %s", event));
            queue.add(event);
            // TODO - fire dispatch if dispatchInterval equals 0
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
     * @param namespace `_cvar` or `cvar` stored in
     *      QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES and
     *      QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES
     * @return CustomVariables HashMap
     */
    private CustomVariables getCustomVariables(String namespace) {
        if(namespace == null){
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

    private String getBreadcrumbs(final Activity activity){
        Activity currentActivity = activity;
        ArrayList<String> breadcrumbs = new ArrayList<String>();

        while (currentActivity != null){
            breadcrumbs.add(currentActivity.getTitle().toString());
            currentActivity = currentActivity.getParent();
        }
        return joinSlash(breadcrumbs);
    }

    private String joinSlash(List<String> sequence){
        if(sequence != null && sequence.size() > 0) {
            return TextUtils.join("/", sequence);
        }
        return "";
    }

    private String breadcrumbsToPath(String breadcrumbs){
        return breadcrumbs.replaceAll("\\s", "");
    }

    private String getQuery() {
        return TrackerBulkURLProcessor.urlEncodeUTF8(queryParams);
    }

    protected String getParamUlr() {
        String url = queryParams.get(QueryParams.URL_PATH);
        url = (url == null) ? "" : url;
        return String.format("http://%s/%s", piwik.getApplicationDomain() , url);
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
        if(s == null){
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

