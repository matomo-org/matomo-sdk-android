/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.content.Context;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.tools.Connectivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import testhelpers.BaseTest;
import testhelpers.TestHelper;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ALL")
public class DefaultDispatcherTest extends BaseTest {

    DefaultDispatcher mDispatcher;
    @Mock EventCache mEventCache;
    @Mock PacketSender mPacketSender;
    @Mock Connectivity mConnectivity;
    @Mock Context mContext;
    final String mApiUrl = "http://example.com";

    final LinkedBlockingQueue<Event> mEventCacheData = new LinkedBlockingQueue<>();

    @Before
    public void setup() throws Exception {
        super.setup();
        MockitoAnnotations.initMocks(this);
        when(mConnectivity.isConnected()).thenReturn(true);
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.MOBILE);

        doAnswer(invocation -> {
            mEventCacheData.add((Event) invocation.getArgument(0));
            return null;
        }).when(mEventCache).add(any(Event.class));
        when(mEventCache.isEmpty()).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return mEventCacheData.isEmpty();
            }
        });
        when(mEventCache.updateState(anyBoolean())).thenAnswer(invocation -> {
            return (Boolean) invocation.getArgument(0) && !mEventCacheData.isEmpty();
        });
        doAnswer(invocation -> {
            List<Event> drainTarget = invocation.getArgument(0);
            mEventCacheData.drainTo(drainTarget);
            return null;
        }).when(mEventCache).drainTo(Matchers.anyList());
        doAnswer(invocation -> {
            List<Event> toRequeue = invocation.getArgument(0);
            mEventCacheData.addAll(toRequeue);
            return null;
        }).when(mEventCache).requeue(Matchers.anyList());
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
    public void testClear_cleanExit() throws InterruptedException {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
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
    public void testDispatchMode_wifiOnly() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        when(mConnectivity.getType()).thenReturn(Connectivity.Type.MOBILE);

        mDispatcher.setDispatchMode(DispatchMode.WIFI_ONLY);
        mDispatcher.submit(getTestEvent());
        mDispatcher.forceDispatch();

        verify(mEventCache, timeout(1000)).updateState(false);
        verify(mEventCache, never()).drainTo(Matchers.anyList());

        when(mConnectivity.getType()).thenReturn(Connectivity.Type.WIFI);
        mDispatcher.forceDispatch();
        await().atMost(1, TimeUnit.SECONDS).until(() -> dryRunData.size(), is(1));

        verify(mEventCache).updateState(true);
        verify(mEventCache).drainTo(Matchers.anyList());
    }

    @Test
    public void testConnectivityChange() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        when(mConnectivity.isConnected()).thenReturn(false);

        mDispatcher.submit(getTestEvent());
        mDispatcher.forceDispatch();

        verify(mEventCache, timeout(1000)).add(any());
        verify(mEventCache, never()).drainTo(Matchers.anyList());
        assertThat(dryRunData.size(), is(0));

        when(mConnectivity.isConnected()).thenReturn(true);
        mDispatcher.forceDispatch();

        await().atMost(1, TimeUnit.SECONDS).until(() -> dryRunData.size(), is(1));

        verify(mEventCache).updateState(true);
        verify(mEventCache).drainTo(Matchers.anyList());
    }

    @Test
    public void testGetDispatchGzipped() {
        assertFalse(mDispatcher.getDispatchGzipped());
        mDispatcher.setDispatchGzipped(true);
        assertTrue(mDispatcher.getDispatchGzipped());
        verify(mPacketSender).setGzipData(true);
    }

    @Test
    public void testDefaultConnectionTimeout() throws Exception {
        assertEquals(Dispatcher.DEFAULT_CONNECTION_TIMEOUT, mDispatcher.getConnectionTimeOut());
    }

    @Test
    public void testSetConnectionTimeout() throws Exception {
        mDispatcher.setConnectionTimeOut(100);
        assertEquals(100, mDispatcher.getConnectionTimeOut());
        verify(mPacketSender).setTimeout(100);
    }

    @Test
    public void testDefaultDispatchInterval() throws Exception {
        assertEquals(Dispatcher.DEFAULT_DISPATCH_INTERVAL, mDispatcher.getDispatchInterval());
    }

    @Test
    public void testForceDispatchTwice() throws Exception {
        mDispatcher.setDispatchInterval(-1);
        mDispatcher.setConnectionTimeOut(20);
        mDispatcher.submit(getTestEvent());

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
        TestHelper.sleep(500);
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

        await().atMost(2, TimeUnit.SECONDS).until(() -> createdEvents.size(), is(threadCount * queryCount));
        assertEquals(0, dryRunData.size());

        await().atMost(2, TimeUnit.SECONDS).until(() -> createdEvents.size(), is(threadCount * queryCount));
        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    @Test
    public void testDispatchRetryWithBackoff() throws Exception {
        AtomicInteger cnt = new AtomicInteger(0);
        when(mPacketSender.send(any())).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return cnt.incrementAndGet() > 5;
            }
        });

        mDispatcher.setDispatchInterval(100);
        mDispatcher.submit(getTestEvent());

        await().atLeast(100, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 1);
        await().atLeast(100, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 2);

        await().atMost(1900, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 5);

        mDispatcher.submit(getTestEvent());
        await().atMost(150, TimeUnit.MILLISECONDS).until(() -> cnt.get() == 5);
    }

    @Test
    public void testDispatchInterval() throws Exception {
        List<Packet> dryRunData = Collections.synchronizedList(new ArrayList<Packet>());
        mDispatcher.setDryRunTarget(dryRunData);
        mDispatcher.setDispatchInterval(500);
        assertThat(dryRunData.isEmpty(), is(true));
        mDispatcher.submit(getTestEvent());
        await().atLeast(500, TimeUnit.MILLISECONDS).until(() -> dryRunData.size() == 1);
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
                    while (getFlattenedQueries(new ArrayList<>(dryRunData)).size() != threadCount * queryCount) {
                        mDispatcher.setDispatchInterval(new Random().nextInt(20 - -1) + -1);
                    }
                } catch (Exception e) {e.printStackTrace();}
            }
        }).start();

        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData);
    }

    public static void checkForMIAs(int expectedEvents, List<String> createdEvents, List<Packet> dryRunOutput) throws Exception {
        int previousEventCount = 0;
        int previousFlatQueryCount = 0;
        List<String> flattenedQueries;
        long lastChange = System.currentTimeMillis();
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
                    lastChange = System.currentTimeMillis();
                    previousEventCount = currentEventCount;
                    previousFlatQueryCount = currentFlatQueryCount;
                    nothingHappenedCounter = 0;
                } else {
                    nothingHappenedCounter++;
                    if (nothingHappenedCounter > 50) assertTrue("Test seems stuck, nothing happens", false);
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
        assertTrue(createdEvents.isEmpty());
        assertTrue(flattenedQueries.isEmpty());
    }

    public static void launchTestThreads(final String apiUrl, final Dispatcher dispatcher, int threadCount, final int queryCount, final List<String> createdQueries) {
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < queryCount; j++) {
                            TestHelper.sleep(new Random().nextInt(20 - 0) + 0);
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
