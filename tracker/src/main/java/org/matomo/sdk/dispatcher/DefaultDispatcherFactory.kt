package org.matomo.sdk.dispatcher

import org.matomo.sdk.Tracker
import org.matomo.sdk.tools.Connectivity

open class DefaultDispatcherFactory : DispatcherFactory {
    override fun build(tracker: Tracker): Dispatcher {
        return DefaultDispatcher(
            EventCache(EventDiskCache(tracker)),
            Connectivity(tracker.matomo.context),
            PacketFactory(tracker.apiUrl),
            DefaultPacketSender(), {}
        )
    }
}
