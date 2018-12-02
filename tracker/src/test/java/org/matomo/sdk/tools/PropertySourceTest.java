package org.matomo.sdk.tools;

import org.junit.Test;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


public class PropertySourceTest extends BaseTest {
    @Test
    public void testGetHttpAgent() {
        PropertySource propertySource = spy(new PropertySource());
        propertySource.getHttpAgent();
        verify(propertySource).getSystemProperty("http.agent");

        assertEquals(propertySource.getHttpAgent(), propertySource.getSystemProperty("http.agent"));
    }

    @Test
    public void testGetJVMVersion() {
        PropertySource propertySource = spy(new PropertySource());
        propertySource.getJVMVersion();
        verify(propertySource).getSystemProperty("java.vm.version");

        assertEquals(propertySource.getJVMVersion(), propertySource.getSystemProperty("java.vm.version"));
    }
}
