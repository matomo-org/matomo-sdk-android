package org.piwik.sdk.dispatcher;

import org.piwik.sdk.Piwik;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

import timber.log.Timber;


public class DefaultPacketSender implements PacketSender {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "DefaultPacketSender";
    private long mTimeout = Dispatcher.DEFAULT_CONNECTION_TIMEOUT;
    private boolean mGzip = false;

    public boolean send(Packet packet) {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(packet.getTargetURL()).openConnection();
            Timber.tag(LOGGER_TAG).v("Connection open to %s", urlConnection.getURL().toExternalForm());
            urlConnection.setConnectTimeout((int) mTimeout);
            urlConnection.setReadTimeout((int) mTimeout);

            // IF there is json data we have to do a post
            if (packet.getPostData() != null) { // POST
                urlConnection.setDoOutput(true); // Forces post
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("charset", "utf-8");

                final String toPost = packet.getPostData().toString();
                if (mGzip) {

                    urlConnection.addRequestProperty("Content-Encoding", "gzip");
                    ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

                    GZIPOutputStream gzipStream = null;
                    try {
                        gzipStream = new GZIPOutputStream(byteArrayOS);
                        gzipStream.write(toPost.getBytes(Charset.forName("UTF8")));
                    } finally {
                        // If closing fails we assume the written data to be invalid.
                        // Don't catch the exception and let it abort the `send(Packet)` call.
                        if (gzipStream != null) gzipStream.close();
                    }

                    OutputStream outputStream = null;
                    try {
                        outputStream = urlConnection.getOutputStream();
                        outputStream.write(byteArrayOS.toByteArray());
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                // Failing to close the stream is not enough to consider the transmission faulty.
                                Timber.tag(LOGGER_TAG).d(e, "Failed to close output stream after writing gzipped POST data.");
                            }
                        }
                    }

                } else {

                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                        writer.write(toPost);
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                // Failing to close the stream is not enough to consider the transmission faulty.
                                Timber.tag(LOGGER_TAG).d(e, "Failed to close output stream after writing POST data.");
                            }
                        }
                    }

                }

            } else { // GET
                urlConnection.setDoOutput(false); // Defaults to false, but for readability
            }

            int statusCode = urlConnection.getResponseCode();
            final boolean successful = checkResponseCode(statusCode);

            if (successful) {
                Timber.tag(LOGGER_TAG).v("Transmission succesful (code=%d).", statusCode);
            } else {
                // Consume the error stream (or at least close it) if the statuscode was non-OK (not 2XX)
                final StringBuilder errorReason = new StringBuilder();
                BufferedReader errorReader = null;
                try {
                    errorReader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String line;
                    while ((line = errorReader.readLine()) != null) errorReason.append(line);
                } finally {
                    if (errorReader != null) {
                        try {
                            errorReader.close();
                        } catch (IOException e) {
                            Timber.tag(LOGGER_TAG).d(e, "Failed to close the error stream.");
                        }
                    }
                }
                Timber.tag(LOGGER_TAG).w("Transmission failed (code=%d, reason=%s)", statusCode, errorReason.toString());
            }

            return successful;
        } catch (Exception e) {
            Timber.tag(LOGGER_TAG).e(e, "Transmission failed unexpectedly.");
            return false;
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
    }

    @Override
    public void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    @Override
    public void setGzipData(boolean gzip) {
        mGzip = gzip;
    }

    public static boolean checkResponseCode(int code) {
        return code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_OK;
    }
}
