package org.matomo.sdk.tools

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import testhelpers.BaseTest

class PropertySourceTest : BaseTest() {
    @Test
    fun testGetHttpAgent() {
        val propertySource = Mockito.spy(PropertySource())
        propertySource.httpAgent
        Mockito.verify(propertySource).getSystemProperty("http.agent")

        Assert.assertEquals(propertySource.httpAgent, propertySource.getSystemProperty("http.agent"))
    }

    @Test
    fun testGetJVMVersion() {
        val propertySource = Mockito.spy(PropertySource())
        propertySource.jvmVersion
        Mockito.verify(propertySource).getSystemProperty("java.vm.version")

        Assert.assertEquals(propertySource.jvmVersion, propertySource.getSystemProperty("java.vm.version"))
    }
}
