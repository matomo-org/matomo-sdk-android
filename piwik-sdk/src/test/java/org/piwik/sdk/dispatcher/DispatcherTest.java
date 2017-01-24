/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk.dispatcher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.Tracker;
import org.piwik.sdk.testhelper.FullEnvTestRunner;
import org.robolectric.annotation.Config;

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


@SuppressWarnings("deprecation")
@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class DispatcherTest {

    Dispatcher dispatcher;
    EventCache eventCache;
    @Mock Piwik piwik;
    @Mock EventDiskCache eventDiskCache;
    @Mock Tracker tracker;
    @Mock ConnectivityManager connectivityManager;
    @Mock Context context;
    @Mock NetworkInfo networkInfo;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(tracker.getPiwik()).thenReturn(piwik);
        when(tracker.getAPIUrl()).thenReturn(new URL("http://example.com"));
        when(piwik.isDryRun()).thenReturn(true);
        when(piwik.getContext()).thenReturn(context);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.isConnected()).thenReturn(true);

        when(eventDiskCache.isEmpty()).thenReturn(true);
        eventCache = spy(new EventCache(eventDiskCache));
        dispatcher = new Dispatcher(tracker, eventCache);
    }

    @Test
    public void testConnectivityChange() throws Exception {
        when(eventDiskCache.isEmpty()).thenReturn(false);
        when(networkInfo.isConnected()).thenReturn(false);
        dispatcher.submit("Test");
        dispatcher.forceDispatch();
        Thread.sleep(50);
        verify(eventDiskCache, never()).uncache();
        verify(eventDiskCache).cache(ArgumentMatchers.<Event>anyList());
        when(networkInfo.isConnected()).thenReturn(true);
        dispatcher.forceDispatch();
        Thread.sleep(50);
        verify(eventDiskCache).uncache();
    }

    @Test
    public void testDispatch_gzip() throws Exception {
        when(piwik.isDryRun()).thenReturn(false);

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

        dispatcher.setDispatchGzipped(false);
        dispatcher.dispatch(packet);
        verify(urlConnection, never()).addRequestProperty("Content-Encoding", "gzip");

        dispatcher.setDispatchGzipped(true);
        dispatcher.dispatch(packet);
        verify(urlConnection).addRequestProperty("Content-Encoding", "gzip");
    }

    @Test
    public void testDefaultConnectionTimeout() throws Exception {
        assertEquals(Dispatcher.DEFAULT_CONNECTION_TIMEOUT, dispatcher.getConnectionTimeOut());
    }

    @Test
    public void testSetConnectionTimeout() throws Exception {
        dispatcher.setConnectionTimeOut(100);
        assertEquals(100, dispatcher.getConnectionTimeOut());
    }

    @Test
    public void testDefaultDispatchInterval() throws Exception {
        assertEquals(Dispatcher.DEFAULT_DISPATCH_INTERVAL, dispatcher.getDispatchInterval());
    }

    @Test
    public void testForceDispatchTwice() throws Exception {
        dispatcher.setDispatchInterval(-1);
        dispatcher.setConnectionTimeOut(20);
        dispatcher.submit("url");

        assertTrue(dispatcher.forceDispatch());
        assertFalse(dispatcher.forceDispatch());
    }

    @Test
    public void testUrlEncodeUTF8() throws Exception {
        assertEquals(Dispatcher.urlEncodeUTF8((String) null), "");
    }

    @Test
    public void testMultiThreadDispatch() throws Exception {
        dispatcher.setDispatchInterval(20);

        final int threadCount = 20;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, dispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dispatcher.getDryRunOutput());
    }

    @Test
    public void testForceDispatch() throws Exception {
        dispatcher.setDispatchInterval(-1L);

        final int threadCount = 10;
        final int queryCount = 10;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, dispatcher, threadCount, queryCount, createdEvents);
        Thread.sleep(500);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, dispatcher.getDryRunOutput().size());
        dispatcher.forceDispatch();

        checkForMIAs(threadCount * queryCount, createdEvents, dispatcher.getDryRunOutput());
    }

    @Test
    public void testBatchDispatch() throws Exception {
        dispatcher.setDispatchInterval(1500);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, dispatcher, threadCount, queryCount, createdEvents);
        Thread.sleep(1000);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, dispatcher.getDryRunOutput().size());
        Thread.sleep(1000);

        checkForMIAs(threadCount * queryCount, createdEvents, dispatcher.getDryRunOutput());
    }

    @Test
    public void testRandomDispatchIntervals() throws Exception {

        final int threadCount = 10;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (getFlattenedQueries(new ArrayList<>(dispatcher.getDryRunOutput())).size() != threadCount * queryCount)
                        dispatcher.setDispatchInterval(new Random().nextInt(20 - -1) + -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

        launchTestThreads(tracker, dispatcher, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, dispatcher.getDryRunOutput());
    }

    public static void checkForMIAs(int expectedEvents, List<String> createdEvents, List<Packet> dryRunOutput) throws Exception {
        int previousEventCount = 0;
        int previousFlatQueryCount = 0;
        List<String> flattenedQueries;
        while (true) {
            Thread.sleep(500);
            flattenedQueries = getFlattenedQueries(new ArrayList<>(dryRunOutput));
            Log.d("checkForMIAs", createdEvents.size() + " events created, " + dryRunOutput.size() + " requests dispatched, containing " + flattenedQueries.size() + " flattened queries");
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
        Log.d("checkForMIAs", "All send queries are accounted for.");
    }

    public static void launchTestThreads(final Tracker tracker, final Dispatcher dispatcher, int threadCount, final int queryCount, final List<String> createdQueries) {
        Log.d("launchTestThreads", "Launching " + threadCount + " threads, " + queryCount + " queries each");
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
                            String event = Dispatcher.urlEncodeUTF8(trackMe.toMap());
                            dispatcher.submit(event);
                            createdQueries.add(tracker.getAPIUrl().toString() + event);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        assertFalse(true);
                    }
                }
            }).start();
        }
        Log.d("launchTestThreads", "All launched.");
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
}