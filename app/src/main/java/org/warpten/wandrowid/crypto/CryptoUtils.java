package org.warpten.wandrowid.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class CryptoUtils {
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
