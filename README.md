Piwik SDK for Android
========================

This document describes how to get started using the Piwik Tracking SDK for Android. 
[Piwik](http://piwik.org) is the leading open source web analytics platform 
that gives you valuable insights into your website's visitors, 
your marketing campaigns and much more, so you can optimize your strategy and experience of your visitors.

## Getting started

Integrating Piwik into your Android app
 
0. [Create a new website in the Piwik web interface](http://piwik.org/docs/installation/). Copy the Website ID from "Settings > Websites".
1. [Update AndroidManifest.xml](#update-manifest).
2. Download [latest JAR](https://github.com/piwik/piwik-sdk-android/raw/master/piwik_sdk/jar/PiwikAndroidSdk.jar) and put it into your `lib` folder.
3. [Initialize Tracker](#Initialize-tracker).
4. [Track screen views, exceptions, goals and more](#tracker-usage).
5. [Advanced tracker usage](#advanced-tracker-usage)


### Update Manifest

Update your `AndroidManifest.xml` file by adding the following permissions:

```xml

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

### Initialize Tracker

#### Basic

You can simply extends your application with a 
[``PiwikApplication``](https://github.com/piwik/piwik-sdk-android/blob/master/piwik_sdk/src/main/java/org/piwik/sdk/PiwikApplication.java) class. 
[This approach is used](https://github.com/piwik/piwik-sdk-android/blob/master/demo_app/src/main/java/com/piwik/demo/DemoApp.java) in our demo app.

#### Advanced

Developers could manage the tracker lifecycle by themselves.
To ensure that the metrics are not over-counted, it is highly 
recommended that the tracker be created and managed in the Application class.

```java

import java.net.MalformedURLException;

public class YourApplication extends Application {
    Tracker piwikTracker;

    synchronized Tracker getTracker() {
        if (piwikTracker != null) {
            return piwikTracker;
        }

        try {
            piwikTracker = Piwik.getInstance(this).newTracker("http://your-piwik-domain.tld/piwik.php", 1);
        } catch (MalformedURLException e) {
            Log.w(Tracker.LOGGER_TAG, "url is malformed", e);
            return null;
        }

        return piwikTracker;
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
        ((YourApplication) getApplication()).getTracker()
            .trackScreenView("/your_activity", "Title");
    }
}
```

#### Track goals, [events](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#trackEvent(java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)) and custom vars

```java

((YourApplication) getApplication()).getTracker()
    .setScreenCustomVariable(1, "first", "var")
    .trackEvent("category", "action", "label", 1000)
    .trackGoal(1, revenue);
```

#### Track application downloads

To track the number of app downloads you may call the method [``trackAppDownload``](http://piwik.github.io/piwik-sdk-android/org/piwik/sdk/Tracker.html#trackAppDownload())
This method uses ``SharedPreferences`` to ensures that tracking application downloading will be fired only once.

```java

((YourApplication) getApplication()).getTracker().trackAppDownload();
```

### Advanced tracker usage

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
        tracker.trackException(e.getClass().getName(), "wrong revenue", false);
        tracker.dispatch();
        revenue = 0;
    }
    
```

#### User ID

Providing the tracker with a user ID lets you connect data collected from multiple devices and multiple browsers for the same user. 

A user ID is typically a non empty string such as username, email address or UUID that uniquely identify the user. The User ID must be the same for a given user across all her devices and browsers.

```java

        ((YourApplication) getApplication()).getTracker()
                .setUserId("user@email.com");
```

If user ID is used, it must be persisted locally by the app and set directly on the tracker each time the app is started. 

If no user ID is used, the SDK will generate, manage and persist a random id for you.


#### Detailed API documentation

Here is the design document written by Thomas to give a brief overview of the SDK project: https://github.com/piwik/piwik-android-sdk/wiki/Design-document

Piwik SDK should work fine with Android API Version >= 7 (Android 2.1.x)

Optional [``autoBindActivities``](https://github.com/piwik/piwik-sdk-android/blob/master/piwik_sdk/src/main/java/org/piwik/sdk/Piwik.java#L40)
 method is available on API level >= 14.

Check out the full [API documentation](http://piwik.github.io/piwik-sdk-android/).

## Demo application

Browse [the code](https://github.com/piwik/piwik-sdk-android/tree/master/demo_app) or download [apk](https://github.com/piwik/piwik-sdk-android/raw/master/demo_app/demo_app-debug.apk).

## Build status

[![Build Status](https://travis-ci.org/piwik/piwik-sdk-android.svg?branch=master)](https://travis-ci.org/piwik/piwik-sdk-android)

## License

Android SDK for Piwik is released under the BSD-3 Clause license, see [LICENSE](https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE).

