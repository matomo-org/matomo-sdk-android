package org.piwik.sdk.dispatcher;


import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PacketTest {
    URL mUrl;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
    }

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
