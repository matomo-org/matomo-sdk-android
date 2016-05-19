package org.piwik.sdk;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.piwik.sdk.ecommerce.EcommerceItems;
import org.piwik.sdk.tools.ActivityHelper;
import org.piwik.sdk.tools.CurrencyFormatter;

import java.net.URL;

import timber.log.Timber;

public class TrackHelper {
    private final TrackMe mBaseTrackMe;

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

    public static TrackHelper track(@NonNull TrackMe base) {
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

        @Nullable
        public abstract TrackMe build();

        public void with(@NonNull PiwikApplication piwikApplication) {
            with(piwikApplication.getTracker());
        }

        public void with(@NonNull Tracker tracker) {
            TrackMe trackMe = build();
            if (trackMe != null) tracker.track(trackMe);
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
        private String mTitle;

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
         * Just like {@link Tracker#setVisitCustomVariable(int, String, String)} but only valid per screen.
         * Only takes effect when setting prior to tracking the screen view.
         */
        public Screen variable(int index, String name, String value) {
            mCustomVariables.put(index, name, value);
            return this;
        }

        @Nullable
        @Override
        public TrackMe build() {
            if (mPath == null) return null;
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES, mCustomVariables.toString())
                    .set(QueryParams.URL_PATH, mPath)
                    .set(QueryParams.ACTION_NAME, mTitle);
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
    public EventBuilder event(@NonNull String category, @NonNull String action) {
        return new EventBuilder(this, category, action);
    }

    public static class EventBuilder extends BaseEvent {
        @NonNull private final String mCategory;
        @NonNull private final String mAction;
        private String mPath;
        private String mName;
        private Float mValue;

        EventBuilder(TrackHelper builder, @NonNull String category, @NonNull String action) {
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

        @Nullable
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
     * By default, Goals in Piwik are defined as "matching" parts of the screen path or screen title.
     * In this case a conversion is logged automatically. In some situations, you may want to trigger
     * a conversion manually on other types of actions, for example:
     * when a user submits a form
     * when a user has stayed more than a given amount of time on the page
     * when a user does some interaction in your Android application
     *
     * @param idGoal id of goal as defined in piwik goal settings
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

        @Nullable
        @Override
        public TrackMe build() {
            if (mIdGoal < 0) return null;
            TrackMe trackMe = new TrackMe(getBaseTrackMe()).set(QueryParams.GOAL_ID, mIdGoal);
            if (mRevenue != null) trackMe.set(QueryParams.REVENUE, mRevenue);
            return trackMe;
        }
    }

    /**
     * Tracks an  <a href="http://piwik.org/faq/new-to-piwik/faq_71/">Outlink</a>
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

        @Nullable
        @Override
        public TrackMe build() {
            if (!mURL.getProtocol().equals("http") && !mURL.getProtocol().equals("https") && !mURL.getProtocol().equals("ftp")) {
                return null;
            }
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.LINK, mURL.toExternalForm())
                    .set(QueryParams.URL_PATH, mURL.toExternalForm());
        }
    }

    /**
     * Sends a download event for this app.
     * This only triggers an event once per app version unless you force it.<p/>
     * {@link Download#force()}
     * <p class="note">
     * Resulting download url:<p/>
     * Case {@link DownloadTracker.Extra#APK_CHECKSUM}:<br/>
     * http://packageName:versionCode/apk-md5-checksum<br/>
     * Usually the installer-packagename is something like "com.android.vending" (Google Play),
     * but users can modify this value, don't be surprised by some random values.<p/>
     * <p/>
     * Case {@link DownloadTracker.Extra#NONE}:<br/>
     * http://packageName:versionCode<p/>
     *
     * @return this object, to chain calls.
     */
    public Download download() {
        return new Download(this);
    }

    public static class Download {
        private final TrackHelper mBaseBuilder;
        private DownloadTracker.Extra mExtra = DownloadTracker.Extra.NONE;
        private boolean mForced = false;
        private String mVersion;

        Download(TrackHelper baseBuilder) {
            mBaseBuilder = baseBuilder;
        }

        /**
         * Sets the identifier type for this download
         *
         * @param identifier {@link DownloadTracker.Extra#APK_CHECKSUM} or {@link DownloadTracker.Extra#NONE}
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
            final DownloadTracker downloadTracker = new DownloadTracker(tracker, mBaseBuilder.mBaseTrackMe);
            if (mVersion != null) downloadTracker.setVersion(mVersion);
            if (mForced) {
                downloadTracker.trackNewAppDownload(mExtra);
            } else {
                downloadTracker.trackOnce(mExtra);
            }
        }
    }

    /**
     * Tracking the impressions
     *
     * @param contentName The name of the content. For instance 'Ad Foo Bar'
     */
    public ContentImpression impression(@NonNull String contentName) {
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

        @Nullable
        @Override
        public TrackMe build() {
            if (TextUtils.isEmpty(mContentName)) return null;
            return new TrackMe(getBaseTrackMe())
                    .set(QueryParams.CONTENT_NAME, mContentName)
                    .set(QueryParams.CONTENT_PIECE, mContentPiece)
                    .set(QueryParams.CONTENT_TARGET, mContentTarget);
        }
    }

    /**
     * Tracking the interactions</p>
     * To map an interaction to an impression make sure to set the same value for contentName and contentPiece as
     * the impression has.
     *
     * @param contentInteraction The name of the interaction with the content. For instance a 'click'
     * @param contentName        The name of the content. For instance 'Ad Foo Bar'
     */
    public ContentInteraction interaction(@NonNull String contentName, @NonNull String contentInteraction) {
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

        @Nullable
        @Override
        public TrackMe build() {
            if (TextUtils.isEmpty(mContentName) || TextUtils.isEmpty(mInteraction)) return null;
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

        @Nullable
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

        @Nullable
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
     * <p/>
     * This is just a different way to define an event.
     * Keep in mind Piwik is not a crash tracker, use this sparingly.
     * <p/>
     * For this to be useful you should ensure that proguard does not remove all classnames and line numbers.
     * Also note that if this is used across different app versions and obfuscation is used, the same exception might be mapped to different obfuscated names by proguard.
     * This would mean the same exception (event) is tracked as different events by Piwik.
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

        @Nullable
        @Override
        public TrackMe build() {
            String className;
            try {
                StackTraceElement trace = mThrowable.getStackTrace()[0];
                className = trace.getClassName() + "/" + trace.getMethodName() + ":" + trace.getLineNumber();
            } catch (java.lang.Exception e) {
                Timber.tag(Tracker.LOGGER_TAG).w(e, "Couldn't get stack info");
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
     * <p/>
     * Be wary of relying on this for complete crash tracking..
     * Think about how to deal with older app versions still throwing already fixed exceptions.
     * <p/>
     * See discussion here: https://github.com/piwik/piwik-sdk-android/issues/28
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
            if (Thread.getDefaultUncaughtExceptionHandler() instanceof PiwikExceptionHandler) {
                throw new RuntimeException("Trying to wrap an existing PiwikExceptionHandler.");
            }
            Thread.UncaughtExceptionHandler handler = new PiwikExceptionHandler(tracker, mBaseBuilder.mBaseTrackMe);
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
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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
}
