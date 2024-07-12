package org.matomo.sdk.tools

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import testhelpers.BaseTest

@RunWith(MockitoJUnitRunner::class)
class CurrencyFormatterTest : BaseTest() {
    @Test
    fun testCurrencyFormat() {
        Assert.assertEquals("10.00", CurrencyFormatter.priceString(1000))
        Assert.assertEquals("39.50", CurrencyFormatter.priceString(3950))
        Assert.assertEquals("0.01", CurrencyFormatter.priceString(1))
        Assert.assertEquals("250.34", CurrencyFormatter.priceString(25034))
        Assert.assertEquals("1747.20", CurrencyFormatter.priceString(174720))
        Assert.assertEquals("1234567.89", CurrencyFormatter.priceString(123456789))
    }
}
