package org.piwik.sdk.dispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.QueryParams;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class PacketFactoryTest {

    @Test
    public void testPOST_authtoken() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, "token");
        List<Packet> packets = factory.buildPackets(Arrays.asList("straw", "berries"));
        for (Packet p : packets) {
            assertEquals("token", p.getPostData().getString(QueryParams.AUTHENTICATION_TOKEN.toString()));
        }
    }

    @Test
    public void testPOST_apiUrl() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, null);
        List<Packet> packets = factory.buildPackets(Arrays.asList("straw", "berries"));
        for (Packet p : packets) {
            assertEquals(url, p.getTargetURL());
        }
    }

    @Test
    public void testPOST_data() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, null);
        List<Packet> packets = factory.buildPackets(Arrays.asList("straw", "berries"));
        assertEquals("straw", packets.get(0).getPostData().getJSONArray("requests").get(0));
        assertEquals("berries", packets.get(0).getPostData().getJSONArray("requests").get(1));
    }

    @Test
    public void testGET_apiUrl() throws Exception {
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, null);
        List<Packet> packets = factory.buildPackets(Collections.singletonList("strawberries"));
        assertTrue(packets.get(0).getTargetURL().toExternalForm().startsWith(url.toExternalForm()));
    }

    @Test
    public void testGET_badUrl() throws Exception {
        PacketFactory factory = new PacketFactory(new URL("http://example.com/"), null);
        assertTrue(factory.buildPackets(Collections.singletonList("")).isEmpty());
    }

    @Test
    public void testEmptyEvents() throws Exception {
        PacketFactory factory = new PacketFactory(new URL("http://example.com/"), null);
        assertTrue(factory.buildPackets(Collections.<String>emptyList()).isEmpty());
    }

    @Test
    public void testPacking_rest() throws Exception {
        List<String> events = new LinkedList<>();
        for (int i = 1; i <= PacketFactory.PAGE_SIZE + 1; i++) {
            events.add("?eve" + i);
        }
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, null);
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
        List<String> events = new LinkedList<>();
        for (int i = 0; i < PacketFactory.PAGE_SIZE * 2 - 2; i++) {
            events.add("?eve" + i);
        }
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, null);
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
        List<String> events = new LinkedList<>();
        for (int i = 0; i < PacketFactory.PAGE_SIZE * 3; i++) {
            events.add("?eve" + i);
        }
        URL url = new URL("http://example.com/");
        PacketFactory factory = new PacketFactory(url, null);
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

    @Test
    public void testPacking() throws Exception {
//        List<String> events = new LinkedList<>();
//        for (int i = 0; i < PacketFactory.getPageSize() * 2; i++) {
//            events.add("eve" + i);
//        }
//        PacketFactory wrapper = new PacketFactory(new URL("http://example.com/"), events, null);
//
//        Iterator<PacketFactory.Page> it = wrapper.iterator();
//        assertTrue(it.hasNext());
//        while (it.hasNext()) {
//            PacketFactory.Page page = it.next();
//            assertEquals(page.elementsCount(), PacketFactory.getPageSize());
//            JSONArray requests = wrapper.getPacket(page).getPostData().getJSONArray("requests");
//            assertEquals(requests.length(), PacketFactory.getPageSize());
//            assertTrue(requests.get(0).toString().startsWith("eve"));
//            assertTrue(requests.get(PacketFactory.getPageSize() - 1).toString().length() >= 4);
//            assertFalse(page.isEmpty());
//        }
//        assertFalse(it.hasNext());
//        assertNull(it.next());
    }
}