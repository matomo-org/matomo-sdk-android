/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;


public interface Dispatchable<T> {

    public long getDispatchIntervalMillis();

    public boolean dispatch();

    public void dispatchingStarted();

    public void dispatchingCompleted(T result);

    public boolean isDispatching();
}
