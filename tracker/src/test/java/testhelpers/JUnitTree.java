package testhelpers;

import androidx.annotation.NonNull;
import android.util.Log;

import timber.log.Timber;


public class JUnitTree extends Timber.DebugTree {
    private final int minlogLevel;

    public JUnitTree() {
        minlogLevel = Log.VERBOSE;
    }

    private static String priorityToString(int priority) {
        switch (priority) {
            case Log.ERROR:
                return "E";
            case Log.WARN:
                return "W";
            case Log.INFO:
                return "I";
            case Log.DEBUG:
                return "D";
            case Log.VERBOSE:
                return "V";
            default:
                return String.valueOf(priority);
        }
    }

    @Override
    protected void log(int priority, String tag, @NonNull String message, Throwable t) {
        if (priority < minlogLevel) return;
        System.out.println(System.currentTimeMillis() + " " + priorityToString(priority) + "/" + tag + ": " + message);
    }
}
