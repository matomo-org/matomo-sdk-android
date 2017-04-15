package org.piwik.sdk.dispatcher;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.tools.Connectivity;

public class DispatcherFactory {
    public Dispatcher build(Tracker tracker) {
        return new Dispatcher(
                new EventCache(new EventDiskCache(tracker)),
                new Connectivity(tracker.getPiwik().getContext()),
                new PacketFactory(tracker.getAPIUrl()
                )
        );
    }
}
