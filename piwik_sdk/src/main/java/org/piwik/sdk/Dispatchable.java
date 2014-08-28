package org.piwik.sdk;


public interface Dispatchable<T> {

    public long getDispatchIntervalMillis();

    public boolean dispatch();

    public void dispatchingStarted();

    public void dispatchingCompleted(T result);

    public boolean isDispatching();
}
