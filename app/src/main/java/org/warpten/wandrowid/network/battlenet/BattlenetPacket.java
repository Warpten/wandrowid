package org.warpten.wandrowid.network.battlenet;

import org.warpten.wandrowid.network.GamePacket;

import java.nio.ByteBuffer;

public abstract class BattlenetPacket implements GamePacket {
    protected BattlenetPacketHeader Header;
    protected BattlenetBitStream Stream;

    public BattlenetPacket(BattlenetPacketHeader header, BattlenetBitStream stream) {
        Header = header;
        Stream = stream;
    }

    BattlenetPacketHeader GetHeader() { return Header; }

    public abstract boolean Write(BattlenetSocket socket);
    public abstract boolean Read(BattlenetSocket socket);
    public abstract String ToString();

    public abstract boolean Interprete(BattlenetSocket socket);

    public abstract ByteBuffer ToByteBuffer();
}
