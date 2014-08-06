package org.warpten.wandrowid.handlers.wotlk;

import android.util.Log;

import org.apache.http.util.EncodingUtils;
import org.warpten.wandrowid.G;
import org.warpten.wandrowid.crypto.ARC4;
import org.warpten.wandrowid.crypto.BigNumber;
import org.warpten.wandrowid.crypto.CryptoUtils;
import org.warpten.wandrowid.network.WorldPacket;
import org.warpten.wandrowid.WorldSocket;
import org.warpten.wandrowid.handlers.Handlers;

public class WoTLKHandlers extends Handlers {

    public WoTLKHandlers(WorldSocket socket) {
        super(socket);
    }

    public void CallHandler(WorldPacket opc) {
        WorldPacket sendData = null;
        Log.i("WandroWid", "S->C: Received 0x" + Integer.toString(opc.GetOpcode(), 16).toUpperCase());
        switch (opc.GetOpcode()) {
            case Opcodes.SMSG_AUTH_CHALLENGE:
                sendData = HandleAuthChallenge(opc);
                break;
            case Opcodes.SMSG_AUTH_RESPONSE:
                sendData = HandleAuthResponse(opc);
                break;
            case Opcodes.SMSG_CHAR_ENUM:
                sendData = HandleCharEnum(opc);
                break;
            default:
                break;
        }
        if (sendData != null)
            socket.SendPacket(sendData);
    }

    public WorldPacket HandleAuthChallenge(WorldPacket recvData) {
        recvData.ReadUint32(); // 1
        byte[] serverSeed = recvData.ReadBytes(4); // uint32, just use an array for convenience
        byte[] seedOne = recvData.ReadBytes(16);
        byte[] seedTwo = recvData.ReadBytes(16);

        // Generate our own 4 byte seed
        BigNumber clientSeed = BigNumber.setRandBytes(4);

        int zero = 0;
        byte[] zeroInt = new byte[]{0, 0, 0, 0};
        byte[] digest = new byte[20];
        try {
            digest = new BigNumber(CryptoUtils.SHA1(
                    EncodingUtils.getAsciiBytes(G.Username.toUpperCase()),
                    zeroInt,
                    clientSeed.toByteArray(4),
                    serverSeed,
                    G.SessionKey.toByteArray(40)
            )).toByteArray(20);
        } catch (Exception e) {
            // Crud
            e.printStackTrace();
        }

        // Grossly over-estimated size
        WorldPacket response = new WorldPacket(Opcodes.CMSG_AUTH_SESSION, 80 + G.Username.length());
        response.WriteUint32(12340);
        response.WriteUint32(0); // Unk 2
        response.WriteCString(G.Username.toUpperCase());
        response.WriteUint32(0); // Unk 3
        response.WriteBytes(clientSeed.toByteArray(4));
        response.WriteUint32(0); // Unk 5
        response.WriteUint32(0); // Unk 6
        response.WriteUint32(0); // Unk 7
        response.WriteInt64(0L); // Unk 4
        response.WriteBytes(digest);
        response.WriteUint32(0); // Addons size

        // Our encryption key is the server's decryption key, and vice-versa.
        ARC4.SetupKey(G.SessionKey.toByteArray(40), new byte[]{
                (byte) 0xCC, (byte) 0x98, (byte) 0xAE, (byte) 0x04,
                (byte) 0xE8, (byte) 0x97, (byte) 0xEA, (byte) 0xCA,
                (byte) 0x12, (byte) 0xDD, (byte) 0xC0, (byte) 0x93,
                (byte) 0x42, (byte) 0x91, (byte) 0x53, (byte) 0x57
        }, new byte[]{
                (byte) 0xC2, (byte) 0xB3, (byte) 0x72, (byte) 0x3C,
                (byte) 0xC6, (byte) 0xAE, (byte) 0xD9, (byte) 0xB5,
                (byte) 0x34, (byte) 0x3C, (byte) 0x53, (byte) 0xEE,
                (byte) 0x2F, (byte) 0x43, (byte) 0x67, (byte) 0xCE
        });

        response.ForceCryptoInit = true;

        return response;
    }

    public WorldPacket HandleAuthResponse(WorldPacket recvData) {
        int errorCode = recvData.ReadUint8();
        if (errorCode != 0x0C)
            return null;

        return new WorldPacket(Opcodes.CMSG_CHAR_ENUM, 0);
    }

    public WorldPacket HandleCharEnum(WorldPacket recvData) {
        int charCount = recvData.ReadUint8();

        return null;
    }

    @Override
    public void SendPlayerLogin(String charName, byte[] guid) {

    }

    @Override
    public void SendMessageChat(int type, String... args) {

    }

    @Override
    public void SendChannelJoin(int channelId, String channelName, String channelPassword) {

    }

    @Override
    public void SendLeaveChannel(int channelId, String channelName) {

    }
}
