package org.warpten.wandrowid.network.battlenet;

import java.nio.ByteBuffer;

/**
 * Created by perquet on 11/08/14.
 */
public abstract class BattlenetServerPacket extends BattlenetPacket {
    public BattlenetServerPacket(BattlenetPacketHeader header) {
        super(null, null); // fixme
    }

    public final boolean Write(BattlenetSocket socket) {
        assert false : "Write not implemented for Server Packets.";
        return false; // Make compiler happy
    }

    @Override
    public final ByteBuffer ToByteBuffer() {
        assert false : "ToByteBuffer not implemented for Server Packets.";
        return null;
    }

    @Override
    public final boolean Interprete(BattlenetSocket socket) {
        return false;
    }

    public final byte[] GetData() { return Stream.GetReadBuffer(); }
    public final int GetSize() { return Stream.GetSize(); }

    @Override
    public String ToString() {
        return null;
    }
}
