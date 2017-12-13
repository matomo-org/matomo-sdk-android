package org.piwik.sdk.dispatcher;

import org.piwik.sdk.Tracker;

public interface DispatcherFactory {
    Dispatcher build(Tracker tracker);
}
