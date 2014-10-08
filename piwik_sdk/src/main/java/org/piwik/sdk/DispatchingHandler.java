/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.os.Handler;
import android.os.Message;


public class DispatchingHandler extends Handler {
    private Dispatchable<Integer> dispatchable;
    private static final int DEFAULT_MIN_RETRY_TIMEOUT = 1000;
    private static final int START_LOOP = 0;
    private static final int STOP_LOOP = 1;
    private boolean started = false;

    public DispatchingHandler(Dispatchable<Integer> dispatchable) {
        this.dispatchable = dispatchable;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case START_LOOP:
                long interval = dispatchable.getDispatchIntervalMillis();
                if (interval < DEFAULT_MIN_RETRY_TIMEOUT) {
                    break;
                }
                started = true;

                // process only when dispatchable is not busy
                if (!dispatchable.isDispatching()) {
                    dispatchable.dispatch();
                }

                // make a recursion
                sendEmptyMessageDelayed(START_LOOP, interval);

                break;

            case STOP_LOOP:
                // remove pending task
                removeMessages(START_LOOP);
                break;

            default:
                removeMessages(START_LOOP);
                removeMessages(STOP_LOOP);
                break;
        }
    }

    public void start() {
        if (!isStarted()) {
            sendEmptyMessage(START_LOOP);
        }
    }

    public void stop() {
        if (isStarted()) {
            started = false;
            sendEmptyMessage(STOP_LOOP);
        }
    }

    public boolean isStarted() {
        return started;
    }
}
