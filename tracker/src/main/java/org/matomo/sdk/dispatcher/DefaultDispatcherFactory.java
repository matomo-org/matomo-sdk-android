package org.matomo.sdk.dispatcher;

import org.matomo.sdk.Tracker;
import org.matomo.sdk.tools.Connectivity;

public class DefaultDispatcherFactory implements DispatcherFactory {
    public Dispatcher build(Tracker tracker) {
        PacketSender packetSender;
        if (tracker.getOkHttpClient()!= null) {
            packetSender = new DefaultPacketSender();
        } else {
            packetSender = new OkHttpPacketSender(tracker.getOkHttpClient());
        }

        return new DefaultDispatcher(
                new EventCache(new EventDiskCache(tracker)),
                new Connectivity(tracker.getMatomo().getContext()),
                new PacketFactory(tracker.getAPIUrl()),
                packetSender
        );
    }
}
