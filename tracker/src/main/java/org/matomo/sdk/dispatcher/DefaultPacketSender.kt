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

    override fun send(packet: Packet): Boolean {
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
            val rejected = isPermanentlyRejected(statusCode)

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
                    // null if the error response has no body
                    urlConnection.errorStream?.let {
                        val reader = BufferedReader(InputStreamReader(it))
                        errorReader = reader
                        reader.lineSequence().forEach { line -> errorReason.append(line) }
                    }
                } catch (e: IOException) {
                    // The body is only read for the log, so a failed read must not decide whether the packet is dropped.
                    Timber.tag(TAG).d(e, "Failed to read the error stream.")
                } finally {
                    try {
                        errorReader?.close()
                    } catch (e: IOException) {
                        Timber.tag(TAG).d(e, "Failed to close the error stream.")
                    }
                }
                if (rejected) {
                    Timber.tag(TAG).w("Transmission rejected, dropping the packet (code=%d, reason=%s)", statusCode, errorReason.toString())
                } else {
                    Timber.tag(TAG).w("Transmission failed (code=%d, reason=%s)", statusCode, errorReason.toString())
                }
            }

            // Sending a rejected packet again would fail the same way and hold up all packets after it.
            return successful || rejected
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Transmission failed unexpectedly.")
            return false
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

        /**
         * A client error the server will answer the same way when the packet is sent again, unlike
         * 408 Request Timeout and 429 Too Many Requests.
         */
        private fun isPermanentlyRejected(code: Int): Boolean {
            return code in 400..499 && code != HttpURLConnection.HTTP_CLIENT_TIMEOUT && code != HTTP_TOO_MANY_REQUESTS
        }

        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
