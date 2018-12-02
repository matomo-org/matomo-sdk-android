package org.matomo.sdk.dispatcher;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class PacketTest extends BaseTest {

    @Test
    public void testEventCount() {
        Packet testPacket = new Packet("", null, 55);
        assertEquals(55, testPacket.getEventCount());
    }

    @Test
    public void testTimeStamp() {
        Packet testPacket = new Packet("");
        long timeStamp = System.currentTimeMillis();
        assertTrue(timeStamp - testPacket.getTimeStamp() < 5);
    }

}
