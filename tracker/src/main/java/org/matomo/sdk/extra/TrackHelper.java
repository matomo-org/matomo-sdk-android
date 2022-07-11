package org.matomo.sdk.extra;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.matomo.sdk.Matomo;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.Tracker;
import org.matomo.sdk.tools.ActivityHelper;
import org.matomo.sdk.tools.CurrencyFormatter;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class TrackHelper {
    private static final String TAG = Matomo.tag(TrackHelper.class);
    protected final TrackMe mBaseTrackMe;

    private TrackHelper() {
        this(null);
    }

    private TrackHelper(@Nullable TrackMe baseTrackMe) {
        if (baseTrackMe == null) baseTrackMe = new TrackMe();
        mBaseTrackMe = baseTrackMe;
    }

    public static TrackHelper track() {
        return new TrackHelper();
    }

    public static TrackHelper track(@Nullable TrackMe base) {
        return new TrackHelper(base);
    }

    static abstract class BaseEvent {

        private final TrackHelper mBaseBuilder;

        BaseEvent(TrackHelper baseBuilder) {
            mBaseBuilder = baseBuilder;
        }

        TrackMe getBaseTrackMe() {
            return mBaseBuilder.mBaseTrackMe;
        }

        /**
         * May throw an {@link IllegalArgumentException} if the TrackMe was build with incorrect arguments.
         */
        public abstract TrackMe build();

        public void with(MatomoApplication matomoApplication) {
            with(matomoApplication.getTracker());
        }

        public void with(Tracker tracker) {
            TrackMe trackMe = build();
            tracker.track(trackMe);
        }

        public boolean safelyWith(MatomoApplication matomoApplication) {
            return safelyWith(matomoApplication.getTracker());
        }

        /**
         * {@link #build()} can throw an exception on illegal arguments.
         * This can be used to avoid crashes when using dynamic {@link TrackMe} arguments.
         *
         * @return false if an error occured, true if the TrackMe has been submitted to be dispatched.
         */
        public boolean safelyWith(Tracker tracker) {
            try {
                TrackMe trackMe = build();
                tracker.track(trackMe);
            } catch (IllegalArgumentException e) {
                Timber.e(e);
                return false;
            }
            return true;
        }
    }

    /**
     * To track a screenview.
     *
     * @param path Example: "/user/settings/billing"
     * @return an object that allows addition of further details.
     */
    public Screen screen(String path) {
        return new Screen(this, path);
    }

    /**
     * Calls {@link #screen(String)} for an activity.
     * Uses the activity-stack as path and activity title as names.
     *
     * @param activity the activity to track
     */
    public Screen screen(Activity activity) {
        String breadcrumbs = ActivityHelper.getBreadcrumbs(activity);
        return new Screen(this, ActivityHelper.breadcrumbsToPath(breadcrumbs)).title(breadcrumbs);
    }

    public static class Screen extends BaseEvent {
        private final String mPath;
        private final CustomVariables mCustomVariables = new CustomVariables();
        private final Map<Integer, String> mCustomDimensions = new HashMap<>();
        private String mTitle;
        private String mCampaignName;
        private String mCampaignKeyword;

        Screen(TrackHelper baseBuilder, String path) {
            super(baseBuilder);
            mPath = path;
        }

        /**
         * The title of the action being tracked. It is possible to use slashes / to set one or several categories for this action.
         *
         * @param title Example: Help / Feedback will create the Action Feedback in the category Help.
         * @return this object to allow chaining calls
         */
        public Screen title(String title) {
            mTitle = title;
            return this;
        }

        /**
         * Requires <a href="https://plugins.matomo.org/CustomDimensions">Custom Dimensions</a> plugin (server-side)
         *
         * @param index          accepts values greater than 0
         * @param dimensionValue is limited to 255 characters, you can pass null to delete a value
         */
        public Screen dimension(int index, String dimensionValue) {
            mCustomDimensions.put(index, dimensionValue);
            return this;
        }

        /**
         * Custom Variable valid per screen.
         * Only takes effect when setting prior to tracking the screen view.
         *
         * @see org.matomo.sdk.extra.CustomDimension and {@link #dimension(int, String)}
         * @deprecated Consider using <a href="http://matomo.org/docs/custom-dimensions/">Custom Dimensions</a>
         */
        @Deprecated
        public Screen variable(int index, String name, String value) {
            mCustomVariables.put(index, name, value);
            return this;
        }

        /**
         * The marketing campaign for this visit if the user opens the app for example because of an
         * ad or a newsletter. Used to populate the <i>Referrers > Campaigns</i> report.
         *
         * @param name    the name of the campaign
         * @param keyword the keyword of the campaign
         * @return this object to allow chaining calls
         */
        public Screen campaign(String name, String keyword) {
            mCampaignName = name;
            mCampaignKeyword = keyword;
            return this;
        }

        @Override
        public TrackMe build() {
            if (mPath == null) {
                throw new IllegalArgumentException("Screen tracking requires a non-empty path");
            }

            final TrackMe trackMe = new TrackMe(getBaseTrackMe())
                    .set(QueryParams.URL_PATH, mPath)
                    .set(QueryParams.ACTION_NAME, mTitle)
                    .set(QueryParams.CAMPAIGN_NAME, mCampaignName)
                    .set(QueryParams.CAMPAIGN_KEYWORD, mCampaignKeyword);
            if (mCustomVariables.size() > 0) {
                //noinspection deprecation
                trackMe.set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, mCustomVariables.toString());
            }
            for (Map.Entry<Integer, String> entry : mCustomDimensions.entrySet()) {
                CustomDimension.setDimension(trackMe, entry.getKey(), entry.getValue());
            }
            return trackMe;
        }
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
     * @return an object that allows addition of further details.
     */
    public EventBuilder event(String category, String action) {
        return new EventBuilder(this, category, action);
    }

    public static class EventBuilder extends BaseEvent {
        private final String mCategory;
        private final String mAction;
        private String mPath;
        private String mName;
        private Float mValue;

        EventBuilder(TrackHelper builder, String category, String action) {
            super(builder);
            mCategory = category;
            mAction = action;
        }

        /**
         * The path under which this event occurred.
         * Example: "/user/settings/billing", if you pass NULL, the last path set by #trackScreenView will be used.
         */
        public EventBuilder path(String path) {
            mPath = path;
            return this;
        }

        /**
         * Defines a label associated with the event.
         * For example, if you have multiple Button controls on a screen, you might use the label to specify the specific View control identifier that was clicked.
         */
        public EventBuilder name(String name) {
            mName = name;
            return this;
        }

        /**
         * Defines a numeric value associated with the event.
         * For example, if you were tracking "Buy" button clicks, you might log the number of items being purchased, or their total cost.
         */
        public EventBuilder value(Float value) {
            mValue = value;
            return this;
        }

        @Override
        public TrackMe build() {
            TrackMe trackMe = new TrackMe(getBaseTrackMe())
                    .set(QueryParams.URL_PATH, mPath)
                    .set(QueryParams.EVENT_CATEGORY, mCategory)
                    .set(QueryParams.EVENT_ACTION, mAction)
                    .set(QueryParams.EVENT_NAME, mName);
            if (mValue != null) trackMe.set(QueryParams.EVENT_VALUE, mValue);
            return trackMe;
        }
    }

    /**
     * By default, Goals in Matomo are defined as "matching" parts of the screen path or screen title.
     * In this case a conversion is logged automatically. In some situations, you may want to trigger
     * a conversion manually on other types of actions, for example:
     * when a user submits a form
     * when a user has stayed more than a given amount of time on the page
     * when a user does some interaction in your Android application
     *
     * @param idGoal id of goal as defined in matomo goal settings
     */
    public Goal goal(int idGoal) {
        return new Goal(this, idGoal);
    }

    public static class Goal extends BaseEvent {
        private final int mIdGoal;
        private Float mRevenue;

        Goal(TrackHelper baseBuilder, int idGoal) {
            super(baseBuilder);
            mIdGoal = idGoal;
        }

        /**
         * Tracking request will trigger a conversion for the goal of the website being tracked with this ID
         *
         * @param revenue a monetary value that was generated as revenue by this goal conversion.
         */
        public Goal revenue(Float revenue) {
            mRevenue = revenue;
            return this;
        }

        @Override
        public TrackMe build() {
            if (mIdGoal < 0) {
                throw new IllegalArgumentException("Goal id needs to be >=0");
            }

            TrackMe trackMe = new TrackMe(getBaseTrackMe()).set(QueryParams.GOAL_ID, mIdGoal);
            if (mRevenue != null) trackMe.set(QueryParams.REVENUE, mRevenue);
            return trackMe;
        }
    }

    /**
     * Tracks an  <a href="http://matomo.org/faq/new-to-matomo/faq_71/">Outlink</a>
     *
     * @param url HTTPS, HTTP and FTPare valid
     * @return this Tracker for chaining
     */
    public Outlink outlink(URL url) {
        return new Outlink(this, url);
    }

    public static class Outlink extends BaseEvent {
        private final URL mURL;

        Outlink(TrackHelper baseBuilder, URL url) {
            super(baseBuilder);
            mURL = url;
        }

        @Override
        public TrackMe build() {
            if (mURL == null || mURL.toExternalForm().length() == 0) {
                throw new IllegalArgumentException("Outlink tracking requires a non-empty URL");
            }
            if (!mURL.getProtocol().equals("http") && !mURL.getProtocol().equals("https") && !mURL.getProtocol().equals("ftp")) {
                throw new IllegalArgumentException("Only http|https|ftp is supported for outlinks");
            }

            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.LINK, mURL.toExternalForm())
                    .set(QueryParams.URL_PATH, mURL.toExternalForm());
        }
    }

    /**
     * Tracks an  <a href="http://matomo.org/docs/site-search/">site search</a>
     *
     * @param keyword Searched query in the app
     * @return this Tracker for chaining
     */
    public Search search(String keyword) {
        return new Search(this, keyword);
    }

    public static class Search extends BaseEvent {
        private final String mKeyword;
        private String mCategory;
        private Integer mCount;

        Search(TrackHelper baseBuilder, String keyword) {
            super(baseBuilder);
            mKeyword = keyword;
        }

        /**
         * You can optionally specify a search category with this parameter.
         *
         * @return this object, to chain calls.
         */
        public Search category(String category) {
            mCategory = category;
            return this;
        }

        /**
         * We recommend to set the search count to the number of search results displayed on the results page.
         * When keywords are tracked with a count of 0, they will appear in the "No Result Search Keyword" report.
         *
         * @return this object, to chain calls.
         */
        public Search count(Integer count) {
            mCount = count;
            return this;
        }

        @Override
        public TrackMe build() {
            TrackMe trackMe = new TrackMe(getBaseTrackMe())
                    .set(QueryParams.SEARCH_KEYWORD, mKeyword)
                    .set(QueryParams.SEARCH_CATEGORY, mCategory);
            if (mCount != null) trackMe.set(QueryParams.SEARCH_NUMBER_OF_HITS, mCount);
            return trackMe;
        }
    }

    /**
     * Sends a download event for this app.
     * This only triggers an event once per app version unless you force it.<p>
     * {@link Download#force()}
     * <p class="note">
     * Resulting download url:<p>
     * Case {@link org.matomo.sdk.extra.DownloadTracker.Extra.ApkChecksum}:<br>
     * http://packageName:versionCode/apk-md5-checksum<br>
     * <p>
     * Case {@link org.matomo.sdk.extra.DownloadTracker.Extra.None}:<br>
     * http://packageName:versionCode<p>
     *
     * @return this object, to chain calls.
     */
    public Download download(DownloadTracker downloadTracker) {
        return new Download(downloadTracker, this);
    }

    public Download download() {
        return new Download(null, this);
    }

    public static class Download {
        private DownloadTracker mDownloadTracker;
        private final TrackHelper mBaseBuilder;
        private DownloadTracker.Extra mExtra = new DownloadTracker.Extra.None();
        private boolean mForced = false;
        private String mVersion;

        Download(DownloadTracker downloadTracker, TrackHelper baseBuilder) {
            mDownloadTracker = downloadTracker;
            mBaseBuilder = baseBuilder;
        }

        /**
         * Sets the identifier type for this download
         *
         * @param identifier {@link org.matomo.sdk.extra.DownloadTracker.Extra.ApkChecksum} or {@link org.matomo.sdk.extra.DownloadTracker.Extra.None}
         * @return this object, to chain calls.
         */
        public Download identifier(DownloadTracker.Extra identifier) {
            mExtra = identifier;
            return this;
        }

        /**
         * Normally a download event is only fired once per app version.
         * If the download has already been tracked for this version, nothing happens.
         * Calling this will force this download to be tracked.
         *
         * @return this object, to chain calls.
         */
        public Download force() {
            mForced = true;
            return this;
        }

        /**
         * To track specific app versions. Useful if the app can change without the apk being updated (e.g. hybrid apps/web apps).
         *
         * @param version by default {@link android.content.pm.PackageInfo#versionCode} is used.
         * @return this object, to chain calls.
         */
        public Download version(String version) {
            mVersion = version;
            return this;
        }

        public void with(Tracker tracker) {
            if (mDownloadTracker == null) mDownloadTracker = new DownloadTracker(tracker);
            if (mVersion != null) mDownloadTracker.setVersion(mVersion);
            if (mForced) mDownloadTracker.trackNewAppDownload(mBaseBuilder.mBaseTrackMe, mExtra);
            else mDownloadTracker.trackOnce(mBaseBuilder.mBaseTrackMe, mExtra);
        }
    }

    /**
     * Tracking the impressions
     *
     * @param contentName The name of the content. For instance 'Ad Foo Bar'
     */
    public ContentImpression impression(String contentName) {
        return new ContentImpression(this, contentName);
    }

    public static class ContentImpression extends BaseEvent {
        private final String mContentName;
        private String mContentPiece;
        private String mContentTarget;

        ContentImpression(TrackHelper baseBuilder, String contentName) {
            super(baseBuilder);
            mContentName = contentName;
        }

        /**
         * @param contentPiece The actual content. For instance the path to an image, video, audio, any text
         */
        public ContentImpression piece(String contentPiece) {
            mContentPiece = contentPiece;
            return this;
        }

        /**
         * @param contentTarget The target of the content. For instance the URL of a landing page.
         */
        public ContentImpression target(String contentTarget) {
            mContentTarget = contentTarget;
            return this;
        }

        @Override
        public TrackMe build() {
            if (mContentName == null || mContentName.length() == 0) {
                throw new IllegalArgumentException("Tracking content impressions requires a non-empty content-name");
            }
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.CONTENT_NAME, mContentName)
                    .set(QueryParams.CONTENT_PIECE, mContentPiece)
                    .set(QueryParams.CONTENT_TARGET, mContentTarget);
        }
    }

    /**
     * Tracking the interactions<p>
     * To map an interaction to an impression make sure to set the same value for contentName and contentPiece as
     * the impression has.
     *
     * @param contentInteraction The name of the interaction with the content. For instance a 'click'
     * @param contentName        The name of the content. For instance 'Ad Foo Bar'
     */
    public ContentInteraction interaction(String contentName, String contentInteraction) {
        return new ContentInteraction(this, contentName, contentInteraction);
    }

    public static class ContentInteraction extends BaseEvent {
        private final String mContentName;
        private final String mInteraction;
        private String mContentPiece;
        private String mContentTarget;

        ContentInteraction(TrackHelper baseBuilder, String contentName, String interaction) {
            super(baseBuilder);
            mContentName = contentName;
            mInteraction = interaction;
        }

        /**
         * @param contentPiece The actual content. For instance the path to an image, video, audio, any text
         */
        public ContentInteraction piece(String contentPiece) {
            mContentPiece = contentPiece;
            return this;
        }

        /**
         * @param contentTarget The target the content leading to when an interaction occurs. For instance the URL of a landing page.
         */
        public ContentInteraction target(String contentTarget) {
            mContentTarget = contentTarget;
            return this;
        }

        @Override
        public TrackMe build() {
            if (mContentName == null || mContentName.length() == 0) {
                throw new IllegalArgumentException("Content name needs to be non-empty");
            }
            if (mInteraction == null || mInteraction.length() == 0) {
                throw new IllegalArgumentException("Interaction name needs to be non-empty");
            }

            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.CONTENT_NAME, mContentName)
                    .set(QueryParams.CONTENT_PIECE, mContentPiece)
                    .set(QueryParams.CONTENT_TARGET, mContentTarget)
                    .set(QueryParams.CONTENT_INTERACTION, mInteraction);
        }
    }


    /**
     * Tracks a shopping cart. Call this javascript function every time a user is adding, updating
     * or deleting a product from the cart.
     *
     * @param grandTotal total value of items in cart
     */
    public CartUpdate cartUpdate(int grandTotal) {
        return new CartUpdate(this, grandTotal);
    }

    public static class CartUpdate extends BaseEvent {
        private final int mGrandTotal;
        private EcommerceItems mEcommerceItems;

        CartUpdate(TrackHelper baseBuilder, int grandTotal) {
            super(baseBuilder);
            mGrandTotal = grandTotal;
        }

        /**
         * @param items Items included in the cart
         */
        public CartUpdate items(EcommerceItems items) {
            mEcommerceItems = items;
            return this;
        }

        @Override
        public TrackMe build() {
            if (mEcommerceItems == null) mEcommerceItems = new EcommerceItems();
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.GOAL_ID, 0)
                    .set(QueryParams.REVENUE, CurrencyFormatter.priceString(mGrandTotal))
                    .set(QueryParams.ECOMMERCE_ITEMS, mEcommerceItems.toJson());
        }
    }

    /**
     * Tracks an Ecommerce order, including any ecommerce item previously added to the order.  All
     * monetary values should be passed as an integer number of cents (or the smallest integer unit
     * for your currency)
     *
     * @param orderId    (required) A unique string identifying the order
     * @param grandTotal (required) total amount of the order, in cents
     */
    public Order order(String orderId, int grandTotal) {
        return new Order(this, orderId, grandTotal);
    }

    public static class Order extends BaseEvent {
        private final String mOrderId;
        private final int mGrandTotal;
        private EcommerceItems mEcommerceItems;
        private Integer mDiscount;
        private Integer mShipping;
        private Integer mTax;
        private Integer mSubTotal;

        Order(TrackHelper baseBuilder, String orderId, int grandTotal) {
            super(baseBuilder);
            mOrderId = orderId;
            mGrandTotal = grandTotal;
        }

        /**
         * @param subTotal the subTotal for the order, in cents
         */
        public Order subTotal(Integer subTotal) {
            mSubTotal = subTotal;
            return this;
        }

        /**
         * @param tax the tax for the order, in cents
         */
        public Order tax(Integer tax) {
            mTax = tax;
            return this;
        }

        /**
         * @param shipping the shipping for the order, in cents
         */
        public Order shipping(Integer shipping) {
            mShipping = shipping;
            return this;
        }

        /**
         * @param discount the discount for the order, in cents
         */
        public Order discount(Integer discount) {
            mDiscount = discount;
            return this;
        }

        /**
         * @param items the items included in the order
         */
        public Order items(EcommerceItems items) {
            mEcommerceItems = items;
            return this;
        }

        @Override
        public TrackMe build() {
            if (mEcommerceItems == null) mEcommerceItems = new EcommerceItems();
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.GOAL_ID, 0)
                    .set(QueryParams.ORDER_ID, mOrderId)
                    .set(QueryParams.REVENUE, CurrencyFormatter.priceString(mGrandTotal))
                    .set(QueryParams.ECOMMERCE_ITEMS, mEcommerceItems.toJson())
                    .set(QueryParams.SUBTOTAL, CurrencyFormatter.priceString(mSubTotal))
                    .set(QueryParams.TAX, CurrencyFormatter.priceString(mTax))
                    .set(QueryParams.SHIPPING, CurrencyFormatter.priceString(mShipping))
                    .set(QueryParams.DISCOUNT, CurrencyFormatter.priceString(mDiscount));
        }
    }

    /**
     * Caught exceptions are errors in your app for which you've defined exception handling code,
     * such as the occasional timeout of a network connection during a request for data.
     * <p>
     * This is just a different way to define an event.
     * Keep in mind Matomo is not a crash tracker, use this sparingly.
     * <p>
     * For this to be useful you should ensure that proguard does not remove all classnames and line numbers.
     * Also note that if this is used across different app versions and obfuscation is used, the same exception might be mapped to different obfuscated names by proguard.
     * This would mean the same exception (event) is tracked as different events by Matomo.
     *
     * @param throwable exception instance
     */
    public Exception exception(Throwable throwable) {
        return new Exception(this, throwable);
    }

    public static class Exception extends BaseEvent {
        private final Throwable mThrowable;
        private String mDescription;
        private boolean mIsFatal;

        Exception(TrackHelper baseBuilder, Throwable throwable) {
            super(baseBuilder);
            mThrowable = throwable;
        }

        /**
         * @param description exception message
         */
        public Exception description(String description) {
            mDescription = description;
            return this;
        }

        /**
         * @param isFatal true if it's fatal exception
         */
        public Exception fatal(boolean isFatal) {
            mIsFatal = isFatal;
            return this;
        }

        @Override
        public TrackMe build() {
            String className;
            try {
                StackTraceElement trace = mThrowable.getStackTrace()[0];
                className = trace.getClassName() + "/" + trace.getMethodName() + ":" + trace.getLineNumber();
            } catch (java.lang.Exception e) {
                Timber.tag(TAG).w(e, "Couldn't get stack info");
                className = mThrowable.getClass().getName();
            }
            String actionName = "exception/" + (mIsFatal ? "fatal/" : "") + (className + "/") + mDescription;
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.ACTION_NAME, actionName)
                    .set(QueryParams.EVENT_CATEGORY, "Exception")
                    .set(QueryParams.EVENT_ACTION, className)
                    .set(QueryParams.EVENT_NAME, mDescription)
                    .set(QueryParams.EVENT_VALUE, mIsFatal ? 1 : 0);
        }
    }

    /**
     * This will create an exception handler that wraps any existing exception handler.
     * Exceptions will be caught, tracked, dispatched and then rethrown to the previous exception handler.
     * <p>
     * Be wary of relying on this for complete crash tracking..
     * Think about how to deal with older app versions still throwing already fixed exceptions.
     * <p>
     * See discussion here: https://github.com/matomo-org/matomo-sdk-android/issues/28
     */
    public UncaughtExceptions uncaughtExceptions() {
        return new UncaughtExceptions(this);
    }

    public static class UncaughtExceptions {
        private final TrackHelper mBaseBuilder;

        UncaughtExceptions(TrackHelper baseBuilder) {
            mBaseBuilder = baseBuilder;
        }

        /**
         * @param tracker the tracker that should receive the exception events.
         * @return returns the new (but already active) exception handler.
         */
        public Thread.UncaughtExceptionHandler with(Tracker tracker) {
            if (Thread.getDefaultUncaughtExceptionHandler() instanceof MatomoExceptionHandler) {
                throw new RuntimeException("Trying to wrap an existing MatomoExceptionHandler.");
            }
            Thread.UncaughtExceptionHandler handler = new MatomoExceptionHandler(tracker, mBaseBuilder.mBaseTrackMe);
            Thread.setDefaultUncaughtExceptionHandler(handler);
            return handler;
        }
    }

    /**
     * This method will bind a tracker to your application,
     * causing it to automatically track Activities with {@link #screen(Activity)} within your app.
     *
     * @param app your app
     * @return the registered callback, you need this if you wanted to unregister the callback again
     */
    public AppTracking screens(Application app) {
        return new AppTracking(this, app);
    }

    public static class AppTracking {
        private final Application mApplication;
        private final TrackHelper mBaseBuilder;

        public AppTracking(TrackHelper baseBuilder, Application application) {
            mBaseBuilder = baseBuilder;
            mApplication = application;
        }

        /**
         * @param tracker the tracker to use
         * @return the registered callback, you need this if you wanted to unregister the callback again
         */
        public Application.ActivityLifecycleCallbacks with(final Tracker tracker) {
            final Application.ActivityLifecycleCallbacks callback = new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle bundle) {

                }

                @Override
                public void onActivityStarted(Activity activity) {

                }

                @Override
                public void onActivityResumed(Activity activity) {
                    mBaseBuilder.screen(activity).with(tracker);
                }

                @Override
                public void onActivityPaused(Activity activity) {

                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (activity != null && activity.isTaskRoot()) {
                        tracker.dispatch();
                    }
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {

                }
            };
            mApplication.registerActivityLifecycleCallbacks(callback);
            return callback;
        }
    }

    public Dimension dimension(int id, String value) {
        return new Dimension(mBaseTrackMe).dimension(id, value);
    }

    public static class Dimension extends TrackHelper {

        Dimension(TrackMe base) {
            super(base);
        }

        @Override
        public Dimension dimension(int id, String value) {
            CustomDimension.setDimension(mBaseTrackMe, id, value);
            return this;
        }
    }


    /**
     * To track visit scoped custom variables.
     *
     * @see CustomVariables#put(int, String, String)
     * @deprecated Consider using <a href="http://matomo.org/docs/custom-dimensions/">Custom Dimensions</a>
     */
    @Deprecated
    public VisitVariables visitVariables(int id, String name, String value) {
        CustomVariables customVariables = new CustomVariables();
        customVariables.put(id, name, value);
        return visitVariables(customVariables);
    }

    /**
     * To track visit scoped custom variables.
     *
     * @deprecated Consider using <a href="http://matomo.org/docs/custom-dimensions/">Custom Dimensions</a>
     */
    @Deprecated
    public VisitVariables visitVariables(CustomVariables customVariables) {
        return new VisitVariables(this, customVariables);
    }

    @SuppressWarnings("deprecation")
    public static class VisitVariables extends TrackHelper {

        public VisitVariables(TrackHelper baseBuilder, CustomVariables customVariables) {
            super(baseBuilder.mBaseTrackMe);
            CustomVariables mergedVariables = new CustomVariables(mBaseTrackMe.get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
            mergedVariables.putAll(customVariables);
            mBaseTrackMe.set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, mergedVariables.toString());
        }

        /**
         * @see CustomVariables#put(int, String, String)
         */
        public VisitVariables visitVariables(int id, String name, String value) {
            CustomVariables customVariables = new CustomVariables(mBaseTrackMe.get(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES));
            customVariables.put(id, name, value);
            mBaseTrackMe.set(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, customVariables.toString());
            return this;
        }
    }
}
