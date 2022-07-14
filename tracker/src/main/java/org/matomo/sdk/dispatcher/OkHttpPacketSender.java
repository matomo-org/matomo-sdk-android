package org.matomo.sdk.dispatcher;

import org.matomo.sdk.Matomo;

import java.net.HttpURLConnection;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class OkHttpPacketSender implements PacketSender {

    private static final String TAG = Matomo.tag(OkHttpPacketSender.class);
    private long mTimeout = Dispatcher.DEFAULT_CONNECTION_TIMEOUT;
    private boolean mGzip = true;
    private final OkHttpClient mClient;

    public OkHttpPacketSender(OkHttpClient client) {
        mClient = client;
    }

    @Override
    public boolean send(Packet packet) {
        Request.Builder reqBuilder = new Request.Builder()
                .url(packet.getTargetURL());

        Timber.tag(TAG).d("sending matomo packet");
        if (packet.getPostData() != null) {
            RequestBody body = RequestBody.create(
                    MediaType.get("application/json; charset=urf-8"),
                    packet.getPostData().toString()
            );
            reqBuilder.post(body);
        } else {
            reqBuilder.get();
        }

        Request request = reqBuilder.build();

        boolean isSuccessful;
        Response res = null;
        try {
            res = mClient.newCall(request).execute();
            isSuccessful = res.code() == HttpURLConnection.HTTP_NO_CONTENT || res.code() == HttpURLConnection.HTTP_OK;
            Timber.tag(TAG).d("matomo packet sent successfully");
            Timber.tag(TAG).d("status code: " + res.code());
        } catch (Exception e) {
            isSuccessful = false;

            Timber.tag(TAG).e(e, "matomo packet sent failed");
        } finally {
            if (res != null) {
                res.close();
            }
        }
        return isSuccessful;
    }

    @Override
    public void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    @Override
    public void setGzipData(boolean gzip) {
        mGzip = gzip;
    }
}
