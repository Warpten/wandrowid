package org.warpten.wandrowid.network.battlenet;

import java.nio.ByteBuffer;

/**
 * Created by perquet on 11/08/14.
 */
public abstract class BattlenetClientPacket extends BattlenetPacket {
    public BattlenetClientPacket(BattlenetPacketHeader header, BattlenetBitStream stream) {
        super(header, stream);
    }

    @Override
    public final boolean Read(BattlenetSocket socket) {
        assert false : "Read not implemented for Client Packets!";
        return false; // Make compiler happy
    }

    @Override
    public final ByteBuffer ToByteBuffer() {
        try {
            BattlenetBitStream stream = new BattlenetBitStream(Stream.GetSize() + 6 + 4 + 1);
            stream.Write(Header.Opcode, 6);
            stream.Write(1, 1); // Has Channel (Let's just pretend there ALWAYS is a channel)
            stream.Write(Header.Channel, 4);
            stream.append(Stream);
            return ByteBuffer.wrap(stream.GetWriteBuffer());
        } catch (BattlenetBitStream.BitStreamException e) {
            e.printStackTrace();
            return null;
        }
    }
}
