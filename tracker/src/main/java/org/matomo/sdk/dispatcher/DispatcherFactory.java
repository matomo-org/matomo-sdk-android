package org.matomo.sdk.dispatcher;

import org.matomo.sdk.Tracker;

public interface DispatcherFactory {
    Dispatcher build(Tracker tracker);
}
