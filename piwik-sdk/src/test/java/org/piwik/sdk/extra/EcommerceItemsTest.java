package org.piwik.sdk.extra;

import org.json.JSONArray;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EcommerceItemsTest {

    @Test
    public void testEmptyItems() throws Exception {
        EcommerceItems items = new EcommerceItems();
        assertEquals("[]", items.toJson());
    }

    @Test
    public void testAddItems() throws Exception {
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        items.addItem(new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3));
        items.addItem(new EcommerceItems.Item("fake_sku_3"));

        String itemsJson = items.toJson();
        assertTrue(itemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(itemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
        assertTrue(itemsJson.contains("[\"fake_sku_3\"]"));
    }

    @Test
    public void testRemoveItem() throws Exception {
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        final EcommerceItems.Item item2 = new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3);
        items.addItem(item2);
        items.remove("fake_sku");

        assertEquals("[[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]]", items.toJson());

        items.remove(item2);
        assertEquals(new JSONArray().toString(), items.toJson());
    }

    @Test
    public void testRemoveAllItems() throws Exception {
        EcommerceItems items = new EcommerceItems();
        items.addItem(new EcommerceItems.Item("fake_sku").name("fake_product").category("fake_category").price(200).quantity(2));
        items.addItem(new EcommerceItems.Item("fake_sku_2").name("fake_product_2").category("fake_category_2").price(400).quantity(3));
        items.clear();

        assertEquals("[]", items.toJson());
    }

    @Test
    public void testItem() {
        EcommerceItems.Item item = new EcommerceItems
                .Item("fake_sku")
                .name("fake_product")
                .category("fake_category")
                .price(200)
                .quantity(2);
        assertEquals("fake_sku", item.getSku());
        assertEquals("fake_product", item.getName());
        assertEquals("fake_category", item.getCategory());
        assertTrue(200 == item.getPrice());
        assertTrue(2 == item.getQuantity());
    }

}
