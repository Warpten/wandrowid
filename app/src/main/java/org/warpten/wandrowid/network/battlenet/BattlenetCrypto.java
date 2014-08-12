package org.warpten.wandrowid.network.battlenet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import gnu.javax.crypto.prng.ARCFour;

/**
 * Created by perquet on 11/08/14.
 */
public class BattlenetCrypto {
    private static ARCFour dCipher;
    private static ARCFour eCipher;
    private static boolean Initialized = false;

    private BattlenetCrypto() { }

    public static void Init(byte[] k) {
        try {
            byte[] decryptionSeed = new byte[] {
                    (byte)0x68, (byte)0xE0, (byte)0xC7, (byte)0x2E,
                    (byte)0xDD, (byte)0xD6, (byte)0xD2, (byte)0xF3,
                    (byte)0x1E, (byte)0x5A, (byte)0xB1, (byte)0x55,
                    (byte)0xB1, (byte)0x8B, (byte)0x63, (byte)0x1E
            };
            byte[] encryptionSeed = new byte[] {
                    (byte)0xDE, (byte)0xA9, (byte)0x65, (byte)0xAE,
                    (byte)0x54, (byte)0x3A, (byte)0x1E, (byte)0x93,
                    (byte)0x9E, (byte)0x69, (byte)0x0C, (byte)0xAA,
                    (byte)0x68, (byte)0xDE, (byte)0x78, (byte)0x39
            };

            SecretKeySpec ks = new SecretKeySpec(decryptionSeed, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(ks);

            dCipher = new ARCFour();
            dCipher.init(mac.doFinal(k));

            ks = new SecretKeySpec(decryptionSeed, "HmacSHA256");
            mac.init(ks);

            eCipher = new ARCFour();
            eCipher.init(mac.doFinal(k));
            Initialized = true;
        } catch (Exception e) {
            Initialized = false;
            e.printStackTrace();
        }
    }

    public static void Decrypt(byte[] data) {
        if (!Initialized)
            return;

        for (int i = 0; i < data.length; ++i)
            data[i] = (byte)(dCipher.nextByte() ^ (data[i] & 0xFF));
    }

    public static void Encrypt(byte[] data) {
        if (!Initialized)
            return;

        for (int i = 0; i < data.length; ++i)
            data[i] ^= dCipher.nextByte();
    }
}
