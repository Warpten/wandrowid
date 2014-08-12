package org.warpten.wandrowid.network;

import java.nio.ByteBuffer;

// Shallow interface, serves no exact purpose but reduce code
public interface GamePacket {
    public ByteBuffer ToByteBuffer();
}
