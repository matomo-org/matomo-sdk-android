package org.matomo.sdk.tools;

import android.app.Activity;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;


public class ActivityHelper {

    public static String getBreadcrumbs(final Activity activity) {
        Activity currentActivity = activity;
        ArrayList<String> breadcrumbs = new ArrayList<>();

        while (currentActivity != null) {
            breadcrumbs.add(currentActivity.getTitle().toString());
            currentActivity = currentActivity.getParent();
        }
        return joinSlash(breadcrumbs);
    }

    public static String joinSlash(List<String> sequence) {
        if (sequence != null && sequence.size() > 0) {
            return TextUtils.join("/", sequence);
        }
        return "";
    }

    public static String breadcrumbsToPath(String breadcrumbs) {
        return breadcrumbs.replaceAll("\\s", "");
    }
}
