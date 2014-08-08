package org.warpten.wandrowid.handlers.wotlk;

import android.os.Bundle;
import android.util.Log;

import org.apache.http.util.EncodingUtils;
import org.warpten.wandrowid.G;
import org.warpten.wandrowid.ObjectGuid;
import org.warpten.wandrowid.crypto.ARC4;
import org.warpten.wandrowid.crypto.BigNumber;
import org.warpten.wandrowid.crypto.CryptoUtils;
import org.warpten.wandrowid.fragments.CharEnumStruct;
import org.warpten.wandrowid.fragments.ChatMessageType;
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
            case Opcodes.SMSG_MESSAGECHAT:
                HandleMessageChat(opc);
                break;
            case Opcodes.SMSG_NAME_QUERY_RESPONSE:
                HandleNameQueryResponse(opc);
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

        G.CharacterData = new CharEnumStruct(charCount);

        for (int i = 0; i < charCount; ++i) {
            G.CharacterData.CharGuids[i] = recvData.ReadBytes(8);
            G.CharacterData.CharNames[i] = recvData.ReadCString();
            G.CharacterData.CharRaces[i] = recvData.ReadUint8();
            G.CharacterData.CharClasses[i] = recvData.ReadUint8();
            G.CharacterData.CharGenders[i] = recvData.ReadUint8();

            recvData.ReadBytes(5); // Skin, Face, Hair Style, Hair Color, Facial Hair

            G.CharacterData.CharLevels[i] = recvData.ReadUint8();
            int zoneId = recvData.ReadInt32();
            int mapId = recvData.ReadInt32();

            float X = recvData.ReadFloat();
            float Y = recvData.ReadFloat();
            float Z = recvData.ReadFloat();
            G.CharacterData.InGuild[i] = recvData.ReadInt32() != 0;
            recvData.ReadInt32(); // Character Flags
            recvData.ReadInt32(); // Customization Flags

            recvData.ReadBytes(1 + 4 + 4 + 4); // FirstLogin, Pet Display Id, Pet Level, Pet Family
            recvData.ReadBytes(19 * (4 + 1 + 4)); // Inventory data
            recvData.ReadBytes(4 * (4 + 1 + 4)); // Bag data
        }

        socket.DisplayCharEnum();
        return null;
    }

    public void HandleMessageChat(WorldPacket recvData)
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
        // Remains are useless for us

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


    @Override
    public void SendPlayerLogin(String charName, byte[] guid) {
        WorldPacket data = new WorldPacket(Opcodes.CMSG_PLAYER_LOGIN, 8);
        data.WriteBytes(guid);
        socket.SendPacket(data);
    }

    @Override
    public void SendMessageChat(int type, String... args) {
        int opcodeSize = 4 + 4;
        int msgIndex = 0;
        switch (type)
        {
            case ChatMessageType.Channel:
            case ChatMessageType.Whisper:
                opcodeSize += args[0].length();
                msgIndex = 1;
                // no break
            case ChatMessageType.Guild:
            case ChatMessageType.Say:
            {
                opcodeSize += args[1].length();

                WorldPacket packet = new WorldPacket(Opcodes.CMSG_MESSAGECHAT, opcodeSize);
                packet.WriteInt32(type);
                packet.WriteInt32(G.GetLanguageForActiveCharacter());
                if (type == ChatMessageType.Channel ||
                    type == ChatMessageType.Whisper)
                    packet.WriteCString(args[0]);
                packet.WriteCString(args[msgIndex]);
                socket.SendPacket(packet);
                break;
            }
        }
    }

    @Override
    public void SendChannelJoin(int channelId, String channelName, String channelPassword) {
        int opcodeSize = 6 + channelName.length();
        if (channelPassword != null)
            opcodeSize += channelPassword.length();

        WorldPacket packet = new WorldPacket(Opcodes.CMSG_JOIN_CHANNEL, opcodeSize);
        packet.WriteInt32(channelId);
        packet.WriteUint8((short) 0); // Has Voice
        packet.WriteUint8((short) 0); // Joined By Zone Update
        packet.WriteCString(channelName);
        packet.WriteCString(channelPassword != null ? channelPassword : "");
        socket.SendPacket(packet);
    }

    @Override
    public void SendLeaveChannel(int channelId, String channelName) {
        WorldPacket data = new WorldPacket(Opcodes.CMSG_LEAVE_CHANNEL, 4 + channelName.length());
        data.WriteInt32(channelId);
        data.WriteCString(channelName);
        socket.SendPacket(data);
    }
}
