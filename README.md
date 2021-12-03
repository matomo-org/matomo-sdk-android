Matomo SDK for Android
========================

[![](https://jitpack.io/v/matomo-org/matomo-sdk-android.svg)](https://jitpack.io/#matomo-org/matomo-sdk-android)
![Build](https://github.com/matomo-org/matomo-sdk-android/actions/workflows/pull-request-ci.yml/badge.svg)
[![Codecov](https://codecov.io/gh/matomo-org/matomo-sdk-android/branch/master/graph/badge.svg)](https://codecov.io/gh/matomo-org/matomo-sdk-android?branch=master)

Welcome to the [Matomo](http://matomo.org) Tracking SDK for Android. This library helps you send analytics data from Android apps to Matomo instances. Until v4 this library was known as **Piwik** Tracking SDK for Android.

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
  via [JitPack](https://jitpack.io/#matomo-org/matomo-sdk-android)

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}
dependencies {
  implementation 'com.github.matomo-org:matomo-sdk-android:<latest-version>'
}
```

* Now you need to initialize your `Tracker`. It's recommended to store it as singleton. You can extend `MatomoApplication` or create and store a `Tracker` instance yourself:
```java
import org.matomo.sdk.TrackerBuilder;

public class YourApplication extends Application {
    private Tracker tracker;
    public synchronized Tracker getTracker() {
        if (tracker == null){
            tracker = TrackerBuilder.createDefault("http://domain.tld/matomo.php", 1).build(Matomo.getInstance(this));
        }
        return tracker;
    }
}
```

* The `TrackHelper` class is the easiest way to submit events to your tracker:
```java
// The `Tracker` instance from the previous step
Tracker tracker = ((MatomoApplication) getApplication()).getTracker();
// Track a screen view
TrackHelper.track().screen("/activity_main/activity_settings").title("Settings").with(tracker);
// Monitor your app installs
TrackHelper.track().download().with(tracker);
```

* Something not working? Check [here](https://github.com/matomo-org/matomo-sdk-android/wiki/Troubleshooting).

## License
Android SDK for Matomo is released under the BSD-3 Clause license, see [LICENSE](https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE).
