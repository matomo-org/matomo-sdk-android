package org.matomo.sdk.tools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CurrencyFormatterTest extends BaseTest {

    @Test
    public void testCurrencyFormat() {
        assertEquals("10.00", CurrencyFormatter.priceString(1000));

        assertEquals("39.50", CurrencyFormatter.priceString(3950));

        assertEquals("0.01", CurrencyFormatter.priceString(1));

        assertEquals("250.34", CurrencyFormatter.priceString(25034));

        assertEquals("1747.20", CurrencyFormatter.priceString(174720));

        assertEquals("1234567.89", CurrencyFormatter.priceString(123456789));
    }
}
