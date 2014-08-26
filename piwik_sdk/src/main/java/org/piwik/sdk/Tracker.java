package org.piwik.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Tracker {

    // Piwik default parameter values
    private static final String defaultTrueValue = "1";
    private static final String defaultRecordValue = defaultTrueValue;
    private static final String defaultAPIVersionValue = "1";

    // Default dispatcher values
    private static final int piwikDefaultSessionTimeout = 30 * 60;
    private static final int piwikDefaultDispatchTimer = 120;
    private static final int piwikDefaultMaxNumberOfStoredEvents = 500;
    private static final int piwikDefaultSampleRate = 100;
    private static final int piwikDefaultNumberOfEventsPerRequest = 20;
    private static final int piwikHTTPRequestTimeout = 5;
    private static final int piwikQueryDefaultCapacity = 12;

    // private
    // @todo: doc
    private Piwik piwik;
    private boolean isDispatching = false;
    private int dispatchInterval = 120;
    private int siteId;
    private URL apiUrl;
    private String userId;
    private String authToken;
    private long sessionTimeoutMillis;
    private long sessionStartedMillis;
    private HashMap<String, String> queryParams;
    private HashMap<String, CustomVariables> customVariables = new HashMap<String, CustomVariables>(2);
    private static final Logger LOGGER = Logger.getLogger(TrackerURLBuilder.class.getName());

    private Tracker(String url, int siteId) throws MalformedURLException {
        this.setAPIUrl(url);
        this.siteId = siteId;
        this.setNewSession();
        this.setSessionTimeout(piwikDefaultSessionTimeout);
        this.clearQueryParams();
        this.setUserId(UUID.randomUUID().toString().substring(0, 32));
    }

    public Tracker(String url, int siteId, String authToken, Piwik piwik) throws MalformedURLException {
        this(url, siteId);
        this.authToken = authToken;
        this.piwik = piwik;
    }

    public boolean dispatch() {
        if (this.isDispatching) {
            return false;
        } else {
            this.isDispatching = true;
            //commitEvents();
            return true;
        }
    }

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

    public void setUserId(String userId){
        this.userId = userId;
    }

    public void setDispatchInterval(int dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
        //@todo: stop dispatching loop when dispatchInterval < 1
    }

    /**
     * Sets the screen resolution of the browser which sends the request.
     *
     * @param width the screen width as an int value
     * @param height the screen height as an int value
     */
    public void setResolution(final int width, final int height) {
        set(QueryParams.SCREEN_RESOLOUTION, String.format("%sx%s", width, height));
    }

    private CustomVariables getCustomVariables(String namespace){
        CustomVariables vars = customVariables.get(namespace);
        if (vars == null){
            vars = new CustomVariables();
            customVariables.put(namespace, vars);
        }
        return vars;
    }

    private void setCustomVariable(String namespace, int index, String name, String value){
        getCustomVariables(namespace).put(index, name, value);
    }

    public void setUserCustomVariable(int index, String name, String value){
        this.setCustomVariable(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }

    public void setScreenCustomVariable(int index, String name, String value){
        this.setCustomVariable(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, index, name, value);
    }


    public void setScreenTitle(String title){
        set(QueryParams.ACTION_NAME, title);
    }

    /**
     * @todo remove?
     * @param fragment Current Fragment instance
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void fragmentStart(Fragment fragment){
        int id = fragment.getId();
    }

    /**
     * Set action_name param from activity's title and track view
     * @param activity Current Activity instance
     */
    public void activityStart(Activity activity){
        setScreenTitle(activity.getTitle().toString());
        trackScreenView("");
    }

    /**
     * Force dispatching events if main activity is about to stop
     * @param activity Current Activity instance
     */
    public void activityStop(Activity activity) {
        if(activity.isTaskRoot()){
            this.dispatch();
        }
    }

    /**
     * Session handling
     * Custom variables of scope "user" are cleared when new session is stared.
     */
    public void setNewSession() {
        this.touchSession();
        this.set(QueryParams.SESSION_START, defaultTrueValue);
        this.getCustomVariables(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES).clear();
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

    private void clearQueryParams() {
        if (queryParams != null) {
            queryParams.clear();
        }
        queryParams = new HashMap<String, String>(piwikQueryDefaultCapacity);

    }

    protected String getParamUlr() {
        String action = this.queryParams.get(QueryParams.ACTION_NAME);
        String url = this.queryParams.get(QueryParams.URL);

        url = (url != null) ? url : "";
        if ("".equals(url)) {
            url = (action != null) ? action : "";
        }

        return "http://" + this.piwik.getApplicationName() + "/" + url;
    }

    public void trackScreenView(String url) {
        set(QueryParams.URL, url);
        doTrack();
    }

    public void trackScreenView(String url, String title) {
        setScreenTitle(title);
        trackScreenView(url);
    }

    /**
     * Events are a useful way to collect data about a user's interaction with interactive components of your app,
     * like button presses or the use of a particular item in a game.
     * @param category (required) – this String defines the event category.
     *                 You might define event categories based on the class of user actions,
     *                 like clicks or gestures or voice commands, or you might define them based upon the
     *                 features available in your application (play, pause, fast forward, etc.).
     * @param action (required) this String defines the specific event action within the category specified.
     *               In the example, we are basically saying that the category of the event is user clicks,
     *               and the action is a button click.
     * @param label defines a label associated with the event. For example, if you have multiple Button controls on a
     *              screen, you might use the label to specify the specific View control identifier that was clicked.
     * @param value defines a numeric value associated with the event. For example, if you were tracking "Buy"
     *              button clicks, you might log the number of items being purchased, or their total cost.
     *
     */
    public void trackEvent(String category, String action, String label, Integer value){
        if(set(QueryParams.EVENT_CATEGORY, category) && set(QueryParams.EVENT_ACTION, action)){
            set(QueryParams.EVENT_NAME, label);
            set(QueryParams.EVENT_VALUE, value);
            doTrack();
        }
    }

    public void trackEvent(String category, String action, String label){
        trackEvent(category, action, label, null);
    }

    public void trackEvent(String category, String action){
        trackEvent(category, action, null, null);
    }

    public void trackGoal(Integer idGoal){
        set(QueryParams.GOAL_ID, idGoal);
        doTrack();
    }

    /**
     * Set up default values
     */
    protected void beforeTracking(){
        this.set(QueryParams.API_VERSION, defaultAPIVersionValue);
        this.set(QueryParams.SITE_ID, siteId);
        this.set(QueryParams.RECORD, defaultRecordValue);
        this.set(QueryParams.URL, this.getParamUlr());
        this.set(QueryParams.VISITOR_ID, this.userId);
        this.set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, this.getCustomVariables(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES).toString());
        this.set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, this.getCustomVariables(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES).toString());
        this.checkSessionTimeout();
        this.touchSession();
    }

    /**
     * Clean up all screen view related params
     */
    protected void afterTracking(){
        this.clearQueryParams();
        this.getCustomVariables(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES).clear();
    }

    protected void doTrack(){
        this.beforeTracking();
        this.queueEvent();
        this.afterTracking();
    }

    protected void queueEvent() {
        TrackerURLBuilder urlBuilder = new TrackerURLBuilder(siteId, apiUrl.toString());
        URL url = urlBuilder.getPageTrackURL("/");

        if (piwik.isOptOut()) {
            LOGGER.log(Level.INFO, "isOptOut");
        } else if (piwik.isDryRun()) {
            LOGGER.log(Level.INFO, "dryRun");
        } else {
            //queue.add(url);
        }
    }

    /**
     * Sets the url of the piwik installation the tracker will track to.
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

    // Piwik query parameter names
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
        public static final String URL = "url";
        public static final String VISITOR_ID = "_id";

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

