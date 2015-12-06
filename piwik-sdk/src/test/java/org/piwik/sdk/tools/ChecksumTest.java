package org.piwik.sdk.tools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.FullEnvTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class ChecksumTest {

    @Test
    public void testgetMD5Checksum() throws Exception {
        String md5 = Checksum.getMD5Checksum("foo");
        assertEquals(md5, "ACBD18DB4CC2F85CEDEF654FCCC4A4D8");
    }

    @Test
    public void testgetMD5ChecksumDir() throws Exception {
        File directory = new File(".", "");
        String md5 = Checksum.getMD5Checksum(directory);
        assertTrue(md5 == null);
    }

}
