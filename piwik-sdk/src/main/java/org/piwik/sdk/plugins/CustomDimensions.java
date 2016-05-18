package org.piwik.sdk.plugins;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.TrackMe;

import timber.log.Timber;

/**
 * This plugins allows you to track any Custom Dimensions.
 * In order to use this functionality install and configure
 * https://plugins.piwik.org/CustomDimensions plugin.
 */
public class CustomDimensions extends TrackMe {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "CustomDimensions";
    /**
     * This method sets a tracking API parameter dimension%dimensionId%=%dimensionValue%.
     * Eg dimension1=foo or dimension2=bar.
     * So the tracking API parameter starts with dimension followed by the set dimensionId.
     * @param dimensionId  accepts values greater than 0
     * @param dimensionValue is limited to 255 characters
     * @return instance of CustomDimensions plugin
     */
    public synchronized CustomDimensions set(int dimensionId, String dimensionValue) {
        if (dimensionId < 1){
            Timber.tag(LOGGER_TAG).w("dimensionId should be great than 0");
            return this;
        }
        if (dimensionValue != null && dimensionValue.length() > 255){
            Timber.tag(LOGGER_TAG).w("dimensionValue will be truncated to 255 chars");
            dimensionValue = dimensionValue.substring(0, 255);
        }
        set("dimension" + dimensionId, dimensionValue);
        return this;
    }

}
