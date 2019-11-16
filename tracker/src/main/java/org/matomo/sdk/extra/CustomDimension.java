package org.matomo.sdk.extra;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.matomo.sdk.Matomo;
import org.matomo.sdk.TrackMe;

import timber.log.Timber;

/**
 * Allows you to track Custom Dimensions.
 * In order to use this functionality install and configure
 * https://plugins.matomo.org/CustomDimensions plugin.
 */
public class CustomDimension {
    private static final String TAG = Matomo.tag(CustomDimension.class);
    private final int mId;
    private final String mValue;

    public CustomDimension(int id, String value) {
        mId = id;
        mValue = value;
    }

    public int getId() {
        return mId;
    }

    public String getValue() {
        return mValue;
    }

    /**
     * This method sets a tracking API parameter dimension%dimensionId%=%dimensionValue%.
     * Eg dimension1=foo or dimension2=bar.
     * So the tracking API parameter starts with dimension followed by the set dimensionId.
     * <p>
     * Requires <a href="https://plugins.matomo.org/CustomDimensions">Custom Dimensions</a> plugin (server-side)
     *
     * @param trackMe        into which the data should be inserted
     * @param dimensionId    accepts values greater than 0
     * @param dimensionValue is limited to 255 characters, you can pass null to delete a value
     * @return true if the value was valid
     */
    public static boolean setDimension(@NonNull TrackMe trackMe, int dimensionId, @Nullable String dimensionValue) {
        if (dimensionId < 1) {
            Timber.tag(TAG).e("dimensionId should be great than 0 (arg: %d)", dimensionId);
            return false;
        }
        if (dimensionValue != null && dimensionValue.length() > 255) {
            dimensionValue = dimensionValue.substring(0, 255);
            Timber.tag(TAG).w("dimensionValue was truncated to 255 chars.");
        }
        if (dimensionValue != null && dimensionValue.length() == 0) {
            dimensionValue = null;
        }
        trackMe.set(formatDimensionId(dimensionId), dimensionValue);
        return true;
    }

    public static boolean setDimension(TrackMe trackMe, CustomDimension dimension) {
        return setDimension(trackMe, dimension.getId(), dimension.getValue());
    }

    @Nullable
    public static String getDimension(TrackMe trackMe, int dimensionId) {
        return trackMe.get(formatDimensionId(dimensionId));
    }

    private static String formatDimensionId(int id) {
        return "dimension" + id;
    }
}
