/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.ecommerce;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.piwik.sdk.tools.CurrencyFormatter;

import java.util.HashMap;
import java.util.Map;

public class EcommerceItems {
    public Map<String, JSONArray> items = new HashMap<>();


    /**
     * Adds a product into the ecommerce order. Must be called for each product in the order.
     * If the same sku is used twice, the first item is overwritten.
     *
     * @param sku      (required) Unique identifier for the product
     * @param name     (optional) Product name
     * @param category (optional) Product category
     * @param price    (optional) Price of the product in cents
     * @param quantity (optional) Quantity
     */
    public void addItem(String sku, @Nullable String name, @Nullable String category, @Nullable Integer price, @Nullable Integer quantity) {
        if (name == null) {
            name = "";
        }
        if (category == null) {
            category = "";
        }
        if (price == null) {
            price = 0;
        }
        if (quantity == null) {
            quantity = 1;
        }

        JSONArray item = new JSONArray();
        item.put(sku);
        item.put(name);
        item.put(category);
        item.put(CurrencyFormatter.priceString(price));
        item.put(quantity.toString());
        items.put(sku, item);
    }

    /**
     * Remove a product from an ecommerce order.
     *
     * @param sku unique identifier for the product
     */
    public void removeItem(String sku) {
        items.remove(sku);
    }

    /**
     * Clears all items from the ecommerce order
     */
    public void removeAll() {
        items.clear();
    }

    public String toJson() {
        JSONArray jsonItems = new JSONArray();

        for (JSONArray item : items.values()) {
            jsonItems.put(item);
        }
        return jsonItems.toString();
    }
}
