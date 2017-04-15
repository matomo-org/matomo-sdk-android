Piwik SDK for Android
========================

[![Build Status](https://travis-ci.org/piwik/piwik-sdk-android.svg?branch=master)](https://travis-ci.org/piwik/piwik-sdk-android) [ ![Download](https://api.bintray.com/packages/darken/maven/piwik-sdk-android/images/download.svg) ](https://bintray.com/darken/maven/piwik-sdk-android/_latestVersion) [![Coverage Status](https://coveralls.io/repos/piwik/piwik-sdk-android/badge.svg?branch=master&service=github)](https://coveralls.io/github/piwik/piwik-sdk-android?branch=master)

Welcome to the Piwik Tracking SDK for Android. [Piwik](http://piwik.org) is the leading open source web analytics platform 
that gives you valuable insights into your website's visitors, your marketing campaigns and much more, so you can optimize your strategy and experience of your visitors.

## Quickstart
For the not so quick start, [see here](https://github.com/piwik/piwik-sdk-android/wiki/Getting-started) or look at our [demo app](https://github.com/piwik/piwik-sdk-android/tree/master/exampleapp)

* [Setup Piwik](https://piwik.org/docs/installation/) on your server.
* Include the library in your app modules `build.gradle` file
```groovy
    compile 'org.piwik.sdk:piwik-sdk:2.0.0'
```

* Ready your `Tracker` by extending our `PiwikApplication` class or do it yourself:
```java
public class YourApplication extends Application {
    private Tracker tracker;
    public synchronized Tracker getTracker() {
        if (tracker == null) tracker = Piwik.getInstance(this).newTracker(new TrackerConfig("http://domain.tld/piwik.php", 1));
        return tracker;
    }
}
```

* Done, your app is ready to use Piwik. Maybe start by tracking a screen view
```java
Tracker tracker = ((PiwikApplication) getApplication()).getTracker();
TrackHelper.track().screen("/activity_main/activity_settings").title("Settings").with(tracker);
```

* or by monitoring your app installs
```java
TrackHelper.track().download().with(tracker);
```

## License
Android SDK for Piwik is released under the BSD-3 Clause license, see [LICENSE](https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE).
