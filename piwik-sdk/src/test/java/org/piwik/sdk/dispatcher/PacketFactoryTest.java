package org.piwik.sdk.dispatcher;

import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PacketFactoryTest {

    @Test
    public void testPOST_apiUrl() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url);
        List<Packet> packets = factory.buildPackets(Arrays.asList(new Event("straw"), new Event("berries")));
        for (Packet p : packets) {
            assertEquals(url, p.getTargetURL());
        }
    }

    @Test
    public void testPOST_data() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url);
        List<Packet> packets = factory.buildPackets(Arrays.asList(new Event("straw"), new Event("berries")));
        assertEquals("straw", packets.get(0).getPostData().getJSONArray("requests").get(0));
        assertEquals("berries", packets.get(0).getPostData().getJSONArray("requests").get(1));
    }

    @Test
    public void testGET_apiUrl() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url);
        List<Packet> packets = factory.buildPackets(Collections.singletonList(new Event("strawberries")));
        assertTrue(packets.get(0).getTargetURL().toExternalForm().startsWith(url.toExternalForm()));
    }

    @Test
    public void testGET_badUrl() throws Exception {
        PacketFactory factory = new PacketFactory(new URL("http://example.com/"));
        assertTrue(factory.buildPackets(Collections.singletonList(new Event(""))).isEmpty());
    }

    @Test
    public void testEmptyEvents() throws Exception {
        PacketFactory factory = new PacketFactory(new URL("http://example.com/"));
        assertTrue(factory.buildPackets(Collections.<Event>emptyList()).isEmpty());
    }

    @Test
    public void testPacking_rest() throws Exception {
        List<Event> events = new LinkedList<>();
        for (int i = 1; i <= PacketFactory.PAGE_SIZE + 1; i++) {
            events.add(new Event("?eve" + i));
        }
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url);
        List<Packet> packets = factory.buildPackets(events);
        Packet first = packets.get(0);
        assertEquals(PacketFactory.PAGE_SIZE, first.getEventCount());
        assertNotNull(first.getPostData());

        Packet second = packets.get(1);
        assertEquals(1, second.getEventCount());
        assertNull(second.getPostData());
        assertTrue(second.getTargetURL().toExternalForm().endsWith("?eve" + events.size()));
    }

    @Test
    public void testPacking_notfull() throws Exception {
        List<Event> events = new LinkedList<>();
        for (int i = 0; i < PacketFactory.PAGE_SIZE * 2 - 2; i++) {
            events.add(new Event("?eve" + i));
        }
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url);
        List<Packet> packets = factory.buildPackets(events);
        Packet first = packets.get(0);
        assertEquals(PacketFactory.PAGE_SIZE, first.getEventCount());
        assertNotNull(first.getPostData());
        assertTrue(first.getPostData().getJSONArray("requests").getString(0).endsWith("?eve0"));

        Packet second = packets.get(1);
        assertEquals(PacketFactory.PAGE_SIZE - 2, second.getEventCount());
        assertNotNull(second.getPostData());
    }

    @Test
    public void testPacking_even() throws Exception {
        List<Event> events = new LinkedList<>();
        for (int i = 0; i < PacketFactory.PAGE_SIZE * 3; i++) {
            events.add(new Event("?eve" + i));
        }
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url);
        List<Packet> packets = factory.buildPackets(events);
        Packet first = packets.get(0);
        assertEquals(PacketFactory.PAGE_SIZE, first.getEventCount());
        assertNotNull(first.getPostData());

        Packet second = packets.get(1);
        assertEquals(PacketFactory.PAGE_SIZE, second.getEventCount());
        assertNotNull(second.getPostData());

        Packet third = packets.get(2);
        assertEquals(PacketFactory.PAGE_SIZE, third.getEventCount());
        assertNotNull(third.getPostData());
    }

}