Piwik SDK for Android
========================

[![Build Status](https://travis-ci.org/piwik/piwik-sdk-android.svg?branch=master)](https://travis-ci.org/piwik/piwik-sdk-android) [ ![Download](https://api.bintray.com/packages/darken/maven/piwik-sdk-android/images/download.svg) ](https://bintray.com/darken/maven/piwik-sdk-android/_latestVersion) [![Coverage Status](https://coveralls.io/repos/piwik/piwik-sdk-android/badge.svg?branch=master&service=github)](https://coveralls.io/github/piwik/piwik-sdk-android?branch=master)

This document describes how to get started using the Piwik Tracking SDK for Android. 
[Piwik](http://piwik.org) is the leading open source web analytics platform 
that gives you valuable insights into your website's visitors, 
your marketing campaigns and much more, so you can optimize your strategy and experience of your visitors.

## Getting started

Integrating Piwik into your Android app
 
1. [Install Piwik](http://piwik.org/docs/installation/)
2. [Create a new website in the Piwik web interface](http://piwik.org/docs/manage-websites/). Copy the Website ID from "Settings > Websites".
3. [Include the library](#include-library)
4. [Initialize Tracker](#initialize-tracker).
5. [Track screen views, exceptions, goals and more](#tracker-usage).
6. [Advanced tracker usage](#advanced-tracker-usage)


### Include library
Add this to your apps build.gradle file:

```groovy
dependencies {
    repositories {
        jcenter()
    }
    // ...
    compile 'org.piwik.sdk:piwik-sdk:1.0.2'
}
```


### Initialize Tracker

#### Basic

You can simply extend your application with a 
[``PiwikApplication``](https://github.com/piwik/piwik-sdk-android/blob/master/piwik-sdk/src/main/java/org/piwik/sdk/PiwikApplication.java) class.
[This approach is used](https://github.com/piwik/piwik-sdk-android/blob/master/exampleapp/src/main/java/com/piwik/demo/DemoApp.java) in our demo app.

#### Advanced

Developers could manage the tracker lifecycle by themselves.
To ensure that the metrics are not over-counted, it is highly 
recommended that the tracker be created and managed in the Application class.

```java

import java.net.MalformedURLException;

public class YourApplication extends Application {
    private Tracker mPiwikTracker;

    public synchronized Tracker getTracker() {
        if (mPiwikTracker != null) {
            return mPiwikTracker;
        }

        try {
            mPiwikTracker = Piwik.getInstance(this).newTracker("http://your-piwik-domain.tld/piwik.php", 1);
        } catch (MalformedURLException e) {
            Log.w(Tracker.LOGGER_TAG, "url is malformed", e);
            return null;
        }

        return mPiwikTracker;
    }
    //...
}
```

Don't forget to add application name to your `AndroidManifest.xml` file.
 
```xml

    <application android:name=".YourApplication">
        <!-- activities goes here -->
    </application>
```


### Tracker Usage

#### Track screen views

To send a screen view set the screen path and titles on the tracker

```java

public class YourActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tracker tracker = ((PiwikApplication) getApplication()).getTracker();
        TrackHelper.track().screen("/your_activity").title("Title").with(tracker);
    }
}
```

#### Track events

To collect data about user's interaction with interactive components of your app, like button presses or the use of a particular item in a game 
use [trackEvent](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#trackEvent(java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)) 
method.

```java

TrackHelper.track().event("category", "action").name("label").value(1000f).with(tracker);
```

#### Track goals

If you want to trigger a conversion manually or track some user interaction simply call the method 
[trackGoal](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#trackGoal(java.lang.Integer)).
Read more about what is a [Goal in Piwik](http://piwik.org/docs/tracking-goals-web-analytics/).

```java

TrackHelper.track().goal(1).revenue(revenue).with(tracker)
```

#### Track custom vars

To track a custom name-value pair assigned to your users or screen views use 
[setVisitCustomVariable](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#setVisitCustomVariable(int, java.lang.String, java.lang.String))
and
[setScreenCustomVariable](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/TrackMe.html#setScreenCustomVariable(int, java.lang.String, java.lang.String))
methods. Those methods have to be called before a call to [trackScreenView](#track-screen-views).
More about [custom variables on piwik.org](http://piwik.org/docs/custom-variables/).


```java

Tracker tracker = ((PiwikApplication) getApplication()).getTracker();
tracker.setVisitCustomVariable(2, "Age", "99");
TrackHelper.track().screen("/path").variable(2, "Price", "0.99").with(tracker);
```

#### Track application downloads

To track the number of app downloads you may call the method [``trackAppDownload``](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#trackAppDownload())
This method uses ``SharedPreferences`` to ensures that tracking application downloading will be fired only once.

```java

TrackHelper.track().download().with(tracker);
```

#### Custom Dimensions
To track [Custom Dimensions](https://plugins.piwik.org/CustomDimensions) in scope Action or Visit
consider following example:

```java

Tracker tracker = ((YourApplication) getApplication()).getTracker();
tracker.track(
    new CustomDimensions()
        .set(1, "foo")
        .set(2, "bar")
);
```

#### Ecommerce

Piwik provides ecommerce analytics that let you measure items added to carts,
and learn detailed metrics about abandoned carts and purchased orders.

To track an Ecommerce order use `trackEcommerceOrder` method.
`orderId` and `grandTotal` (ie. revenue) are required parameters.

```java

Tracker tracker = ((YourApplication) getApplication()).getTracker();
EcommerceItems items = new EcommerceItems();
items.addItem(new EcommerceItems.Item("sku").name("product").category("category").price(200).quantity(2));
items.addItem(new EcommerceItems.Item("sku").name("product2").category("category2").price(400).quantity(3));

TrackHelper.track().order("orderId", 10000).subTotal(7000).tax(2000).shipping(1000).discount(0).items(items).with(tracker);
```

### Advanced tracker usage

#### Custom queries

The base method for any event is
[track](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#track(org.piwik.sdk.TrackMe))
You can create your own objects, set the parameters and send it along.
```java
TrackMe trackMe = new TrackMe()
trackMe.set...
/* ... */
Tracker tracker = ((YourApplication) getApplication()).getTracker();
tracker.track(trackMe);
```

#### Dispatching

The tracker by default will dispatch any pending events every 120 seconds.

If a negative value is used the dispatch timer will never run, a manual dispatch must be used:

```java
        
    Tracker tracker = ((YourApplication) getApplication()).getTracker();
    tracker.setDispatchInterval(-1);
    // Track exception
    try {
        revenue = getRevenue();
    } catch (Exception e) {
        tracker.trackException(e, e.getMessage(), false);
        tracker.dispatch();
        revenue = 0;
    }
    
```

#### User ID

Providing the tracker with a user ID lets you connect data collected from multiple devices and multiple browsers for the same user. 
A user ID is typically a non empty string such as username, email address or UUID that uniquely identifies the user. 
The User ID must be the same for a given user across all her devices and browsers.

```java

        ((YourApplication) getApplication()).getTracker()
                .setUserId("user@email.com");
```

If user ID is used, it must be persisted locally by the app and set directly on the tracker each time the app is started. 

If no user ID is used, the SDK will generate, manage and persist a random id for you.

#### Modifying default parameters

The Tracker has a method
[getDefaultTrackMe](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#getDefaultTrackMe())
modifying the object returned by it will change the default values used on each query.
Note though that the Tracker will not overwrite any values you set on your own TrackMe object.

#### Detailed API documentation

Here is the design document written by Thomas to give a brief overview of the SDK project: https://github.com/piwik/piwik-android-sdk/wiki/Design-document

Piwik SDK should work fine with Android API Version >= 10 (Android 2.3.3+)

Optional [``autoBindActivities``](https://github.com/piwik/piwik-sdk-android/blob/master/piwik-sdk/src/main/java/org/piwik/sdk/QuickTrack.java)
 method is available on API level >= 14.

Check out the full [API documentation](http://piwik.github.io/piwik-sdk-android/).

#### Debugging

Piwik uses [Timber](https://github.com/JakeWharton/timber).
If you don't use Timber in your own app call `Timber.plant(new Timber.DebugTree());`, if you do use Timber in your app then Piwik should automatically participate in your logging efforts.
For more information see [Timbers GitHub](https://github.com/JakeWharton/timber)

### Check SDK

Following command will clean, build, test, generate documentation, do coverage reports and then create a jar.

```
$ ./gradlew :piwik-sdk:clean :piwik-sdk:assemble :piwik-sdk:test :piwik-sdk:jacocoTestReport :piwik-sdk:generateReleaseJavadoc :piwik-sdk:coveralls --info :piwik-sdk:makeJar
```


* Coverage output _./piwik-sdk/build/reports/jacoco/jacocoTestReport/html/index.html_
* Tests report _./piwik-sdk/build/test-report/debug/index.html_
* Javadoc _./piwik-sdk/build/docs/javadoc/index.html_

## Demo application

Browse [the code](https://github.com/piwik/piwik-sdk-android/tree/master/exampleapp) or
build  an .apk by running following command:

```bash
./gradlew :exampleapp:clean :exampleapp:build
```
Generated .apk would be placed in  ``./exampleapp/build/apk/`



## Contribute

* Fork the project
* Create a feature branch based on the 'dev' branch
* Drink coffee and develop an awesome new feature
* Add tests for your new feature
* Make sure that everything still works by running "./gradlew clean assemble test".
* Commit & push the changes to your repo
* Create a pull request from your feature branch against the dev branch of the original repo
* Explain your changes, we can see what changed, but tell us why.
* If your PR passes the travis-ci build and has no merge conflicts, just wait, otherwise fix the code first.

## License

Android SDK for Piwik is released under the BSD-3 Clause license, see [LICENSE](https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE).

