package org.matomo.sdk.tools

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import testhelpers.BaseTest
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class ChecksumTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testgetMD5Checksum() {
        val md5 = Checksum.getMD5Checksum("foo")
        Assert.assertEquals(md5, "ACBD18DB4CC2F85CEDEF654FCCC4A4D8")
    }

    @Test
    fun testHex() {
        Assert.assertNull(Checksum.getHex(null))
    }

    @Test
    @Throws(Exception::class)
    fun testgetMD5ChecksumDir() {
        val directory = File(".", "")
        val md5 = Checksum.getMD5Checksum(directory)
        Assert.assertNull(md5)
    }
}
