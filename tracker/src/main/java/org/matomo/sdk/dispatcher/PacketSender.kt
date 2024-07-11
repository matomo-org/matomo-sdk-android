package org.matomo.sdk.dispatcher


interface PacketSender {
    /**
     * @return true if successful
     */
    fun send(packet: Packet): Boolean

    /**
     * @param timeout in milliseconds
     */
    fun setTimeout(timeout: Long)

    fun setGzipData(gzip: Boolean)
}
