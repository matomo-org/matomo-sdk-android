package org.piwik.sdk.tools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CurrencyFormatterTest extends BaseTest {

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
