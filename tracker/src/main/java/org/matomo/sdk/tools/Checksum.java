/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Offers to calculate checksums
 */
public class Checksum {
    private static final String HEXES = "0123456789ABCDEF";

    /**
     * Transforms byte into hex representation.
     */
    public static String getHex(byte[] raw) {
        if (raw == null)
            return null;
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw)
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        return hex.toString();
    }

    /**
     * MD5-Checksum for a string.
     */
    public static String getMD5Checksum(String string) throws Exception {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(string.getBytes());
        byte[] messageDigest = digest.digest();
        return getHex(messageDigest);
    }

    /**
     * MD5-Checksum for a file.
     */
    public static String getMD5Checksum(File file) throws Exception {
        if (!file.isFile())
            return null;
        InputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0)
                complete.update(buffer, 0, numRead);
        } while (numRead != -1);
        fis.close();
        return getHex(complete.digest());
    }
}
