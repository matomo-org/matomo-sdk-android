Piwik SDK for Android
========================

[![Build Status](https://travis-ci.org/matomo-org/piwik-sdk-android.svg?branch=master)](https://travis-ci.org/matomo-org/piwik-sdk-android) [ ![Download](https://api.bintray.com/packages/darken/maven/piwik-sdk-android/images/download.svg) ](https://bintray.com/darken/maven/piwik-sdk-android/_latestVersion) [![Coverage Status](https://coveralls.io/repos/piwik/piwik-sdk-android/badge.svg?branch=master&service=github)](https://coveralls.io/github/piwik/piwik-sdk-android?branch=master)

Welcome to the [Piwik](http://piwik.org) Tracking SDK for Android. This library helps you send analytics data from Android apps to Piwik instances.

__Features__:
* Caching and offline support
* Graceful reconnection handling
* WIFI-only mode
* Thread-safe support for multiple trackers
* Support for custom connection implementations
* Complete [Piwik HTTP API](https://developer.piwik.org/api-reference/tracking-api) support
    * [Custom dimensions](https://piwik.org/docs/custom-dimensions/)
    * [Event Tracking](https://piwik.org/docs/event-tracking/)
    * [Content Tracking](https://piwik.org/docs/content-tracking/)
    * [Ecommerce](https://piwik.org/docs/ecommerce-analytics/)
* Checksum based app install/upgrade tracking

## Quickstart
For the not so quick start, [see here](https://github.com/piwik/piwik-sdk-android/wiki/Getting-started) or look at our [demo app](https://github.com/piwik/piwik-sdk-android/tree/master/exampleapp)

* [Setup Piwik](https://piwik.org/docs/installation/) on your server.
* Include the library in your app modules `build.gradle` file
```groovy
    compile 'org.piwik.sdk:piwik-sdk:3.0.1'
```

* Initialize your `Tracker` either by extending our `PiwikApplication` class or storing an instance yourself:
```java
public class YourApplication extends Application {
    private Tracker tracker;
    public synchronized Tracker getTracker() {
        if (tracker == null) tracker = Piwik.getInstance(this).newTracker(new TrackerConfig("http://domain.tld/piwik.php", 1));
        return tracker;
    }
}
```

* The `TrackHelper` class is the easiest way to submit events to your tracker:
```java
// Get the `Tracker` you want to use
Tracker tracker = ((PiwikApplication) getApplication()).getTracker();
// Track a screen view
TrackHelper.track().screen("/activity_main/activity_settings").title("Settings").with(tracker);
// Monitor your app installs
TrackHelper.track().download().with(tracker);
```

## License
Android SDK for Piwik is released under the BSD-3 Clause license, see [LICENSE](https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE).
