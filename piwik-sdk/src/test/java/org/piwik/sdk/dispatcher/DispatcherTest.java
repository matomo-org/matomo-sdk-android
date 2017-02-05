/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.tools.Connectivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ALL")
public class DispatcherTest {

    Dispatcher mDispatcher;
    EventCache mEventCache;
    @Mock EventDiskCache mEventDiskCache;
    @Mock Connectivity mConnectivity;
    @Mock Context mContext;
    URL mApiUrl;

    @Before
    public void setup() throws Exception {
        mApiUrl = new URL("http://example.com");
        MockitoAnnotations.initMocks(this);
        when(mConnectivity.isConnected()).thenReturn(true);
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.MOBILE);

        when(mEventDiskCache.isEmpty()).thenReturn(true);
        mEventCache = spy(new EventCache(mEventDiskCache));
        mDispatcher = new Dispatcher(mEventCache, mConnectivity, new PacketFactory(mApiUrl));
    }

    @Test
    public void testClear() {
        mDispatcher.clear();
        verify(mEventCache).clear();
    }

    @Test
    public void testClear_cleanExit() throws InterruptedException {
        final List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.submit(getGuestEvent());
        mDispatcher.forceDispatch();
        Thread.sleep(100);
        assertFalse(dryRunData.isEmpty());
        dryRunData.clear();

        when(mConnectivity.isConnected()).thenReturn(false);
        mDispatcher.submit(getGuestEvent());
        assertFalse(mEventCache.isEmpty());
        mDispatcher.clear();
        when(mConnectivity.isConnected()).thenReturn(true);
        assertTrue(mEventCache.isEmpty());
        verify(mEventCache).clear();
        Thread.sleep(100);
        assertTrue(dryRunData.isEmpty());
    }

    @Test
    public void testGetDispatchMode() {
        assertEquals(DispatchMode.ALWAYS, mDispatcher.getDispatchMode());
        mDispatcher.setDispatchMode(DispatchMode.WIFI_ONLY);
        assertEquals(DispatchMode.WIFI_ONLY, mDispatcher.getDispatchMode());
    }

    @Test
    public void testDispatchMode_wifiOnly() throws Exception {
        when(mEventDiskCache.isEmpty()).thenReturn(false);
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.MOBILE);
        mDispatcher.setDispatchMode(DispatchMode.WIFI_ONLY);
        mDispatcher.submit(getGuestEvent());
        mDispatcher.forceDispatch();
        Thread.sleep(50);
        verify(mEventDiskCache, never()).uncache();
        verify(mEventDiskCache).cache(ArgumentMatchers.<Event>anyList());
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.WIFI);
        mDispatcher.forceDispatch();
        Thread.sleep(50);
        verify(mEventDiskCache).uncache();
    }

    @Test
    public void testConnectivityChange() throws Exception {
        when(mEventDiskCache.isEmpty()).thenReturn(false);
        when(mConnectivity.isConnected()).thenReturn(false);
        mDispatcher.submit(getGuestEvent());
        mDispatcher.forceDispatch();
        Thread.sleep(50);
        verify(mEventDiskCache, never()).uncache();
        verify(mEventDiskCache).cache(ArgumentMatchers.<Event>anyList());
        when(mConnectivity.isConnected()).thenReturn(true);
        mDispatcher.forceDispatch();
        Thread.sleep(50);
        verify(mEventDiskCache).uncache();
    }

    @Test
    public void testGetDispatchGzipped() {
        assertFalse(mDispatcher.getDispatchGzipped());
        mDispatcher.setDispatchGzipped(true);
        assertTrue(mDispatcher.getDispatchGzipped());
    }

    @Test
    public void testDispatch_gzip() throws Exception {
        Packet packet = mock(Packet.class);

        URL url = new URL("http://example.com");
        when(packet.getTargetURL()).thenReturn(url);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "test");
        when(packet.getPostData()).thenReturn(jsonObject);

        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(packet.openConnection()).thenReturn(urlConnection);
        OutputStream outputStream = mock(OutputStream.class);
        when(urlConnection.getOutputStream()).thenReturn(outputStream);

        mDispatcher.setDispatchGzipped(false);
        mDispatcher.dispatch(packet);
        verify(urlConnection, never()).addRequestProperty("Content-Encoding", "gzip");

        mDispatcher.setDispatchGzipped(true);
        mDispatcher.dispatch(packet);
        verify(urlConnection).addRequestProperty("Content-Encoding", "gzip");
    }

    @Test
    public void testDefaultConnectionTimeout() throws Exception {
        assertEquals(Dispatcher.DEFAULT_CONNECTION_TIMEOUT, mDispatcher.getConnectionTimeOut());
    }

    @Test
    public void testSetConnectionTimeout() throws Exception {
        mDispatcher.setConnectionTimeOut(100);
        assertEquals(100, mDispatcher.getConnectionTimeOut());
    }

    @Test
    public void testDefaultDispatchInterval() throws Exception {
        assertEquals(Dispatcher.DEFAULT_DISPATCH_INTERVAL, mDispatcher.getDispatchInterval());
    }

    @Test
    public void testForceDispatchTwice() throws Exception {
        mDispatcher.setDispatchInterval(-1);
        mDispatcher.setConnectionTimeOut(20);
        mDispatcher.submit(getGuestEvent());

        assertTrue(mDispatcher.forceDispatch());
        assertFalse(mDispatcher.forceDispatch());
    }

    @Test
    public void testMultiThreadDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(20);

        final int threadCount = 20;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testForceDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(-1L);

        final int threadCount = 10;
        final int queryCount = 10;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);
        Thread.sleep(500);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, dryRunData.size());
        mDispatcher.forceDispatch();

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testBatchDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(1500);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);
        Thread.sleep(1000);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, dryRunData.size());
        Thread.sleep(1000);

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testRandomDispatchIntervals() throws Exception {
        final List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);

        final int threadCount = 10;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (getFlattenedQueries(new ArrayList<>(dryRunData)).size() != threadCount * queryCount)
                        mDispatcher.setDispatchInterval(new Random().nextInt(20 - -1) + -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    public static void checkForMIAs(int expectedEvents, List<String> createdEvents, List<Packet> dryRunOutput) throws Exception {
        int previousEventCount = 0;
        int previousFlatQueryCount = 0;
        List<String> flattenedQueries;
        while (true) {
            Thread.sleep(500);
            flattenedQueries = getFlattenedQueries(new ArrayList<>(dryRunOutput));
            if (flattenedQueries.size() == expectedEvents) {
                break;
            } else {
                int currentEventCount = createdEvents.size();
                int currentFlatQueryCount = flattenedQueries.size();
                assertNotEquals(previousEventCount, currentEventCount);
                assertNotEquals(previousFlatQueryCount, currentFlatQueryCount);
                previousEventCount = currentEventCount;
                previousFlatQueryCount = currentFlatQueryCount;
            }
        }

        assertEquals(flattenedQueries.size(), expectedEvents);
        assertEquals(createdEvents.size(), expectedEvents);

        // We are done, lets make sure can find all send queries in our dispatched results
        while (!createdEvents.isEmpty()) {
            String query = createdEvents.remove(0);
            assertTrue(flattenedQueries.remove(query));
        }
        assertTrue(createdEvents.isEmpty());
        assertTrue(flattenedQueries.isEmpty());
    }

    public static void launchTestThreads(final URL apiUrl, final Dispatcher dispatcher, int threadCount, final int queryCount, final List<String> createdQueries) {
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < queryCount; j++) {
                            Thread.sleep(new Random().nextInt(20 - 0) + 0);
                            TrackMe trackMe = new TrackMe()
                                    .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_VALUE, j);
                            dispatcher.submit(trackMe);
                            createdQueries.add(apiUrl.toString() + new Event(trackMe.toMap()).getEncodedQuery());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        assertFalse(true);
                    }
                }
            }).start();
        }
    }

    public static List<String> getFlattenedQueries(List<Packet> packets) throws Exception {
        List<String> flattenedQueries = new ArrayList<>();
        for (Packet request : packets) {
            if (request.getPostData() != null) {
                JSONArray batchedRequests = request.getPostData().getJSONArray("requests");
                for (int json = 0; json < batchedRequests.length(); json++) {
                    String unbatchedRequest = request.getTargetURL().toExternalForm() + batchedRequests.get(json).toString();
                    flattenedQueries.add(unbatchedRequest);
                }
            } else {
                flattenedQueries.add(request.getTargetURL().toExternalForm());
            }
        }
        return flattenedQueries;
    }

    public static TrackMe getGuestEvent() {
        TrackMe trackMe = new TrackMe();
        trackMe.set(QueryParams.SESSION_START, 1);
        return trackMe;
    }
}