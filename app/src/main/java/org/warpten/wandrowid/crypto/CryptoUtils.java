package org.warpten.wandrowid.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class CryptoUtils {
    public static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString().toUpperCase();
    }

    public static byte[] SHA1(String... text) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.reset();
        for (int i = 0; i < text.length; ++i)
            md.update(text[i].getBytes("iso-8859-1"), 0, text[i].length());
        return md.digest();
    }


    public static byte[] SHA1(byte[]... arrays) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.reset();
        for (int i = 0; i < arrays.length; ++i)
            md.update(arrays[i], 0, arrays[i].length);
        return md.digest();
    }

    public static byte[] Take(byte[] input, int size)
    {
        return Arrays.copyOfRange(input, 0, size);
    }
}
