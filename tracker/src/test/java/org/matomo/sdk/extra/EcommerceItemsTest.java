package org.matomo.sdk.extra;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Locale;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class EcommerceItemsTest extends BaseTest {

    @Test
    public void testEmptyItems() {
        EcommerceItems items = new EcommerceItems();
        assertEquals("[]", items.toJson());
    }

    @Test
    public void testAddItems() {
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
    public void testRemoveItem() {
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
    public void testRemoveAllItems() {
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
        assertEquals(200, (int) item.getPrice());
        assertEquals(2, (int) item.getQuantity());
    }

}
