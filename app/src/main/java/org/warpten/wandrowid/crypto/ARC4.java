package org.warpten.wandrowid.crypto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import gnu.javax.crypto.prng.ARCFour;

public class ARC4 {
    private static ARCFour dCipher;
    private static ARCFour eCipher;

    // private static SARC4 dCipher;
    // private static SARC4 eCipher;

    public static boolean Enabled = false;

    public static void SetupKey(byte[] key, byte[] decryptionSeed, byte[] encryptionSeed)
    {
        try {
            byte[] skipSize = new byte[1024];
            Arrays.fill(skipSize, (byte)0);

            SecretKeySpec ks = new SecretKeySpec(decryptionSeed, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(ks);
            dCipher = new ARCFour();
            dCipher.init(mac.doFinal(key));
            dCipher.nextBytes(skipSize);

            Arrays.fill(skipSize, (byte)0);

            ks = new SecretKeySpec(encryptionSeed, "HmacSHA1");
            mac = Mac.getInstance("HmacSHA1");
            mac.init(ks);
            eCipher = new ARCFour();
            eCipher.init(mac.doFinal(key));
            eCipher.nextBytes(new byte[1024]);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static byte[] DecryptPacketHeader(byte[] incomingBuffer) {
        ByteBuffer data = ByteBuffer.wrap(incomingBuffer, 0, 5);
        byte[] dataBytes = new byte[5];
        byte[] returnValue = new byte[4]; // Actual data returned

        if (!Enabled) {
            data.get(returnValue); // Unencrypted, 4 bytes (2size + u16 opc)
        } else {
            data.get(dataBytes); // Not sure, read all 5 bytes

            byte firstHeaderByte = DecryptByte(dataBytes[0]);
            boolean isLargeHeader = (firstHeaderByte & 0x80) != 0;
            if (isLargeHeader) {
                returnValue = new byte[5]; // Increase size
                returnValue[0] = (byte)(0x7F & firstHeaderByte);
            } else
                returnValue[0] = firstHeaderByte;

            // Decrypt the remaining bytes
            byte[] headerRemains = DecryptBytes(dataBytes, 1, returnValue.length);
            System.arraycopy(headerRemains, 0, returnValue, 1, headerRemains.length);
        }

        return returnValue;
    }

    public static byte[] DecryptBytes(byte[] input, int start, int end) {
        if (!Enabled)
            return input;

        // Server header:
        // header[5 || 4]
        // size uint8[3 || 2]
        // command uint16

        int length = end - start;
        byte[] retVal = new byte[length];
        byte[] cipherBytes = new byte[length];
        dCipher.nextBytes(cipherBytes);
        for (int i = 0; i < length; ++i)
            retVal[i] = (byte)(cipherBytes[i] ^ (input[start + i] & 0xFF));

        return retVal;
    }

    public static byte DecryptByte(byte input)
    {
        if (!Enabled)
            return input;

        byte[] cipherByte = new byte[1];
        dCipher.nextBytes(cipherByte);
        return (byte)(cipherByte[0] ^ (input & 0xFF));
    }

    /**
     * Called by WorldSocket::ToByteBuffer().
     * @param opcode opcode ID
     * @param pkt    opcode content
     * @param enableAfterBuild Activate ARC4 AFTER packet is sent
     * @return  ByteBuffer bytebuffer with size, opc, content
     */
    public static ByteBuffer EncryptPacket(int opcode, ByteBuffer pkt, boolean enableAfterBuild)
    {
        // Client packet:
        // uint16 size;
        // uint32 cmd;

        // Last two bytes from "WORL(..)" are dropped and written by WriteAsciiCString
        // This is why size is only 4 bytes if MSG_VERIFY_CONNECTIVITY
        // Vodoo below: + 2 for "WO", other +2 for opcode get added iff opcode isnt 4F57
        int packetLength = pkt.position() + (opcode == 0x4F57 ? 2 : 4);
        pkt.flip();
        ByteBuffer sendableData = ByteBuffer.allocate(packetLength + 2); // Allocate 2 bytes for size too
        byte[] header = new byte[opcode == 0x4F57 ? 4 : 6];
        header[0] = (byte)(packetLength >> 8);
        header[1] = (byte)(packetLength & 0xFF);
        header[2] = (byte)(opcode & 0xFF);
        header[3] = (byte)(opcode >> 8);
        if (opcode != 0x4F57)
            header[4] = header[5] = 0;

        if (Enabled) {
            byte[] cipherBytes = new byte[header.length];
            eCipher.nextBytes(cipherBytes);
            for (int i = 0; i < header.length; i++)
                header[i] ^= cipherBytes[i];
        }
        sendableData.order(ByteOrder.LITTLE_ENDIAN);
        sendableData.put(header);
        sendableData.put(pkt);
        sendableData.flip(); // Make ready for the socket

        if (enableAfterBuild)
            Enabled = true;

        return sendableData;
    }
}
