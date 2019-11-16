/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.tools;

import androidx.annotation.Nullable;

import java.math.BigDecimal;

public class CurrencyFormatter {
    @Nullable
    public static String priceString(@Nullable Integer cents) {
        if (cents == null) return null;
        return new BigDecimal(cents).movePointLeft(2).toPlainString();
    }
}
