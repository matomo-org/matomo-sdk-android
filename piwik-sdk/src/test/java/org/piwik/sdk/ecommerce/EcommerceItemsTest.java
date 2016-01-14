package org.piwik.sdk.ecommerce;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.FullEnvTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
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
        items.addItem("fake_sku", "fake_product", "fake_category", 200, 2);
        items.addItem("fake_sku_2", "fake_product_2", "fake_category_2", 400, 3);

        String itemsJson = items.toJson();
        assertTrue(itemsJson.contains("[\"fake_sku\",\"fake_product\",\"fake_category\",\"2.00\",\"2\"]"));
        assertTrue(itemsJson.contains("[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]"));
    }

    @Test
    public void testRemoveItem() throws Exception {
        Locale.setDefault(Locale.US);
        EcommerceItems items = new EcommerceItems();
        items.addItem("fake_sku", "fake_product", "fake_category", 200, 2);
        items.addItem("fake_sku_2", "fake_product_2", "fake_category_2", 400, 3);
        items.removeItem("fake_sku");

        assertEquals("[[\"fake_sku_2\",\"fake_product_2\",\"fake_category_2\",\"4.00\",\"3\"]]", items.toJson());
    }

    @Test
    public void testRemoveAllItems() throws Exception {
        EcommerceItems items = new EcommerceItems();
        items.addItem("fake_sku", "fake_product", "fake_category", 200, 2);
        items.addItem("fake_sku_2", "fake_product_2", "fake_category_2", 400, 3);
        items.removeAll();

        assertEquals("[]", items.toJson());
    }

}
