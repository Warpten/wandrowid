package org.warpten.wandrowid.handlers.cataclysm;

import android.os.Bundle;
import android.util.Log;

import org.apache.http.util.EncodingUtils;
import org.warpten.wandrowid.G;
import org.warpten.wandrowid.ObjectGuid;
import org.warpten.wandrowid.PacketLogger;
import org.warpten.wandrowid.WorldSocket;
import org.warpten.wandrowid.crypto.ARC4;
import org.warpten.wandrowid.crypto.BigNumber;
import org.warpten.wandrowid.crypto.CryptoUtils;
import org.warpten.wandrowid.fragments.CharEnumStruct;
import org.warpten.wandrowid.fragments.ChatMessageType;
import org.warpten.wandrowid.handlers.Handlers;
import org.warpten.wandrowid.network.WorldPacket;

/**
 * Cataclysm opcode handlers.
 */
public class CataclysmHandlers extends Handlers {

    public CataclysmHandlers(WorldSocket socket) { super(socket); }

    public void CallHandler(WorldPacket recvData)
    {
        if ((recvData.GetOpcode() & Opcodes.COMPRESSED_OPCODE_MASK) != 0)
            recvData = recvData.Inflate();

        Log.i("WandroWid", "[WorldServer: S->C] Received " + recvData.GetOpcodeForLogging());
        PacketLogger.LogPacket(recvData, PacketLogger.SERVER_TO_CLIENT);

        WorldPacket sndData = null;

        switch (recvData.GetOpcode()) {
            case Opcodes.MSG_VERIFY_CONNECTIVITY:
                sndData = HandleMsgVerifyConnectivity(recvData);
                break;
            case Opcodes.SMSG_AUTH_CHALLENGE:
                sndData = HandleAuthChallenge(recvData);
                break;
            case Opcodes.SMSG_AUTH_RESPONSE:
                sndData = HandleAuthResponse(recvData);
                break;
            case Opcodes.SMSG_CHAR_ENUM:
                sndData = HandleCharEnum(recvData);
                break;
            case Opcodes.SMSG_MESSAGECHAT:
                HandleMessageChat(recvData);
                break;
            case Opcodes.SMSG_NAME_QUERY_RESPONSE:
                HandleNameQueryResponse(recvData);
                break;
            case Opcodes.SMSG_CHANNEL_NOTIFY:
                HandleChannelNotify(recvData);
                break;
            case Opcodes.SMSG_NOTIFICATION:
                HandleNotification(recvData);
                break;
            case Opcodes.SMSG_CHAT_PLAYER_NOT_FOUND:
                HandleChatPlayerNotFound(recvData);
            default:
                return;
        }

        if (sndData != null)
            socket.SendPacket(sndData);
    }

    public WorldPacket HandleMsgVerifyConnectivity(WorldPacket recvData) {
        String str = recvData.ReadString(45);
        if (!str.equals("RLD OF WARCRAFT CONNECTION - SERVER TO CLIENT"))
            return null;

        WorldPacket CTS = new WorldPacket(Opcodes.MSG_VERIFY_CONNECTIVITY, 46);
        CTS.WriteCString("RLD OF WARCRAFT CONNECTION - CLIENT TO SERVER");
        return CTS;
    }

    public WorldPacket HandleAuthChallenge(WorldPacket recvData)
    {
        byte[] seedOne = recvData.ReadBytes(16);
        byte[] seedTwo = recvData.ReadBytes(16);
        byte[] serverSeed = recvData.ReadBytes(4); // uint32, just use an array for convenience
        recvData.ReadUint8(); // 1

        // Generate our own 4 byte seed
        BigNumber clientSeed = BigNumber.setRandBytes(4);

        int zero = 0;
        byte[] zeroInt = new byte[] { 0, 0, 0, 0 };
        byte[] digest;
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
            digest = new byte[20];
        }

