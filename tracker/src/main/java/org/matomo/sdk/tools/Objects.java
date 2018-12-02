package org.matomo.sdk.tools;


public class Objects {
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
