Matomo SDK for Android
========================

[![Build Status](https://travis-ci.org/matomo-org/matomo-sdk-android.svg?branch=master)](https://travis-ci.org/matomo-org/matomo-sdk-android) [ ![Download](https://api.bintray.com/packages/darken/maven/matomo-sdk-android/images/download.svg) ](https://bintray.com/darken/maven/matomo-sdk-android/_latestVersion) [![Coverage Status](https://coveralls.io/repos/matomo/matomo-sdk-android/badge.svg?branch=master&service=github)](https://coveralls.io/github/matomo/matomo-sdk-android?branch=master)

Welcome to the [Matomo](http://matomo.org) Tracking SDK for Android. This library helps you send analytics data from Android apps to Matomo instances.

__Features__:
* Caching and offline support
* Graceful reconnection handling
* WIFI-only mode
* Thread-safe support for multiple trackers
* Support for custom connection implementations
* Complete [Matomo HTTP API](https://developer.matomo.org/api-reference/tracking-api) support
    * [Custom dimensions](https://matomo.org/docs/custom-dimensions/)
    * [Event Tracking](https://matomo.org/docs/event-tracking/)
    * [Content Tracking](https://matomo.org/docs/content-tracking/)
    * [Ecommerce](https://matomo.org/docs/ecommerce-analytics/)
* Checksum based app install/upgrade tracking

## Quickstart
For the not so quick start, [see here](https://github.com/matomo-org/matomo-sdk-android/wiki/Getting-started) or look at our [demo app](https://github.com/matomo-org/matomo-sdk-android/tree/master/exampleapp)

* [Setup Matomo](https://matomo.org/docs/installation/) on your server.
* Include the library in your app modules `build.gradle` file
```groovy
    implementation 'org.matomo.sdk:tracker:<latest-version>'
```

* Initialize your `Tracker` either by extending our `MatomoApplication` class or storing an instance yourself:
```java
public class YourApplication extends Application {
    private Tracker tracker;
    public synchronized Tracker getTracker() {
        if (tracker == null) tracker = Matomo.getInstance(this).newTracker(new TrackerConfig("http://domain.tld/matomo.php", 1));
        return tracker;
    }
}
```

* The `TrackHelper` class is the easiest way to submit events to your tracker:
```java
// Get the `Tracker` you want to use
Tracker tracker = ((MatomoApplication) getApplication()).getTracker();
// Track a screen view
TrackHelper.track().screen("/activity_main/activity_settings").title("Settings").with(tracker);
// Monitor your app installs
TrackHelper.track().download().with(tracker);
```

## License
Android SDK for Matomo is released under the BSD-3 Clause license, see [LICENSE](https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE).
