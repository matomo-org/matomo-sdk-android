/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import org.matomo.sdk.dispatcher.DefaultDispatcherFactory;
import org.matomo.sdk.dispatcher.DispatcherFactory;
import org.matomo.sdk.tools.BuildInfo;
import org.matomo.sdk.tools.Checksum;
import org.matomo.sdk.tools.DeviceHelper;
import org.matomo.sdk.tools.PropertySource;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;


public class Matomo {
    public static final String LOGGER_PREFIX = "MATOMO:";
    private static final String TAG = Matomo.tag(Matomo.class);
    private static final String BASE_PREFERENCE_FILE = "org.matomo.sdk";

    @SuppressLint("StaticFieldLeak") private static volatile Matomo sInstance;

    private final Map<Tracker, SharedPreferences> mPreferenceMap = new HashMap<>();
    private final Context mContext;
    private final SharedPreferences mBasePreferences;
    private DispatcherFactory mDispatcherFactory = new DefaultDispatcherFactory();

    public static synchronized Matomo getInstance(Context context) {
        if (sInstance == null) {
            synchronized (Matomo.class) {
                if (sInstance == null) sInstance = new Matomo(context);
            }
        }
        return sInstance;
    }

    private Matomo(Context context) {
        mContext = context.getApplicationContext();
        mBasePreferences = context.getSharedPreferences(BASE_PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Base preferences, tracker idenpendent.
     */
    public SharedPreferences getPreferences() {
        return mBasePreferences;
    }

    /**
     * @return Tracker specific settings object
     */
    public SharedPreferences getTrackerPreferences(@NonNull Tracker tracker) {
        synchronized (mPreferenceMap) {
            SharedPreferences newPrefs = mPreferenceMap.get(tracker);
            if (newPrefs == null) {
                String prefName;
                try {
                    prefName = "org.matomo.sdk_" + Checksum.getMD5Checksum(tracker.getName());
                } catch (Exception e) {
                    Timber.tag(TAG).e(e);
                    prefName = "org.matomo.sdk_" + tracker.getName();
                }
                newPrefs = getContext().getSharedPreferences(prefName, Context.MODE_PRIVATE);
                mPreferenceMap.put(tracker, newPrefs);
            }
            return newPrefs;
        }
    }

    /**
     * If you want to use your own {@link org.matomo.sdk.dispatcher.Dispatcher}
     */
    public void setDispatcherFactory(DispatcherFactory dispatcherFactory) {
        this.mDispatcherFactory = dispatcherFactory;
    }

    public DispatcherFactory getDispatcherFactory() {
        return mDispatcherFactory;
    }

    DeviceHelper getDeviceHelper() {
        return new DeviceHelper(mContext, new PropertySource(), new BuildInfo());
    }

    public static String tag(Class... classes) {
        String[] tags = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            tags[i] = classes[i].getSimpleName();
        }
        return tag(tags);
    }

    public static String tag(String... tags) {
        StringBuilder sb = new StringBuilder(LOGGER_PREFIX);
        for (int i = 0; i < tags.length; i++) {
            sb.append(tags[i]);
            if (i < tags.length - 1) sb.append(":");
        }
        return sb.toString();
    }
}
