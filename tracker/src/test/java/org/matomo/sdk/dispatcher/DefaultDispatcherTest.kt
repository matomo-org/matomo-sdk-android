/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher

import org.awaitility.Awaitility
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe
import org.matomo.sdk.tools.Connectivity
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import testhelpers.BaseTest
import testhelpers.TestHelper
import java.util.Collections
import java.util.Random
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DefaultDispatcherTest : BaseTest() {
    private var mDispatcher: DefaultDispatcher? = null

    @Mock
    var mEventCache: EventCache? = null

    @Mock
    var mPacketSender: PacketSender? = null

    @Mock
    var mConnectivity: Connectivity? = null
    private val mApiUrl: String = "http://example.com"

    private val mEventCacheData: LinkedBlockingQueue<Event> = LinkedBlockingQueue()

    @Before
    @Throws(Exception::class)
    override fun setup() {
        super.setup()
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mConnectivity!!.isConnected).thenReturn(true)
        Mockito.`when`(mConnectivity!!.type).thenReturn(Connectivity.Type.MOBILE)

        Mockito.doAnswer { invocation: InvocationOnMock ->
            mEventCacheData.add(invocation.getArgument(0))
            null
        }.`when`(mEventCache)?.add(ArgumentMatchers.any(Event::class.java))
        Mockito.`when`(mEventCache!!.isEmpty).then(Answer { mEventCacheData.isEmpty() } as Answer<Boolean>)
        Mockito.`when`(mEventCache!!.updateState(ArgumentMatchers.anyBoolean()))
            .thenAnswer { invocation: InvocationOnMock -> invocation.getArgument<Any>(0) as Boolean && !mEventCacheData.isEmpty() }
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val drainTarget = invocation.getArgument<MutableList<Event>>(0)
            mEventCacheData.drainTo(drainTarget)
            null
        }.`when`(mEventCache)?.drainTo(ArgumentMatchers.anyList())
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val toRequeue = invocation.getArgument<List<Event>>(0)
            mEventCacheData.addAll(toRequeue)
            null
        }.`when`(mEventCache)?.requeue(ArgumentMatchers.anyList())
        Mockito.doAnswer {
            mEventCacheData.clear()
            null
        }.`when`(mEventCache)?.clear()
        mDispatcher = DefaultDispatcher(mEventCache!!, mConnectivity!!, PacketFactory(mApiUrl), mPacketSender!!)
    }

    @Test
    fun testClear() {
        mDispatcher!!.clear()
        Mockito.verify(mEventCache)?.clear()
    }

    @Test
    fun testClear_cleanExit() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>())
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.submit(testEvent)
        mDispatcher!!.forceDispatch()

        TestHelper.sleep(100)
        MatcherAssert.assertThat(dryRunData.size, Is.`is`(1))
        dryRunData.clear()

        Mockito.`when`(mConnectivity!!.isConnected).thenReturn(false)
        mDispatcher!!.submit(testEvent)

        TestHelper.sleep(100)
        MatcherAssert.assertThat(mEventCacheData.size, Is.`is`(1))

        mDispatcher!!.clear()

        Mockito.`when`(mConnectivity!!.isConnected).thenReturn(true)
        mDispatcher!!.forceDispatch()

        TestHelper.sleep(100)
        MatcherAssert.assertThat(dryRunData.size, Is.`is`(0))
    }

    @Test
    fun testGetDispatchMode() {
        Assert.assertEquals(DispatchMode.ALWAYS, mDispatcher!!.dispatchMode)
        mDispatcher!!.dispatchMode = DispatchMode.WIFI_ONLY
        Assert.assertEquals(DispatchMode.WIFI_ONLY, mDispatcher!!.dispatchMode)
    }

    @Test
    fun testDispatchMode_wifiOnly() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>())
        mDispatcher!!.setDryRunTarget(dryRunData)
        Mockito.`when`(mConnectivity!!.type).thenReturn(Connectivity.Type.MOBILE)

        mDispatcher!!.dispatchMode = DispatchMode.WIFI_ONLY
        mDispatcher!!.submit(testEvent)
        mDispatcher!!.forceDispatch()

        Mockito.verify(mEventCache, Mockito.timeout(1000))?.updateState(false)
        Mockito.verify(mEventCache, Mockito.never())?.drainTo(ArgumentMatchers.anyList())

        Mockito.`when`(mConnectivity!!.type).thenReturn(Connectivity.Type.WIFI)
        mDispatcher!!.forceDispatch()
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until({ dryRunData.size }, Is.`is`(1))

        Mockito.verify(mEventCache)?.updateState(true)
        Mockito.verify(mEventCache)?.drainTo(ArgumentMatchers.anyList())
    }

    @Test
    fun testConnectivityChange() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>())
        mDispatcher!!.setDryRunTarget(dryRunData)
        Mockito.`when`(mConnectivity!!.isConnected).thenReturn(false)

        mDispatcher!!.submit(testEvent)
        mDispatcher!!.forceDispatch()

        Mockito.verify(mEventCache, Mockito.timeout(1000))?.add(ArgumentMatchers.any())
        Mockito.verify(mEventCache, Mockito.never())?.drainTo(ArgumentMatchers.anyList())
        MatcherAssert.assertThat(dryRunData.size, Is.`is`(0))

        Mockito.`when`(mConnectivity!!.isConnected).thenReturn(true)
        mDispatcher!!.forceDispatch()

        Awaitility.await().atMost(1, TimeUnit.SECONDS).until({ dryRunData.size }, Is.`is`(1))

        Mockito.verify(mEventCache)?.updateState(true)
        Mockito.verify(mEventCache)?.drainTo(ArgumentMatchers.anyList())
    }

    @Test
    fun testGetDispatchGzipped() {
        Assert.assertFalse(mDispatcher!!.dispatchGzipped)
        mDispatcher!!.dispatchGzipped = true
        Assert.assertTrue(mDispatcher!!.dispatchGzipped)
        Mockito.verify(mPacketSender)?.setGzipData(true)
    }

    @Test
    fun testDefaultConnectionTimeout() {
        Assert.assertEquals(Dispatcher.DEFAULT_CONNECTION_TIMEOUT.toLong(), mDispatcher!!.connectionTimeOut.toLong())
    }

    @Test
    fun testSetConnectionTimeout() {
        mDispatcher!!.connectionTimeOut = 100
        Assert.assertEquals(100, mDispatcher!!.connectionTimeOut.toLong())
        Mockito.verify(mPacketSender)?.setTimeout(100)
    }

    @Test
    fun testDefaultDispatchInterval() {
        Assert.assertEquals(Dispatcher.DEFAULT_DISPATCH_INTERVAL, mDispatcher!!.dispatchInterval)
    }

    @Test
    fun testForceDispatchTwice() {
        mDispatcher!!.dispatchInterval = -1
        mDispatcher!!.connectionTimeOut = 20
        mDispatcher!!.submit(testEvent)

        Assert.assertTrue(mDispatcher!!.forceDispatch())
        Assert.assertFalse(mDispatcher!!.forceDispatch())
    }

    @Test
    @Throws(Exception::class)
    fun testMultiThreadDispatch() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>()).toMutableList()
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.dispatchInterval = 20

        val threadCount = 20
        val queryCount = 100
        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData)
    }

    @Test
    @Throws(Exception::class)
    fun testForceDispatch() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>()).toMutableList()
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.dispatchInterval = -1L

        val threadCount = 10
        val queryCount = 10
        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)
        TestHelper.sleep(500)
        Assert.assertEquals((threadCount * queryCount).toLong(), createdEvents.size.toLong())
        Assert.assertEquals(0, dryRunData.size.toLong())
        mDispatcher!!.forceDispatch()

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData)
    }

    @Test
    @Throws(Exception::class)
    fun testBatchDispatch() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>()).toMutableList()
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.dispatchInterval = 1500

        val threadCount = 5
        val queryCount = 5
        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until({ createdEvents.size }, Is.`is`(threadCount * queryCount))
        Assert.assertEquals(0, dryRunData.size.toLong())

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until({ createdEvents.size }, Is.`is`(threadCount * queryCount))
        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData)
    }

    @Test
    @Throws(Exception::class)
    fun testBlockingDispatch() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>()).toMutableList()
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.dispatchInterval = -1

        val threadCount = 5
        val queryCount = 5
        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until({ createdEvents.size }, Is.`is`(threadCount * queryCount))

        Assert.assertEquals(dryRunData.size.toLong(), 0)
        Assert.assertEquals(createdEvents.size.toLong(), (threadCount * queryCount).toLong())

        mDispatcher!!.forceDispatchBlocking()

        val flattenedQueries: List<String> = getFlattenedQueries(dryRunData)
        Assert.assertEquals(flattenedQueries.size.toLong(), (threadCount * queryCount).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testBlockingDispatchInFlight() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>()).toMutableList()
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.dispatchInterval = 20

        val threadCount = 5
        val queryCount = 5
        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until({ createdEvents.size }, Is.`is`(threadCount * queryCount))

        Assert.assertEquals(createdEvents.size.toLong(), (threadCount * queryCount).toLong())
        Assert.assertNotEquals(ArrayList<Any?>(dryRunData).size.toLong(), 0)

        mDispatcher!!.forceDispatchBlocking()

        val flattenedQueries: List<String> = getFlattenedQueries(dryRunData)
        Assert.assertEquals(flattenedQueries.size.toLong(), (threadCount * queryCount).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testBlockingDispatchCollision() {
        val lock = Semaphore(0)
        val eventCount = AtomicInteger(0)

        mDispatcher!!.dispatchInterval = -1

        Mockito.`when`<Exception?>(mPacketSender!!.send(ArgumentMatchers.any())).thenAnswer { invocation: InvocationOnMock ->
            val packet = invocation.getArgument<Packet>(0)
            eventCount.addAndGet(packet.eventCount)

            lock.release()
            Thread.sleep(100)
            true
        }

        val threadCount = 7
        val queryCount = 13
        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until({ createdEvents.size }, Is.`is`(threadCount * queryCount))

        mDispatcher!!.forceDispatch()

        lock.acquire()

        mDispatcher!!.forceDispatchBlocking()

        Assert.assertEquals(eventCount.get().toLong(), (threadCount * queryCount).toLong())
    }

    @Test
    fun testBlockingDispatchExceptionMode() {
        mDispatcher!!.dispatchInterval = 200

        val threadCount = 5
        val queryCount = 10

        val createdEvents = Collections.synchronizedList(ArrayList<String>())
        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)

        val sentEvents = AtomicInteger(0)

        Mockito.`when`<Exception?>(mPacketSender!!.send(ArgumentMatchers.any())).thenAnswer { invocation: InvocationOnMock ->
            val packet = invocation.getArgument<Packet>(0)
            sentEvents.addAndGet(packet.eventCount)

            mDispatcher!!.dispatchMode = DispatchMode.EXCEPTION
            true
        }

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until({ createdEvents.size }, Is.`is`(threadCount * queryCount))

        mDispatcher!!.forceDispatchBlocking()

        val sentEventCount = sentEvents.get()

        Assert.assertEquals(sentEventCount.toLong(), PacketFactory.PAGE_SIZE.toLong())
        Assert.assertEquals((mEventCacheData.size + sentEventCount).toLong(), (threadCount * queryCount).toLong())
    }

    @Test
    fun testDispatchRetryWithBackoff() {
        val cnt = AtomicInteger(0)
        Mockito.`when`(mPacketSender!!.send(ArgumentMatchers.any()))
            .then(Answer { cnt.incrementAndGet() > 5 } as Answer<Boolean>)

        mDispatcher!!.dispatchInterval = 100
        mDispatcher!!.submit(testEvent)

        Awaitility.await().atLeast(100, TimeUnit.MILLISECONDS).until { cnt.get() == 1 }
        Awaitility.await().atLeast(100, TimeUnit.MILLISECONDS).until { cnt.get() == 2 }

        Awaitility.await().atMost(1900, TimeUnit.MILLISECONDS).until { cnt.get() == 5 }

        mDispatcher!!.submit(testEvent)
        Awaitility.await().atMost(150, TimeUnit.MILLISECONDS).until { cnt.get() == 5 }
    }

    @Test
    fun testDispatchInterval() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>())
        mDispatcher!!.setDryRunTarget(dryRunData)
        mDispatcher!!.dispatchInterval = 500
        MatcherAssert.assertThat(dryRunData.isEmpty(), Is.`is`(true))
        mDispatcher!!.submit(testEvent)
        Awaitility.await().atLeast(500, TimeUnit.MILLISECONDS).until { dryRunData.size == 1 }
    }

    @Test
    @Throws(Exception::class)
    fun testRandomDispatchIntervals() {
        val dryRunData = Collections.synchronizedList(ArrayList<Packet>()).toMutableList()
        mDispatcher!!.setDryRunTarget(dryRunData)

        val threadCount = 10
        val queryCount = 100
        val createdEvents = Collections.synchronizedList(ArrayList<String>())

        Thread {
            try {
                while (getFlattenedQueries(ArrayList(dryRunData)).size != threadCount * queryCount) {
                    mDispatcher!!.dispatchInterval = (Random().nextInt(20 + 1) - 1).toLong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        launchTestThreads(mApiUrl, mDispatcher, threadCount, queryCount, createdEvents)

        checkForMIAs(threadCount * queryCount, createdEvents, dryRunData)
    }

    companion object {
        @Throws(Exception::class)
        fun checkForMIAs(expectedEvents: Int, createdEvents: MutableList<String>, dryRunOutput: List<Packet>) {
            var previousEventCount = 0
            var previousFlatQueryCount = 0
            var flattenedQueries: MutableList<String>
            var nothingHappenedCounter = 0
            while (true) {
                TestHelper.sleep(100)
                flattenedQueries = getFlattenedQueries(ArrayList(dryRunOutput))
                if (flattenedQueries.size == expectedEvents) {
                    break
                } else {
                    flattenedQueries = getFlattenedQueries(ArrayList(dryRunOutput))
                    val currentEventCount = createdEvents.size
                    val currentFlatQueryCount = flattenedQueries.size
                    if (previousEventCount != currentEventCount && previousFlatQueryCount != currentFlatQueryCount) {
                        previousEventCount = currentEventCount
                        previousFlatQueryCount = currentFlatQueryCount
                        nothingHappenedCounter = 0
                    } else {
                        nothingHappenedCounter++
                        if (nothingHappenedCounter > 50) Assert.fail("Test seems stuck, nothing happens")
                    }
                }
            }

            Assert.assertEquals(flattenedQueries.size.toLong(), expectedEvents.toLong())
            Assert.assertEquals(createdEvents.size.toLong(), expectedEvents.toLong())

            // We are done, lets make sure can find all send queries in our dispatched results
            while (createdEvents.isNotEmpty()) {
                val query: String = createdEvents.removeAt(0)
                Assert.assertTrue(flattenedQueries.remove(query))
            }
            Assert.assertTrue(true)
            Assert.assertTrue(flattenedQueries.isEmpty())
        }

        fun launchTestThreads(apiUrl: String, dispatcher: Dispatcher?, threadCount: Int, queryCount: Int, createdQueries: MutableList<String>) {
            for (i in 0 until threadCount) {
                Thread {
                    try {
                        for (j in 0 until queryCount) {
                            TestHelper.sleep(Random().nextInt(20).toLong())
                            val trackMe = TrackMe()
                                .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                                .set(QueryParams.EVENT_VALUE, j)
                            dispatcher!!.submit(trackMe)
                            createdQueries.add(apiUrl + Event(trackMe.toMap()).encodedQuery)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Assert.fail()
                    }
                }.start()
            }
        }

        @Throws(Exception::class)
        fun getFlattenedQueries(packets: List<Packet?>): MutableList<String> {
            val flattenedQueries: MutableList<String> = ArrayList()
            for (request in packets) {
                if (request!!.postData != null) {
                    val batchedRequests = request.postData!!.getJSONArray("requests")
                    for (json in 0 until batchedRequests.length()) {
                        val unbatchedRequest = request.targetURL + batchedRequests[json].toString()
                        flattenedQueries.add(unbatchedRequest)
                    }
                } else {
                    flattenedQueries.add(request.targetURL)
                }
            }
            return flattenedQueries
        }

        val testEvent: TrackMe
            get() {
                val trackMe = TrackMe()
                trackMe[QueryParams.SESSION_START] = 1
                return trackMe
            }
    }
}
