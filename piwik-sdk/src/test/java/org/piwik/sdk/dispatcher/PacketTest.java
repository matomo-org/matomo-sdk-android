package org.piwik.sdk.dispatcher;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class PacketTest extends BaseTest {
    URL mUrl;

    @Test
    public void testEventCount() {
        Packet testPacket = new Packet(mUrl, null, 55);
        assertEquals(55, testPacket.getEventCount());
    }

    @Test
    public void testTimeStamp() {
        Packet testPacket = new Packet(mUrl);
        long timeStamp = System.currentTimeMillis();
        assertTrue(timeStamp - testPacket.getTimeStamp() < 5);
    }

}
