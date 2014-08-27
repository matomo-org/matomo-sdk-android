package org.piwik.sdk;


public interface Dispatchable<T> {
    public boolean dispatch();

    public void startDispatching();

    public void dispatchingCompleted(T result);

    public boolean isDispatching();
}
