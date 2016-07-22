package org.piwik.sdk.tools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class CurrencyFormatterTest {

    @Test
    public void testCurrencyFormat() throws Exception {
        String currency = CurrencyFormatter.priceString(1000);
        assertEquals("10.00", currency);

        currency = CurrencyFormatter.priceString(3950);
        assertEquals("39.50", currency);

        currency = CurrencyFormatter.priceString(1);
        assertEquals("0.01", currency);

        currency = CurrencyFormatter.priceString(25034);
        assertEquals("250.34", currency);
    }
}