        WorldPacket response = new WorldPacket(Opcodes.CMSG_AUTH_SESSION, 58 + G.Username.length());
        response.WriteUint32(0); // Skipped
        response.WriteUint32(0); // Skipped
        response.WriteUint8((byte)0); // Skipped

        response.WriteBytes(digest, 10, 18, 12, 5);

        response.WriteInt64(0L); // Skipped

        response.WriteBytes(digest, 15, 9, 19, 4, 7, 16, 3);
        response.WriteUint16((short)15595); // Client build
        response.WriteUint8(digest[8]);

        response.WriteUint32(0); // Skipped
        response.WriteUint8((byte)0); // Skipped

        response.WriteBytes(digest, 17, 6, 0, 1, 11);
        response.WriteBytes(clientSeed.toByteArray(4)); // Client seed (technically u32)
        response.WriteUint8(digest[2]);

        response.WriteUint32(0); // Skipped
        response.WriteBytes(digest, 14, 13);

        response.WriteUint32(0); // Addon size (none)

        response.WriteBit(0); // isUsingIPv6
        response.WriteBits(G.Username.length(), 12);
        response.WriteString(G.Username.toUpperCase());

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

        //! This is a hack due to code design - encryption should begin
        //! after this opcode is sent, but old calls to SetupKeys would
        //! cause this opcode to be encrypted, which must not happen
        //! A generic solution in WorldSocket cannot be made either,
        //! because opcode IDs are different in WoTLK and Cataclysm.
        response.ForceCryptoInit = true;

