/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.tools.Connectivity;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import testhelpers.BaseTest;
import testhelpers.TestHelper;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DefaultDispatcherTest extends BaseTest {

    DefaultDispatcher mDispatcher;
    @Mock EventCache mEventCache;
    @Mock PacketSender mPacketSender;
    @Mock Connectivity mConnectivity;
    final String mApiUrl = "http://example.com";

    final LinkedBlockingQueue<Event> mEventCacheData = new LinkedBlockingQueue<>();

    @Before
    public void setup() throws Exception {
        super.setup();
        MockitoAnnotations.openMocks(this);
        when(mConnectivity.isConnected()).thenReturn(true);
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.MOBILE);

        doAnswer(invocation -> {
            mEventCacheData.add(invocation.getArgument(0));
            return null;
        }).when(mEventCache).add(any(Event.class));
        when(mEventCache.isEmpty()).then((Answer<Boolean>) invocation -> mEventCacheData.isEmpty());
        when(mEventCache.updateState(anyBoolean())).thenAnswer(invocation -> (Boolean) invocation.getArgument(0) && !mEventCacheData.isEmpty());
        doAnswer(invocation -> {
            List<Event> drainTarget = invocation.getArgument(0);
            mEventCacheData.drainTo(drainTarget);
            return null;
        }).when(mEventCache).drainTo(ArgumentMatchers.anyList());
        doAnswer(invocation -> {
            List<Event> toRequeue = invocation.getArgument(0);
            mEventCacheData.addAll(toRequeue);
            return null;
        }).when(mEventCache).requeue(ArgumentMatchers.anyList());
        doAnswer(invocation -> {
            mEventCacheData.clear();
            return null;
        }).when(mEventCache).clear();
        mDispatcher = new DefaultDispatcher(mEventCache, mConnectivity, new PacketFactory(mApiUrl), mPacketSender);
    }

    @Test
    public void testClear() {
        mDispatcher.clear();
        verify(mEventCache).clear();
    }

    @Test
    public void testClear_cleanExit() {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.submit(getTestEvent());
        mDispatcher.forceDispatch();

        TestHelper.sleep(100);
        assertThat(dryRunData.size(), is(1));
        dryRunData.clear();

        when(mConnectivity.isConnected()).thenReturn(false);
        mDispatcher.submit(getTestEvent());

        TestHelper.sleep(100);
        assertThat(mEventCacheData.size(), is(1));

        mDispatcher.clear();

        when(mConnectivity.isConnected()).thenReturn(true);
        mDispatcher.forceDispatch();

        TestHelper.sleep(100);
        assertThat(dryRunData.size(), is(0));
    }

    @Test
    public void testGetDispatchMode() {
        assertEquals(DispatchMode.ALWAYS, mDispatcher.getDispatchMode());
        mDispatcher.setDispatchMode(DispatchMode.WIFI_ONLY);
        assertEquals(DispatchMode.WIFI_ONLY, mDispatcher.getDispatchMode());
    }

    @Test
    public void testDispatchMode_wifiOnly() {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.MOBILE);

        mDispatcher.setDispatchMode(DispatchMode.WIFI_ONLY);
        mDispatcher.submit(getTestEvent());
        mDispatcher.forceDispatch();

        verify(mEventCache, timeout(1000)).updateState(false);
        verify(mEventCache, never()).drainTo(ArgumentMatchers.anyList());

        when(mConnectivity.getType()).thenReturn(Connectivity.Type.WIFI);
        mDispatcher.forceDispatch();
        await().atMost(1, TimeUnit.SECONDS).until(dryRunData::size, is(1));

        verify(mEventCache).updateState(true);
        verify(mEventCache).drainTo(ArgumentMatchers.anyList());
    }

    @Test
    public void testConnectivityChange() {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        when(mConnectivity.isConnected()).thenReturn(false);

        mDispatcher.submit(getTestEvent());
        mDispatcher.forceDispatch();

        verify(mEventCache, timeout(1000)).add(any());
        verify(mEventCache, never()).drainTo(ArgumentMatchers.anyList());
        assertThat(dryRunData.size(), is(0));

        when(mConnectivity.isConnected()).thenReturn(true);
        mDispatcher.forceDispatch();

        await().atMost(1, TimeUnit.SECONDS).until(dryRunData::size, is(1));

        verify(mEventCache).updateState(true);
        verify(mEventCache).drainTo(ArgumentMatchers.anyList());
    }

    @Test
    public void testGetDispatchGzipped() {
        assertFalse(mDispatcher.getDispatchGzipped());
        mDispatcher.setDispatchGzipped(true);
        assertTrue(mDispatcher.getDispatchGzipped());
        verify(mPacketSender).setGzipData(true);
    }

    @Test
    public void testDefaultConnectionTimeout() {
        assertEquals(Dispatcher.DEFAULT_CONNECTION_TIMEOUT, mDispatcher.getConnectionTimeOut());
    }

    @Test
    public void testSetConnectionTimeout() {
        mDispatcher.setConnectionTimeOut(100);
        assertEquals(100, mDispatcher.getConnectionTimeOut());
        verify(mPacketSender).setTimeout(100);
    }

    @Test
    public void testDefaultDispatchInterval() {
        assertEquals(Dispatcher.DEFAULT_DISPATCH_INTERVAL, mDispatcher.getDispatchInterval());
    }

    @Test
    public void testForceDispatchTwice() {
        mDispatcher.setDispatchInterval(-1);
        mDispatcher.setConnectionTimeOut(20);
        mDispatcher.submit(getTestEvent());

        assertTrue(mDispatcher.forceDispatch());
        assertFalse(mDispatcher.forceDispatch());
    }

    @Test
    public void testMultiThreadDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(20);

        final int threadCount = 20;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testForceDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(-1L);

        final int threadCount = 10;
        final int queryCount = 10;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);
        TestHelper.sleep(500);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, dryRunData.size());
        mDispatcher.forceDispatch();

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testBatchDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(1500);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        await().atMost(2, TimeUnit.SECONDS).until(createdEvents::size, is(threadCount * queryCount));
        assertEquals(0, dryRunData.size());

        await().atMost(2, TimeUnit.SECONDS).until(createdEvents::size, is(threadCount * queryCount));
        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testBlockingDispatch() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(-1);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);
        await().atMost(2, TimeUnit.SECONDS).until(createdEvents::size, is(threadCount * queryCount));

        assertEquals(dryRunData.size(), 0);
        assertEquals(createdEvents.size(), threadCount * queryCount);

        mDispatcher.forceDispatchBlocking();

        List<String> flattenedQueries = getFlattenedQueries(dryRunData);
        assertEquals(flattenedQueries.size(), threadCount * queryCount);
    }

    @Test
    public void testBlockingDispatchInFlight() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(20);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);
        await().atMost(2, TimeUnit.SECONDS).until(createdEvents::size, is(threadCount * queryCount));

        assertEquals(createdEvents.size(), threadCount * queryCount);
        assertNotEquals(new ArrayList(dryRunData).size(), 0);

        mDispatcher.forceDispatchBlocking();

        List<String> flattenedQueries = getFlattenedQueries(dryRunData);
        assertEquals(flattenedQueries.size(), threadCount * queryCount);
    }

    @Test
    public void testBlockingDispatchCollision() throws Exception {
        final Semaphore lock = new Semaphore(0);
        final AtomicInteger eventCount = new AtomicInteger(0);

        mDispatcher.setDispatchInterval(-1);

        when(mPacketSender.send(any())).thenAnswer((Answer<Boolean>) invocation -> {
            Packet packet = invocation.getArgument(0);

            eventCount.addAndGet(packet.getEventCount());

            lock.release();
            Thread.sleep(100);

            return true;
        });

        final int threadCount = 7;
        final int queryCount = 13;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        await().atMost(2, TimeUnit.SECONDS).until(createdEvents::size, is(threadCount * queryCount));

        mDispatcher.forceDispatch();

        lock.acquire();

        mDispatcher.forceDispatchBlocking();

        assertEquals(eventCount.get(), threadCount * queryCount);
    }

    @Test
    public void testBlockingDispatchExceptionMode() {
        mDispatcher.setDispatchInterval(200);

        final int threadCount = 5;
        final int queryCount = 10;

        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        final AtomicInteger sentEvents = new AtomicInteger(0);

        when(mPacketSender.send(any())).thenAnswer((Answer<Boolean>) invocation -> {
            Packet packet = invocation.getArgument(0);
            sentEvents.addAndGet(packet.getEventCount());

            mDispatcher.setDispatchMode(DispatchMode.EXCEPTION);

            return true;
        });

        await().atMost(2, TimeUnit.SECONDS).until(createdEvents::size, is(threadCount * queryCount));

        mDispatcher.forceDispatchBlocking();

        int sentEventCount = sentEvents.get();

        assertEquals(sentEventCount, PacketFactory.PAGE_SIZE);
        assertEquals(mEventCacheData.size() + sentEventCount, threadCount * queryCount);
    }

    @Test
    public void testDispatchRetryWithBackoff() {
        AtomicInteger cnt = new AtomicInteger(0);
        when(mPacketSender.send(any())).then((Answer<Boolean>) invocation -> cnt.incrementAndGet() > 5);

        mDispatcher.setDispatchInterval(100);
        mDispatcher.submit(getTestEvent());

        await().atLeast(100, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 1);
        await().atLeast(100, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 2);

        await().atMost(1900, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 5);

        mDispatcher.submit(getTestEvent());
        await().atMost(150, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 5);
    }

    @Test
    public void testDispatchInterval() {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(500);
        assertThat(dryRunData.isEmpty(), is(true));
        mDispatcher.submit(getTestEvent());
        await().atLeast(500, TimeUnit.MILLISECONDS).until(() -> dryRunData.size() == 1);
    }

    @Test
    public void testRandomDispatchIntervals() throws Exception {
        final List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<>());
        mDispatcher.setDryRunTarget(dryRunData);

        final int threadCount = 10;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<>());

        new Thread(() -> {
            try {
                while (getFlattenedQueries(new ArrayList<>(dryRunData)).size() != threadCount * queryCount) {
                    mDispatcher.setDispatchInterval(new Random().nextInt(20 + 1) - 1);
                }
            } catch (Exception e) {e.printStackTrace();}
        }).start();

        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    public static void checkForMIAs(int expectedEvents, List<String> createdEvents, List<Packet> dryRunOutput) throws Exception {
        int previousEventCount = 0;
        int previousFlatQueryCount = 0;
        List<String> flattenedQueries;
        int nothingHappenedCounter = 0;
        while (true) {
            TestHelper.sleep(100);
            flattenedQueries = getFlattenedQueries(new ArrayList<>(dryRunOutput));
            if (flattenedQueries.size() == expectedEvents) {
                break;
            } else {
                flattenedQueries = getFlattenedQueries(new ArrayList<>(dryRunOutput));
                int currentEventCount = createdEvents.size();
                int currentFlatQueryCount = flattenedQueries.size();
                if (previousEventCount != currentEventCount && previousFlatQueryCount != currentFlatQueryCount) {
                    previousEventCount = currentEventCount;
                    previousFlatQueryCount = currentFlatQueryCount;
                    nothingHappenedCounter = 0;
                } else {
                    nothingHappenedCounter++;
                    if (nothingHappenedCounter > 50)
                        fail("Test seems stuck, nothing happens");
                }
            }
        }

        assertEquals(flattenedQueries.size(), expectedEvents);
        assertEquals(createdEvents.size(), expectedEvents);

        // We are done, lets make sure can find all send queries in our dispatched results
        while (!createdEvents.isEmpty()) {
            String query = createdEvents.remove(0);
            assertTrue(flattenedQueries.remove(query));
        }
        assertTrue(true);
        assertTrue(flattenedQueries.isEmpty());
    }

    public static void launchTestThreads(final String apiUrl, final Dispatcher dispatcher, int threadCount, final int queryCount, final List<String> createdQueries) {
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < queryCount; j++) {
                        TestHelper.sleep(new Random().nextInt(20));
                        TrackMe trackMe = new TrackMe()
                                .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_VALUE, j);
                        dispatcher.submit(trackMe);
                        createdQueries.add(apiUrl + new Event(trackMe.toMap()).getEncodedQuery());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
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
                    String unbatchedRequest = request.getTargetURL() + batchedRequests.get(json).toString();
                    flattenedQueries.add(unbatchedRequest);
                }
            } else {
                flattenedQueries.add(request.getTargetURL());
            }
        }
        return flattenedQueries;
    }

    public static TrackMe getTestEvent() {
        TrackMe trackMe = new TrackMe();
        trackMe.set(QueryParams.SESSION_START, 1);
        return trackMe;
    }
}
