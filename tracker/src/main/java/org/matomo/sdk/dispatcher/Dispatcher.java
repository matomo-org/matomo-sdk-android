/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.dispatcher;

import org.matomo.sdk.TrackMe;

import java.util.List;

/**
 * Responsible for transmitting packets to a server
 */
public interface Dispatcher {
    int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;  // 5s
    long DEFAULT_DISPATCH_INTERVAL = 120 * 1000; // 120s

    /**
     * Connection timeout in milliseconds
     *
     * @return timeout in milliseconds
     */
    int getConnectionTimeOut();

    /**
     * Timeout when trying to establish connection and when trying to read a response.
     * Values take effect on next dispatch.
     *
     * @param timeOut timeout in milliseconds
     */
    void setConnectionTimeOut(int timeOut);

    /**
     * Packets are collected and dispatched in batches, this intervals sets the pause between batches.
     *
     * @param dispatchInterval in milliseconds
     */
    void setDispatchInterval(long dispatchInterval);

    long getDispatchInterval();

    /**
     * Packets are collected and dispatched in batches. This boolean sets if post must be
     * gzipped or not. Use of gzip needs mod_deflate/Apache ou lua_zlib/NGINX
     *
     * @param dispatchGzipped boolean
     */
    void setDispatchGzipped(boolean dispatchGzipped);

    boolean getDispatchGzipped();

    void setDispatchMode(DispatchMode dispatchMode);

    DispatchMode getDispatchMode();

    /**
     * Starts the dispatcher for one cycle if it is currently not working.
     * If the dispatcher is working it will skip the dispatch interval once.
     */
    boolean forceDispatch();

    /**
     * Dispatch all events in the EventCache and return only after the dispatch is complete.
     *
     * This method may be invoked while the Runtime is being torn down and should not start new threads.
     */
    void forceDispatchBlocking();

    /**
     * To clear the dispatchers queue
     */
    void clear();

    /**
     * Submit for transmission
     */
    void submit(TrackMe trackMe);

    /**
     * For debugging purposes
     * When this is non null then instead of sending data over the network it will be written into this list.
     * Mind thread-safety!
     */
    void setDryRunTarget(List<Packet> dryRunTarget);

    /**
     * For debugging purposes
     * Mind thread-safety!
     */
    List<Packet> getDryRunTarget();
}
