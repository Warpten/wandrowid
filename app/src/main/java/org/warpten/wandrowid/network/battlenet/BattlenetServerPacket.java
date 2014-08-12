package org.warpten.wandrowid.network.battlenet;

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

    public byte[] GetData() { return Stream.GetWriteBuffer(); }
    public int GetSize() { return Stream.GetSize(); }
}
