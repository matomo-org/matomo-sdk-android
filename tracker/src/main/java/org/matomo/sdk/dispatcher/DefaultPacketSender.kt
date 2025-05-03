package org.matomo.sdk.dispatcher

import org.matomo.sdk.Matomo.Companion.tag
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

class DefaultPacketSender : PacketSender {
    private var mTimeout = Dispatcher.DEFAULT_CONNECTION_TIMEOUT.toLong()
    private var mGzip = false

    override fun send(packet: Packet): Exception? {
        var urlConnection: HttpURLConnection? = null
        try {
            urlConnection = URL(packet.targetURL).openConnection() as HttpURLConnection

            Timber.tag(TAG).v("Connection is open to %s", urlConnection.url.toExternalForm())
            Timber.tag(TAG).v("Sending: %s", packet)

            urlConnection.connectTimeout = mTimeout.toInt()
            urlConnection.readTimeout = mTimeout.toInt()

            // IF there is json data we have to do a post
            if (packet.postData != null) { // POST
                urlConnection.doOutput = true // Forces post
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("charset", "utf-8")

                val toPost = packet.postData.toString()
                if (mGzip) {
                    urlConnection.addRequestProperty("Content-Encoding", "gzip")
                    val byteArrayOS = ByteArrayOutputStream()

                    GZIPOutputStream(byteArrayOS).use { gzipStream ->
                        gzipStream.write(toPost.toByteArray(StandardCharsets.UTF_8))
                    }
                    // If closing fails we assume the written data to be invalid.
                    // Don't catch the exception and let it abort the `send(Packet)` call.
                    var outputStream: OutputStream? = null
                    try {
                        outputStream = urlConnection.outputStream
                        outputStream.write(byteArrayOS.toByteArray())
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close()
                            } catch (e: IOException) {
                                // Failing to close the stream is not enough to consider the transmission faulty.
                                Timber.tag(TAG).d(e, "Failed to close output stream after writing gzipped POST data.")
                            }
                        }
                    }
                } else {
                    var writer: BufferedWriter? = null
                    try {
                        writer = BufferedWriter(OutputStreamWriter(urlConnection.outputStream, StandardCharsets.UTF_8))
                        writer.write(toPost)
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close()
                            } catch (e: IOException) {
                                // Failing to close the stream is not enough to consider the transmission faulty.
                                Timber.tag(TAG).d(e, "Failed to close output stream after writing POST data.")
                            }
                        }
                    }
                }
            } else { // GET
                urlConnection.doOutput = false // Defaults to false, but for readability
            }

            val statusCode = urlConnection.responseCode
            Timber.tag(TAG).v("Transmission finished (code=%d).", statusCode)
            val successful = checkResponseCode(statusCode)

            if (successful) {
                // https://github.com/matomo-org/matomo-sdk-android/issues/226

                val `is` = urlConnection.inputStream
                if (`is` != null) {
                    try {
                        `is`.close()
                    } catch (e: IOException) {
                        Timber.tag(TAG).d(e, "Failed to close the error stream.")
                    }
                }
            } else {
                // Consume the error stream (or at least close it) if the status code was non-OK (not 2XX)
                val errorReason = StringBuilder()
                var errorReader: BufferedReader? = null
                try {
                    errorReader = BufferedReader(InputStreamReader(urlConnection.errorStream))
                    var line: String?
                    while ((errorReader.readLine().also { line = it }) != null) errorReason.append(line)
                } finally {
                    if (errorReader != null) {
                        try {
                            errorReader.close()
                        } catch (e: IOException) {
                            Timber.tag(TAG).d(e, "Failed to close the error stream.")
                        }
                    }
                }
                Timber.tag(TAG).w("Transmission failed (code=%d, reason=%s)", statusCode, errorReason.toString())
            }

            return null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Transmission failed unexpectedly.")

            return e
        } finally {
            urlConnection?.disconnect()
        }
    }

    override fun setTimeout(timeout: Long) {
        mTimeout = timeout
    }

    override fun setGzipData(gzip: Boolean) {
        mGzip = gzip
    }

    companion object {
        private val TAG = tag(DefaultPacketSender::class.java)
        private fun checkResponseCode(code: Int): Boolean {
            return code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_OK
        }
    }
}
