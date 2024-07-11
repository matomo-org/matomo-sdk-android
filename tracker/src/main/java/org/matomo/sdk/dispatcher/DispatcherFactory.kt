package org.matomo.sdk.dispatcher

import org.matomo.sdk.Tracker

interface DispatcherFactory {
    fun build(tracker: Tracker): Dispatcher
}

