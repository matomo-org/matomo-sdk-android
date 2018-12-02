/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.tools;

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
