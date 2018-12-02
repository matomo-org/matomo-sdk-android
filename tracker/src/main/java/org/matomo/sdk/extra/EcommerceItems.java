/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.extra;

import org.json.JSONArray;
import org.matomo.sdk.tools.CurrencyFormatter;

import java.util.HashMap;
import java.util.Map;

public class EcommerceItems {
    private final Map<String, JSONArray> mItems = new HashMap<>();

    /**
     * Adds a product into the ecommerce order. Must be called for each product in the order.
     * If the same sku is used twice, the first item is overwritten.
     */
    public void addItem(Item item) {
        mItems.put(item.mSku, item.toJson());
    }

    public static class Item {
        private final String mSku;
        private String mCategory;
        private Integer mPrice;
        private Integer mQuantity;
        private String mName;

        /**
         * If the same sku is used twice, the first item is overwritten.
         *
         * @param sku Unique identifier for the product
         */
        public Item(String sku) {
            mSku = sku;
        }

        /**
         * @param name Product name
         */
        public Item name(String name) {
            mName = name;
            return this;
        }

        /**
         * @param category Product category
         */
        public Item category(String category) {
            mCategory = category;
            return this;
        }

        /**
         * @param price Price of the product in cents
         */
        public Item price(int price) {
            mPrice = price;
            return this;
        }

        /**
         * @param quantity Quantity
         */
        public Item quantity(int quantity) {
            mQuantity = quantity;
            return this;
        }

        public String getSku() {
            return mSku;
        }

        public String getCategory() {
            return mCategory;
        }

        public Integer getPrice() {
            return mPrice;
        }

        public Integer getQuantity() {
            return mQuantity;
        }

        public String getName() {
            return mName;
        }

        protected JSONArray toJson() {
            JSONArray item = new JSONArray();
            item.put(mSku);
            if (mName != null) item.put(mName);
            if (mCategory != null) item.put(mCategory);
            if (mPrice != null) item.put(CurrencyFormatter.priceString(mPrice));
            if (mQuantity != null) item.put(String.valueOf(mQuantity));
            return item;
        }
    }

    /**
     * Remove a product from an ecommerce order.
     *
     * @param sku unique identifier for the product
     */
    public void remove(String sku) {
        mItems.remove(sku);
    }

    public void remove(Item item) {
        mItems.remove(item.mSku);
    }

    /**
     * Clears all items from the ecommerce order
     */
    public void clear() {
        mItems.clear();
    }

    public String toJson() {
        JSONArray jsonItems = new JSONArray();

        for (JSONArray item : mItems.values()) {
            jsonItems.put(item);
        }
        return jsonItems.toString();
    }
}