        return response;
    }

    public WorldPacket HandleAuthResponse(WorldPacket recvData)
    {
        // Socket does not close, but now doesnt receive
        // Socket buffering code broken?

        // If success:
        // b0 (b0 ? b1 : null) b u32 u8 u32 u8 u32 u8 u8 (b0 ? u32 : null)
        // If fail
        // bit bit u8
        if (recvData.GetBodySize() == 2) { // bit, bit, u8 = 1byte + 1byte
            recvData.ReadBit();
            recvData.ReadBit();
            switch (recvData.ReadUint8()) {
                case 0x0C: // AUTH_OK, big problem here
                    break;
                default: // Inform GUI
                    break;
            }
            return null;
        }
        else {
            boolean isQueued = recvData.ReadBit();
            if (isQueued)
                recvData.ReadBit();

            recvData.ReadBit(); // Has account info

            recvData.ReadUint32(); // BillingTimeRemaining
            recvData.ReadUint8(); // Expansion
            recvData.ReadUint32();
            recvData.ReadUint8(); // Expansion
            recvData.ReadUint32(); // BillingTimeRested
            recvData.ReadUint8(); // BillingPlanFlags

            short errorCode = recvData.ReadUint8();
            if (isQueued)
                recvData.ReadUint32(); // Queue position

            switch (errorCode)
            {
                case 0x0C: // AUTH_OK
                    return new WorldPacket(Opcodes.CMSG_CHAR_ENUM, 0);
                default: // Catch-all
                    // TODO: Inform GUI
                    return null;
            }
        }
    }

    public WorldPacket HandleCharEnum(WorldPacket recvData)
    {
        int unkCounter = recvData.ReadBits(23);
        recvData.ReadBit(); // Unk, skipped
        int characterCount = recvData.ReadBits(17);

        G.CharacterData = new CharEnumStruct(characterCount);

        byte[][] guildGuids = new byte[characterCount][8];
        int[] namesLength = new int[characterCount];

        for (int c = 0; c < characterCount; ++c) {
            G.CharacterData.CharGuids[c][3] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][1] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][7] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][2] = (byte) (recvData.ReadBit() ? 1 : 0);
            namesLength[c] = recvData.ReadBits(7);
            G.CharacterData.CharGuids[c][4] = (byte) (recvData.ReadBit() ? 1 : 0);
            G.CharacterData.CharGuids[c][7] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][3] = (byte) (recvData.ReadBit() ? 1 : 0);
            G.CharacterData.CharGuids[c][5] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][6] = (byte) (recvData.ReadBit() ? 1 : 0);
            G.CharacterData.CharGuids[c][1] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][5] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][4] = (byte) (recvData.ReadBit() ? 1 : 0);
            recvData.ReadBit(); // FirstLogin, skipped
            G.CharacterData.CharGuids[c][0] = (byte) (recvData.ReadBit() ? 1 : 0);
            G.CharacterData.CharGuids[c][2] = (byte) (recvData.ReadBit() ? 1 : 0);
            G.CharacterData.CharGuids[c][6] = (byte) (recvData.ReadBit() ? 1 : 0);
            guildGuids[c][0] = (byte) (recvData.ReadBit() ? 1 : 0);
        }

        for (int c = 0; c < characterCount; ++c) {
            G.CharacterData.CharClasses[c] = recvData.ReadUint8();

            recvData.ReadBytes(19 * 9); // Skip inventory data (19 * (i8 + i32 + i32))
            recvData.ReadBytes(4 * 9); // Skip bag data (9 * (i8 + u32 + u32))

            recvData.ReadUint32(); // Pet family
            recvData.ReadXORByte(guildGuids[c], 2);
            recvData.ReadBytes(2); // List Order + Hair Style (2x u8)
            recvData.ReadXORByte(guildGuids[c], 3);
            recvData.ReadUint32(); // Pet Display ID
            recvData.ReadUint32(); // CharacterFlag
            recvData.ReadUint8(); // Hair Color

            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 4);
            int mapId = recvData.ReadInt32();
            recvData.ReadXORByte(guildGuids[c], 5);
            float z = recvData.ReadFloat();
            recvData.ReadXORByte(guildGuids[c], 6);

            recvData.ReadUint32(); // Pet Level

            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 3);

            float y = recvData.ReadFloat();

            recvData.ReadBytes(5); // CustomizationFlag, Facial Hair (u32 + u8)
            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 7);

            G.CharacterData.CharGenders[c] = recvData.ReadUint8();
            G.CharacterData.CharNames[c] = recvData.ReadString(namesLength[c]);

            recvData.ReadUint8(); // Face

            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 0);
            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 2);
            recvData.ReadXORByte(guildGuids[c], 1);
            recvData.ReadXORByte(guildGuids[c], 7);
            float x = recvData.ReadFloat();

            recvData.ReadUint8(); // Skin

            G.CharacterData.CharRaces[c] = recvData.ReadUint8();
            G.CharacterData.CharLevels[c] = recvData.ReadInt8();

            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 6);
            recvData.ReadXORByte(guildGuids[c], 4);
            recvData.ReadXORByte(guildGuids[c], 0);
            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 5);
            recvData.ReadXORByte(G.CharacterData.CharGuids[c], 1);
            int zoneId = recvData.ReadInt32();

            G.CharacterData.InGuild[c] = new ObjectGuid(guildGuids[c]).GUID != 0;
        }

        // Not read everything, the remains are garbage at this point (unkCounter * (u8 + u32))
        socket.DisplayCharEnum();

        return null;
    }

    private void HandleMessageChat(WorldPacket recvData)
    {
        short chatMessageType = recvData.ReadUint8();
        int language = recvData.ReadInt32();

        if (language > 40) // Ignore addon messages
            return;

        ObjectGuid senderGuid = new ObjectGuid(recvData.ReadInt64());
        String senderName = null;
        ObjectGuid receiverGuid = null;
        String receiverName = null;
        String channelName = null;

        recvData.ReadInt32(); // Some flags, pointless

        switch (chatMessageType)
        {
            // Don't handle these
            case ChatMessageType.MonsterSay:
            case ChatMessageType.MonsterEmote:
            case ChatMessageType.MonsterParty:
            case ChatMessageType.MonsterWhisper:
            case ChatMessageType.MonsterYell:
            case ChatMessageType.RaidBossEmote:
            case ChatMessageType.RaidBossWhisper:
            case ChatMessageType.BattleNet: // TODO: implement this one once battle net socket is done
            case ChatMessageType.WhisperForeign:
            case ChatMessageType.BattlegroundAlliance:
            case ChatMessageType.BattlegroundHorde:
            case ChatMessageType.BattlegroundNeutral:
            case ChatMessageType.Achievement:
                return;
            case ChatMessageType.Channel:
                channelName = recvData.ReadCString();
                // no break
            default:
                receiverGuid = new ObjectGuid(recvData.ReadInt64());
                break;
        }

        recvData.ReadInt32(); // Message length, ignore, its a c string anyway
        String message = recvData.ReadCString();
        recvData.ReadUint8(); // Chat tag ? (<GM>, <DEV> i guess, unused)
        // Useless data for us at this point, so just ignore them

        // Add to cache iff name not in cache, else instant dispatch
        Bundle bundle = new Bundle();
        bundle.putLong("receiverGuid", receiverGuid.GUID);
        bundle.putLong("senderGuid", senderGuid.GUID);
        bundle.putString("receiverName", receiverName);
        bundle.putString("senderName", senderName);
        bundle.putString("channelName", channelName);
        bundle.putString("message", message);
        bundle.putLong("timestamp", System.currentTimeMillis());
        bundle.putInt("chatMessageType", chatMessageType);

        if (receiverGuid.GUID != 0L && !G.CharacterCache.containsKey(receiverGuid.GUID))
            SendNameQuery(receiverGuid);

        if (senderGuid.GUID != 0L && !G.CharacterCache.containsKey(senderGuid.GUID))
            SendNameQuery(senderGuid);

        G.EnqueueChatMessage(bundle);        // Queue this message, waiting for char names
        G.TryFlushReadyMessages();           // Try adding new messages
    }

    public void HandleNameQueryResponse(WorldPacket recvData) {
        ObjectGuid guid = recvData.ReadPackedGuid();
        if (recvData.ReadInt8() == 1) // Not found
            return;

        String charName = recvData.ReadCString();
        // Remains useless for us

        G.CharacterCache.put(guid.GUID, charName); // Store name
        G.TryFlushReadyMessages();                 // Try adding new messages
    }

    public void SendNameQuery(ObjectGuid guid)
    {
        if (guid.GUID == 0L)
            return;

        WorldPacket data = new WorldPacket(Opcodes.CMSG_NAME_QUERY, 8);
        data.WriteBytes(guid.asByteArray());
        socket.SendPacket(data);
    }

    public void SendPlayerLogin(String charName, byte[] guid)
    {
        // 9 at max, if bit is not set, byte is not written (stupid cataclysm)
        WorldPacket playerLogin = new WorldPacket(Opcodes.CMSG_PLAYER_LOGIN, 9);
        playerLogin.WriteBit(guid[2]);
        playerLogin.WriteBit(guid[3]);
        playerLogin.WriteBit(guid[0]);
        playerLogin.WriteBit(guid[6]);
        playerLogin.WriteBit(guid[4]);
        playerLogin.WriteBit(guid[5]);
        playerLogin.WriteBit(guid[1]);
        playerLogin.WriteBit(guid[7]);

        playerLogin.WriteByteSeq(guid[2]);
        playerLogin.WriteByteSeq(guid[7]);
        playerLogin.WriteByteSeq(guid[0]);
        playerLogin.WriteByteSeq(guid[3]);
        playerLogin.WriteByteSeq(guid[5]);
        playerLogin.WriteByteSeq(guid[6]);
        playerLogin.WriteByteSeq(guid[1]);
        playerLogin.WriteByteSeq(guid[4]);

        G.CharacterCache.put(new ObjectGuid(guid).GUID, charName); // Cache ourselves
        socket.SendPacket(playerLogin);

        // TODO: move autojoin to settings and load
        SendChannelJoin(0, "world", null); // Custom channels dont have an id( its dbc id)
    }

    public void SendChannelJoin(int channelId, String channelName, String channelPassword)
    {
        int opcodeSize = 15 + channelName.length(); // Overestimated
        if (channelPassword != null)
            opcodeSize += channelPassword.length();
        WorldPacket packet = new WorldPacket(Opcodes.CMSG_JOIN_CHANNEL, opcodeSize);
        packet.WriteInt32(channelId);
        packet.WriteBit(0); // Ignored by TC
        packet.WriteBit(0); // Ignored by TC
        packet.WriteBits(channelName.length(), 8);
        packet.WriteBits(channelPassword != null ? channelPassword.length() : 0, 8);
        packet.WriteString(channelName);
        packet.WriteString(channelPassword != null ? channelPassword : "");
        socket.SendPacket(packet);
    }

    public void HandleChannelNotify(WorldPacket recvData)
    {
        int notifyType = recvData.ReadUint8();
        String channelName = recvData.ReadCString();
        if (notifyType == 2) // YouJoined
            socket.OnJoinedChannel(channelName);
    }

    public void HandleNotification(WorldPacket recvData)
    {
        String message = recvData.ReadCString();
        // TODO: inform gui somehow
    }

    public void HandleChatPlayerNotFound(WorldPacket recvData)
    {
        String playerName = recvData.ReadCString();
        // TODO: Inform GUI somehow
    }

    public void SendLeaveChannel(int channelId, String channelName)
    {
        WorldPacket data = new WorldPacket(Opcodes.CMSG_LEAVE_CHANNEL, 5 + channelName.length());
        data.WriteInt32(channelId);
        data.WriteBits(channelName.length(), 8);
        data.WriteString(channelName);
        socket.SendPacket(data);
    }

    /*
     * type: ChatMessageType.Channel:
     *  -> arg0: channelName, arg1: message
     */
    public void SendMessageChat(int type, String... arg) {
        switch (type) {
            case ChatMessageType.Channel:
            {
                WorldPacket packet = new WorldPacket(Opcodes.CMSG_MESSAGECHAT_CHANNEL, 15 + arg[0].length() + arg[1].length());
                packet.WriteInt32(G.GetLanguageForActiveCharacter());
                packet.WriteBits(arg[0].length(), 10);
                packet.WriteBits(arg[1].length(), 9);
                packet.WriteString(arg[1]);
                packet.WriteString(arg[0]);
                socket.SendPacket(packet);
                break;
            }
            case ChatMessageType.Guild:
            {
                WorldPacket packet = new WorldPacket(Opcodes.CMSG_MESSAGECHAT_GUILD, 8 + arg[0].length());
                packet.WriteInt32(G.GetLanguageForActiveCharacter());
                packet.WriteBits(arg[0].length(), 9);
                packet.WriteString(arg[0]);
                socket.SendPacket(packet);
                break;
            }
            case ChatMessageType.Whisper:
            {
                if (arg[0].length() == 0 || arg[1].length() == 0)
                    return;
                WorldPacket packet = new WorldPacket(Opcodes.CMSG_MESSAGECHAT_WHISPER, 15 + arg[0].length() + arg[1].length());
                packet.WriteInt32(G.GetLanguageForActiveCharacter());
                packet.WriteBits(arg[0].length(), 10);
                packet.WriteBits(arg[1].length(), 9);
                packet.WriteString(arg[0]);
                packet.WriteString(arg[1]);
                socket.SendPacket(packet);
                break;
            }
            case ChatMessageType.Say:
            {
                if (arg[0].length() == 0)
                    return;
                WorldPacket packet = new WorldPacket(Opcodes.CMSG_MESSAGECHAT_SAY, 10 + arg[0].length());
                packet.WriteInt32(G.GetLanguageForActiveCharacter());
                packet.WriteBits(arg[0].length(), 9);
                packet.WriteString(arg[0]);
                socket.SendPacket(packet);
                break;
            }
            default:
                break; // NYI
        }
    }
}
