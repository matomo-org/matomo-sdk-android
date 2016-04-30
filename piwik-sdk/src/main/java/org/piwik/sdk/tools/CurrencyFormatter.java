/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.tools;

import android.support.annotation.Nullable;

import java.text.DecimalFormat;

public class CurrencyFormatter {
    @Nullable
    public static String priceString(@Nullable Integer cents) {
        if (cents == null) return null;
        DecimalFormat form = new DecimalFormat("0.00");
        return form.format(cents / 100.);
    }
}
