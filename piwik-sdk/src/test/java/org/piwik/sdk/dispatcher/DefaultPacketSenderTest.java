package org.piwik.sdk.dispatcher;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPacketSenderTest {

    DefaultPacketSender mDefaultPacketSender;
    MockWebServer mMockWebServer;

    @Before
    public void setup() {
        mDefaultPacketSender = new DefaultPacketSender();
        mMockWebServer = new MockWebServer();
    }

    @After
    public void tearDown() throws IOException {
        mMockWebServer.close();
    }

    @Test
    public void testDispatch() throws Exception {
        mMockWebServer.start();

        Packet packet = mock(Packet.class);
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").url());
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
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").url());
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
    public void testTimeout() throws Exception {
        mMockWebServer.start();

        Packet packet = mock(Packet.class);
        when(packet.getTargetURL()).thenReturn(mMockWebServer.url("/").url());

        mDefaultPacketSender.setTimeout(50);
        mMockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
                Thread.sleep(100);
                return new MockResponse();
            }
        });
        assertThat(mDefaultPacketSender.send(packet), is(false));

        mMockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
                return new MockResponse();
            }
        });
        assertThat(mDefaultPacketSender.send(packet), is(true));
    }
}
