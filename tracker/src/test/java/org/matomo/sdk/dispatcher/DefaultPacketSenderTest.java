package org.matomo.sdk.dispatcher;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import testhelpers.BaseTest;
import testhelpers.TestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPacketSenderTest extends BaseTest {

    DefaultPacketSender mDefaultPacketSender;
    MockWebServer mMockWebServer;

    @Before
    public void setup() throws Exception {
        super.setup();
        mDefaultPacketSender = new DefaultPacketSender();
        mMockWebServer = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        mMockWebServer.close();
        super.tearDown();
    }

    @Test
    public void testDispatch() throws Exception {
        mMockWebServer.start();

        Packet packet = mock(Packet.class);
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").toString());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        when(packet.getPostData()).thenReturn(jsonObject);

        mMockWebServer.enqueue(new MockResponse());
        mDefaultPacketSender.send(packet);

        final RecordedRequest recordedRequest = mMockWebServer.takeRequest();
        assertThat(recordedRequest, is(not(nullValue())));

        String body = recordedRequest.getBody().readUtf8();
        assertThat(jsonObject.toString(), is(body));
        assertThat(recordedRequest.getHeader("Content-Encoding"), is((nullValue())));
    }

    @Test
    public void testGzip() throws Exception {
        mMockWebServer.start();

        Packet packet = mock(Packet.class);
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").toString());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        when(packet.getPostData()).thenReturn(jsonObject);

        mMockWebServer.enqueue(new MockResponse());
        mDefaultPacketSender.send(packet);
        assertThat(mMockWebServer.takeRequest().getHeader("Content-Encoding"), is((nullValue())));

        mDefaultPacketSender.setGzipData(true);

        mMockWebServer.enqueue(new MockResponse());
        mDefaultPacketSender.send(packet);
        assertThat(mMockWebServer.takeRequest().getHeader("Content-Encoding"), is("gzip"));
    }

    @Test
    public void testPermanentlyRejectedPacketIsNotSentAgain() throws Exception {
        mMockWebServer.start();
        Packet packet = mockPacket();

        for (int code : new int[]{400, 401, 403, 404}) {
            mMockWebServer.enqueue(new MockResponse().setResponseCode(code).setBody("rejected"));
            assertThat("status " + code, mDefaultPacketSender.send(packet), is(true));
        }
    }

    @Test
    public void testPermanentlyRejectedPacketWithoutBodyIsNotSentAgain() throws Exception {
        mMockWebServer.start();
        Packet packet = mockPacket();

        mMockWebServer.enqueue(new MockResponse().setResponseCode(400));
        assertThat(mDefaultPacketSender.send(packet), is(true));
    }

    @Test
    public void testFailingToReadTheErrorBodyDoesNotChangeWhetherThePacketIsSentAgain() throws Exception {
        mMockWebServer.start();
        Packet packet = mockPacket();
        mDefaultPacketSender.setTimeout(200);

        // the status line arrives, but reading the body times out
        mMockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("rejected").setBodyDelay(2, TimeUnit.SECONDS));
        assertThat(mDefaultPacketSender.send(packet), is(true));

        mMockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("failed").setBodyDelay(2, TimeUnit.SECONDS));
        assertThat(mDefaultPacketSender.send(packet), is(false));
    }

    @Test
    public void testPacketIsSentAgainAfterAServerErrorOrRetryableClientError() throws Exception {
        mMockWebServer.start();
        Packet packet = mockPacket();

        for (int code : new int[]{408, 429, 500, 503}) {
            mMockWebServer.enqueue(new MockResponse().setResponseCode(code));
            assertThat("status " + code, mDefaultPacketSender.send(packet), is(false));
        }
    }

    @Test
    public void testTimeout() throws Exception {
        mMockWebServer.start();

        Packet packet = mock(Packet.class);
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").toString());

        mDefaultPacketSender.setTimeout(50);
        mMockWebServer.setDispatcher(new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest recordedRequest) {
                TestHelper.sleep(100);
                return new MockResponse();
            }
        });
        assertThat(mDefaultPacketSender.send(packet), is(false));

        mMockWebServer.setDispatcher(new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest recordedRequest) {
                return new MockResponse();
            }
        });
        assertThat(mDefaultPacketSender.send(packet), is(true));
    }

    private Packet mockPacket() {
        Packet packet = mock(Packet.class);
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").toString());
        return packet;
    }
}
