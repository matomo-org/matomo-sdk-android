package org.matomo.sdk.tools;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static org.matomo.sdk.tools.Connectivity.Type.MOBILE;
import static org.matomo.sdk.tools.Connectivity.Type.NONE;
import static org.matomo.sdk.tools.Connectivity.Type.WIFI;

public class Connectivity {
    private final ConnectivityManager mConnectivityManager;

    public Connectivity(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isConnected() {
        NetworkInfo network = mConnectivityManager.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }

    public enum Type {
        NONE, MOBILE, WIFI
    }

    public Type getType() {
        NetworkInfo network = mConnectivityManager.getActiveNetworkInfo();
        if (network == null) return NONE;
        if (network.getType() == ConnectivityManager.TYPE_WIFI) {
            return WIFI;
        } else return MOBILE;
    }
}
