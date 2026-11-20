package org.matomo.sdk.dispatcher


interface PacketSender {
    /**
     * @return true if the packet must not be sent again: it was sent successfully, or the server
     * rejected it permanently
     */
    fun send(packet: Packet): Boolean

    /**
     * @param timeout in milliseconds
     */
    fun setTimeout(timeout: Long)

    fun setGzipData(gzip: Boolean)
}
