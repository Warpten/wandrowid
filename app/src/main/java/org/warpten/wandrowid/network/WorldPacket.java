package org.warpten.wandrowid.network;

import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;

import org.warpten.wandrowid.ObjectGuid;
import org.warpten.wandrowid.crypto.ARC4;
import org.warpten.wandrowid.handlers.cataclysm.Opcodes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WorldPacket implements GamePacket {
    private ByteBuffer data;
    private short opcode;
    private int _bitPos = 8;
    private int _curBitValue = 0;
    public boolean ForceCryptoInit = false;

    public WorldPacket(int opcode, int size) {
        this.opcode = (short)(opcode & 0xFFFF);
        data = ByteBuffer.allocate(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    public WorldPacket(int opcode, byte[] byteData)
    {
        this.opcode = (short)(opcode & 0xFFFF);
        data = ByteBuffer.wrap(byteData);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    public WorldPacket(int opcode)
    {
        this.opcode = (short)(opcode & 0xFFFF);
        data = ByteBuffer.allocate(0x200);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int GetOpcode() { return opcode; }

    /**
     * NOTE: This function can only be used if you are DONE with writing your packet!
     */
    public final ByteBuffer ToByteBuffer()
    {
        FlushBits();
        return ARC4.EncryptPacket(GetOpcode(), data, ForceCryptoInit);
    }

    public int GetBodySize() { return data.limit(); }
    public byte[] GetBody() {
        int length = data.position() == 0 ? data.limit() : data.position();
        byte[] body = new byte[length];
        ByteBuffer copy = data.duplicate();
        copy.position(0);
        copy.limit(length);
        copy.get(body);
        return body;
    }

    // Reads V2
    public String ReadString(int size) {
        byte[] stringBytes = new byte[size];
        data.get(stringBytes);
        return new String(stringBytes);
    }

    public long ReadInt64() { return data.getLong(); }

    public int ReadInt32() { return data.getInt(); }
    public long ReadUint32() { return (long)(data.getInt() & 0xFFFFFFFF); }

    public short ReadInt16() { return data.getShort(); }
    public int ReadUint16() { return (int)(data.getShort() & 0xFFFF); }

    public byte ReadInt8() { return data.get(); }
    public short ReadUint8() { return (short)(data.get() & 0xFF); }

    public byte[] ReadBytes(int count) {
        byte[] ret = new byte[count];
        for (int i = 0; i < count; ++i)
            ret[i] = ReadInt8();
        return ret;
    }

    public float ReadFloat() { return data.getFloat(); }

    public boolean ReadBit() {
        ++_bitPos;
        if (_bitPos > 7) {
            _bitPos = 0;
            _curBitValue = ReadUint8();
        }

        return ((_curBitValue >> (7 - _bitPos)) & 1) != 0;
    }

    public int ReadBits(int bitCount) {
        int value = 0;
        for (int i = bitCount - 1; i >= 0; --i)
            if (ReadBit())
                value |= (1 << i);
        return value;
    }

    public String ReadCString()
    {
        int position = data.position();
        while (data.get() != 0);
        byte[] stringBytes = new byte[data.position() - position - 1];
        data.position(position);
        data.get(stringBytes);
        data.get(); // Skip terminary zero
        return new String(stringBytes);
    }

    public ObjectGuid ReadPackedGuid()
    {
        long guid = 0;

        short guidMark = ReadUint8();
        for (int i = 0; i < 8; ++i)
            if ((guidMark & (guidMark << i)) != 0)
                guid |= (long)ReadUint8() << (i * 8);

        return new ObjectGuid(guid);
    }

    // WPP-Like
    public byte[] StartBitStream(int... offsets)
    {
        byte[] ret = new byte[offsets.length];
        for (int offset : offsets)
            ret[offset] = (byte)(ReadBit() ? 1 : 0);
        return ret;
    }

    // WPP-Like
    public void StartBitStream(byte[] out, int...offsets)
    {
        for (int offset : offsets)
            out[offset] = (byte)(ReadBit() ? 1 : 0);
    }

    public byte[] ParseBitStream(byte[] stream, int... offsets)
    {
        byte[] tempBytes = new byte[offsets.length];
        int i = 0;

        for (int value : offsets)
        {
            if (stream[value] != 0)
                stream[value] ^= ReadUint8();

            tempBytes[i++] = stream[value];
        }

        return tempBytes;
    }

    public byte ReadXORByte(byte[] stream, int value)
    {
        if (stream[value] != 0)
            return stream[value] ^= ReadUint8();
        return 0;
    }

    public void ReadXORBytes(byte[] stream, int... values)
    {
        for (int value : values)
            if (stream[value] != 0)
                stream[value] ^= ReadUint8();
    }

    public void WriteBits(int value, int bitCount) {
        for (int i = bitCount - 1; i >= 0; --i)
            WriteBit((value >> i) & 1);
    }

    public boolean WriteBit(int bit) {
        --_bitPos;
        if (bit != 0)
            _curBitValue |= (1 << _bitPos);

        if (_bitPos == 0) {
            _bitPos = 8;
            data.put((byte)(_curBitValue & 0xFF));
            _curBitValue = 0;
        }

        return bit != 0;
    }

    public void FlushBits() {
        if (_bitPos == 8)
            return;

        data.put((byte)(_curBitValue & 0xFF));
        _curBitValue = 0;
        _bitPos = 8;
    }

    public void WriteByteSeq(byte b) {
        if (b != 0)
            data.put((byte)(b ^ 1));
    }

    public void WriteInt8(byte value)   { FlushBits(); data.put(value); }
    public void WriteUint8(short value) { FlushBits(); data.put((byte)(value & 0xFF)); }

    public void WriteInt16(short value) { FlushBits(); data.putShort(value); }
    public void WriteUint16(int value)  { FlushBits(); data.putShort((short)(value & 0xFFFF)); }

    public void WriteInt32(int value)   { FlushBits(); data.putInt(value); }
    public void WriteUint32(long value) { FlushBits(); data.putInt((int)(value & 0xFFFFFFFF)); }

    public void WriteFloat(float value) { FlushBits(); data.putFloat(value); }

    public void WriteInt64(long value) { FlushBits(); data.putLong(value); }

    public void WriteString(String str)
    {
        try {
            FlushBits();
            data.put(str.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void WriteCString(String str)
    {
        try {
            FlushBits();
            data.put(str.getBytes("UTF-8"));
            data.put((byte)0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void WriteBytes(byte[] bytes, int... offsets) {
        FlushBits();
        for (int offset : offsets)
            data.put(bytes[offset]);
    }

    public void WriteBytes(byte[] bytes)
    {
        FlushBits();
        for (int i = 0; i < bytes.length; ++i)
            data.put(bytes[i]);
    }

    public WorldPacket Inflate()
    {
        // Only designed for 4.3.4, 3.3.5 has more fuckery
        try {
            int realOpcode = (short)(opcode & (short)(~Opcodes.COMPRESSED_OPCODE_MASK));
            int decompressedCount = (int)(ReadUint32() & 0xFFFFFFFF);
            byte[] compressedData = ReadBytes(data.limit() - 4);

            byte[] buffer = new byte[decompressedCount];
            Inflater in = new Inflater();
            in.setInput(compressedData); // inputBuffer
            in.setNextInIndex(0); // nextIn
            in.setAvailIn(compressedData.length); // AvailableBytesCount
            in.setOutput(buffer);
            in.setNextOutIndex(0); // nextOut
            in.setAvailOut(decompressedCount);
            in.inflate(JZlib.Z_NO_FLUSH);
            in.sync();
            in.inflate(JZlib.Z_FINISH);
            in.inflateEnd();

            return new WorldPacket(realOpcode, buffer);
        } catch (Exception e) {
            e.printStackTrace();
            return this; // This is bad
        }
    }

    public String GetOpcodeForLogging()
    {
        return String.format("0x%04X", (short)(GetOpcode() & (short)(~Opcodes.COMPRESSED_OPCODE_MASK)));
    }
}
