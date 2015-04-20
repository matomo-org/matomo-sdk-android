/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.piwik.sdk;

import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


@SuppressWarnings("deprecation")
@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class TestDispatcher {

    public Tracker createTracker() throws MalformedURLException {
        TestPiwikApplication app = (TestPiwikApplication) Robolectric.application;
        return Piwik.getInstance(Robolectric.application).newTracker(app.getTrackerUrl(), app.getSiteId());
    }

    public Piwik getPiwik() {
        return Piwik.getInstance(Robolectric.application);
    }

    @Before
    public void setup() {
        Piwik.getInstance(Robolectric.application).setDryRun(true);
        Piwik.getInstance(Robolectric.application).setOptOut(false);
        Piwik.getInstance(Robolectric.application).setDebug(false);
    }

    @Test
    public void testSessionStartRaceCondition() throws Exception {
        for (int i = 0; i < 50; i++) {
            getPiwik().setDryRun(true);
            final Tracker tracker = createTracker();
            tracker.setDispatchInterval(0);
            final int threadCount = 10;
            final int queryCount = 3;
            final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
            launchTestThreads(tracker, threadCount, queryCount, createdEvents);
            checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
            List<String> output = getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
            for (String out : output) {
                if (output.indexOf(out) == 0) {
                    assertTrue(out.contains("lang"));
                    assertTrue(out.contains("_idts"));
                    assertTrue(out.contains("new_visit"));
                } else {
                    assertFalse(out.contains("lang"));
                    assertFalse(out.contains("_idts"));
                    assertFalse(out.contains("new_visit"));
                }
            }
        }
    }

    @Test
    public void testMultiThreadDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(20);

        final int threadCount = 100;
        final int queryCount = 1000;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void testForceDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(-1);

        final int threadCount = 10;
        final int queryCount = 10;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);
        Thread.sleep(500);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, tracker.getDispatcher().getDryRunOutput().size());
        assertTrue(tracker.dispatch());

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void testBatchDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(1000);

        final int threadCount = 10;
        final int queryCount = 10;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);
        Thread.sleep(500);
        assertEquals(threadCount * queryCount, createdEvents.size());
        assertEquals(0, tracker.getDispatcher().getDryRunOutput().size());
        Thread.sleep(500);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void testRandomDispatchIntervals() throws Exception {
        final Tracker tracker = createTracker();

        final int threadCount = 100;
        final int queryCount = 1000;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (getFlattenedQueries(new ArrayList<>(tracker.getDispatcher().getDryRunOutput())).size() != threadCount * queryCount)
                        tracker.setDispatchInterval(new Random().nextInt(20 - -1) + -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

        launchTestThreads(tracker, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    public static void checkForMIAs(int expectedEvents, List<String> createdEvents, List<HttpRequestBase> dryRunOutput) throws Exception {
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

    public static void launchTestThreads(final Tracker tracker, int threadCount, final int queryCount, final List<String> createdQueries) {
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

                            long start = System.currentTimeMillis();
                            tracker.track(trackMe);
                            long stop = System.currentTimeMillis();
                            Log.v("track(TrackMe)", "track(...) build-time:" + (stop - start) + "ms");
                            createdQueries.add(tracker.getAPIUrl().toString() + trackMe.build());
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

    public static List<String> getFlattenedQueries(List<HttpRequestBase> httpRequestList) throws Exception {
        List<String> flattenedQueries = new ArrayList<>();
        for (HttpRequestBase request : httpRequestList) {
            if (request instanceof HttpPost) {
                HttpPost post = (HttpPost) request;
                JSONObject postContent = new JSONObject(EntityUtils.toString((post).getEntity()));
                JSONArray batchedRequests = postContent.getJSONArray("requests");
                for (int json = 0; json < batchedRequests.length(); json++) {
                    String unbatchedRequest = post.getURI() + batchedRequests.get(json).toString();
                    flattenedQueries.add(unbatchedRequest);
                }
            } else if (request instanceof HttpGet) {
                HttpGet get = (HttpGet) request;
                flattenedQueries.add(get.getURI().toString());
            }
        }
        return flattenedQueries;
    }
}