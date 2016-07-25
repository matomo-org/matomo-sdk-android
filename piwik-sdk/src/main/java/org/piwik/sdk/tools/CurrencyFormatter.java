/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.tools;

import android.support.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    @Nullable
    public static String priceString(@Nullable Integer cents) {
        if (cents == null) return null;
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(2);
        return numberFormat.format(cents / 100.);
    }
}
