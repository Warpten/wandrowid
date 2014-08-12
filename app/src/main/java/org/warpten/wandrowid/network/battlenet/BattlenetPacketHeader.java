package org.warpten.wandrowid.network.battlenet;

/**
 * Created by perquet on 11/08/14.
 */
public class BattlenetPacketHeader {
    public int Opcode;
    public int Channel;

    public static final int CHANNEL_AUTHENTICATION = 0;
    public static final int CHANNEL_CONNECTION     = 1;
    public static final int CHANNEL_WOW            = 2;
    public static final int CHANNEL_FRIEND         = 3;
    public static final int CHANNEL_PRESENCE       = 4;
    public static final int CHANNEL_CHAT           = 5;
    public static final int CHANNEL_SUPPORT        = 7;
    public static final int CHANNEL_ACHIEVEMENT    = 8;
    public static final int CHANNEL_CACHE          = 11;
    public static final int CHANNEL_PROFILE        = 12;

    // AuthOpcodes
    public static final int CMSG_AUTH_CHALLENGE         = 0x0;
    public static final int CMSG_AUTH_RECONNECT         = 0x1;
    public static final int CMSG_AUTH_PROOF_RESPONSE    = 0x2;
    public static final int SMSG_AUTH_COMPLETE          = 0x0;
    public static final int SMSG_AUTH_RESUME            = 0x1;
    public static final int SMSG_AUTH_PROOF_REQUEST     = 0x2;

    // ConnectionOpcodes
    public static final int CMSG_PING               = 0x0;
    public static final int CMSG_ENABLE_ENCRYPTION  = 0x5;
    public static final int CMSG_DISCONNECT         = 0x6;
    public static final int CMSG_INVALID_PACKET     = 0x9;
    public static final int SMSG_PONG               = 0x0;

    // WowOpcodes
    public static final int CMSG_REALM_UPDATE_SUBSCRIBE     = 0x0;
    public static final int CMSG_REALM_UPDATE_UNSUBSCRIBE   = 0x1;
    public static final int CMSG_JOIN_REQUEST               = 0x8;

    public static final int SMSG_CHARACTER_COUNTS           = 0x0;
    public static final int SMSG_REALM_UPDATE               = 0x2;
    public static final int SMSG_REALM_UPDATE_END           = 0x3;
    public static final int SMSG_JOIN_RESULT                = 0x8;

    public BattlenetPacketHeader(int opcode, int channel) {
        Opcode = opcode;
        Channel = channel;
    }

    public BattlenetPacketHeader() {
        Opcode = 0;
        Channel = CHANNEL_AUTHENTICATION;
    }

    public boolean Equals(BattlenetPacketHeader other) {
        return Opcode == other.Opcode && Channel == other.Channel;
    }

    public boolean Equals(int opcode, int channel)
    {
        return Opcode == opcode && channel == channel;
    }

    public String ToString()
    {
        return String.format("Battlenet::PacketHeader Opcode %04X, Channel %u", Opcode, Channel);
    }
}
