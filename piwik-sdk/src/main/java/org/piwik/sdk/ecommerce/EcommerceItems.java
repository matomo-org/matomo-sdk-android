/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.ecommerce;

import org.piwik.sdk.tools.CurrencyFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcommerceItems {
    public Map<String, List<String>> items = new HashMap<>();

    public void addItem(String sku, String name, String category, int price, int quantity) {
        List<String> item = Arrays.asList(sku, name, category, CurrencyFormatter.priceString(price), "" + quantity);
        items.put(sku, item);
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for( List<String> item : items.values() ) {
            builder.append("[");
            for (String attribute : item) {
                builder.append("\"").append(attribute).append("\",");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("],");
        }
        builder.deleteCharAt(builder.length()-1);
        builder.append("]");

        return builder.toString();
    }

    public void removeItem(String sku) {
        items.remove(sku);
    }

    public void removeAll() {
        items.clear();
    }
}
