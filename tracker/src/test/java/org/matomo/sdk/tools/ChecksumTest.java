package org.matomo.sdk.tools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import testhelpers.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ChecksumTest extends BaseTest {

    @Test
    public void testgetMD5Checksum() throws Exception {
        String md5 = Checksum.getMD5Checksum("foo");
        assertEquals(md5, "ACBD18DB4CC2F85CEDEF654FCCC4A4D8");
    }

    @Test
    public void testHex() {
        assertNull(Checksum.getHex(null));
    }

    @Test
    public void testgetMD5ChecksumDir() throws Exception {
        File directory = new File(".", "");
        String md5 = Checksum.getMD5Checksum(directory);
        assertNull(md5);
    }

}
