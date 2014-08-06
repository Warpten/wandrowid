package org.warpten.wandrowid.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * A wrapper class around Java's BigInteger. It uses Little Endian.
 */
public class BigNumber {
    private BigInteger integer;

    public static BigNumber ZERO = new BigNumber(BigInteger.ZERO);
    public static BigNumber ONE = new BigNumber(BigInteger.ONE);

    public BigNumber()
    {
        integer = BigInteger.ZERO;
    }

    public BigNumber(BigInteger num)
    {
        integer = num;
    }

    public BigNumber(byte[] array/*, boolean isLittleEndian = true */)
    {
        this(array, true);
    }

    /*
     * BigInteger internally uses Big endian magnitude
     */
    public BigNumber(byte[] array, boolean isLittleEndian)
    {
        if (!isLittleEndian)
        {
            integer = new BigInteger(array);
            return;
        }

        byte[] rArray = ReverseArray(array);

        if (rArray[0] < 0)
        {
            byte[] tempArray = new byte[rArray.length + 1];
            System.arraycopy(rArray, 0, tempArray, 1, rArray.length);
            rArray = tempArray;
        }

        integer = new BigInteger(rArray);
    }

    public BigNumber(String hexStr/*, int radix = 16 */)
    {
        this(hexStr, 16);
    }

    public BigNumber(String str, int radix)
    {
        integer = new BigInteger(str, radix);
    }

    public byte[] toByteArray(/* int minSize = integer.toByteArray.length(), boolean asLittleEndian = true */)
    {
        return toByteArray(integer.toByteArray().length, true);
    }

    public byte[] toByteArray(int minSize)
    {
        return toByteArray(minSize, true);
    }

    public byte[] toByteArray(int minSize, boolean asLittleEndian)
    {
        byte[] array = integer.toByteArray();
        if (array[0] == 0)
        {
            byte[] temp = new byte[array.length - 1];
            System.arraycopy(array, 1, temp, 0, temp.length);
            array = temp;
        }

        if (asLittleEndian)
            array = ReverseArray(array);

        if (minSize > array.length)
        {
            byte[] newArray = new byte[minSize];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
		
		/*
		 * Possible bugfix (extra byte appended by ctor)
		if (array[0] < 0)
		    return Arrays.copyOfRange(array, 1, array.length);
		 */

        return array;
    }

    public String toHexString()
    {
        return integer.toString(16).toUpperCase();
    }

    public String toString() { return toHexString(); }

    public BigInteger getBigInteger() { return integer; }

    public BigNumber add(BigNumber num)
    {
        return new BigNumber(integer.add(num.getBigInteger()));
    }

    public BigNumber substract(BigNumber num)
    {
        return new BigNumber(integer.subtract(num.getBigInteger()));
    }

    public BigNumber multiply(BigNumber num)
    {
        return new BigNumber(integer.multiply(num.getBigInteger()));
    }

    public BigNumber mod(BigNumber num)
    {
        return new BigNumber(integer.mod(num.getBigInteger()));
    }

    public BigNumber modPow(BigNumber mod, BigNumber pow)
    {
        return new BigNumber(integer.modPow(mod.getBigInteger(), pow.getBigInteger()));
    }

    private static byte[] ReverseArray(byte[] array)
    {
        byte[] rArray = new byte[array.length];
        for (int i = 0; i < array.length; ++i)
            rArray[i] = array[array.length - i - 1];
        return rArray;
    }

    public static BigNumber setRandBytes(int byteCount)
    {
        return new BigNumber(new BigInteger(1, (new SecureRandom()).generateSeed(byteCount)));
    }
}
