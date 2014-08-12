package org.warpten.wandrowid.network.battlenet;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by perquet on 08/08/14.
 */
public class BattlenetBitStream {
    private ByteBuffer _buffer;
    private int _readPos;
    private int _writePos;
    private int _numBits;

    public static final int MaxSize = 0x4000;
    /*
     * Constructs a new instance of BattlenetBitStream
     * @params int length Length in bytes
     */
    public BattlenetBitStream(int length) {
        _buffer = ByteBuffer.allocate(length);
        _numBits = length * 8;
        _readPos = 0;
        _writePos = 0;
    }

    public BattlenetBitStream() {
        this(0x1000);
    }

    public void AlignToNextByte()
    {
        _readPos = (_readPos + 7) & ~7;
        _writePos = (_writePos + 7) & ~7;
    }

    public String ReadString(int bitCount) throws BitStreamException { return ReadString(bitCount, 0); }
    public String ReadString(int bitCount, int baseLength) throws BitStreamException {
        int length = (int)(baseLength + Read(bitCount));
        AlignToNextByte();
        byte[] stringBytes = new byte[length];
        _buffer.position(_readPos >> 3);
        _buffer.get(stringBytes);
        _readPos += length * 8;

        return new String(stringBytes);
    }

    public byte[] ReadBytes(int count) throws BitStreamException {
        AlignToNextByte();
        if (_readPos + count * 8 > _buffer.capacity())
            throw new BitStreamException(true, count * 8, _readPos, _numBits);

        byte[] retVal = new byte[count];
        _buffer.position(_readPos >> 3);
        _buffer.get(retVal);

        _readPos += count * 8;

        return retVal;
    }

    public long Read(int bitCount) throws BitStreamException {
        if (_readPos + bitCount >= _numBits)
            throw new BitStreamException(true, bitCount, _readPos, _numBits);

        long retVal = 0;
        while (bitCount != 0) {
            int bitPos = _readPos & 7;
            int bitsLeftInByte = 8 - bitPos;
            if (bitsLeftInByte >= bitCount)
                bitsLeftInByte = bitCount;

            bitCount -= bitsLeftInByte;
            retVal |= (_buffer.get(_readPos >> 3) >> bitPos & ((1 << bitsLeftInByte) - 1)) << bitCount;
            _readPos += bitsLeftInByte;
        }

        return retVal;
    }

    public float ReadFloat() throws BitStreamException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt((int) Read(32));
        return buffer.getFloat(0);
    }

    public boolean ReadBool() throws BitStreamException { return Read(1) != 0; }

    public String ReadFourCC() throws BitStreamException
    {
        int fcc = (int) Read(32);
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0, fcc);
        buffer.order(ByteOrder.BIG_ENDIAN);
        fcc = buffer.getInt(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int length = 4;
        while ((fcc & 0xFF) != 0) {
            fcc >>= 8;
            --length;
        }

        byte[] stringBytes = new byte[length];
        buffer.putInt(0, fcc);
        buffer.get(stringBytes);
        return new String(stringBytes);
    }

    public void WriteString(String input, int bitCount) throws BitStreamException {
        WriteString(input, bitCount, 0);
    }
    public void WriteString(String input, int bitCount, int baseLength) throws BitStreamException {
        Write(input.length() + baseLength, bitCount);
        try {
            WriteBytes(input.getBytes("UTF-8"), input.length());
        } catch (UnsupportedEncodingException e) {
            WriteBytes(new byte[bitCount], bitCount);
        }
    }

    public void WriteFloat(float value) throws BitStreamException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(0, value);
        Write(buffer.getInt(0), 32);
    }

    public void WriteFourCC(String fcc) {
        try {
            int length = fcc.length();
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(fcc.getBytes("UTF-8"));
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.flip();
            int fcci = buffer.getInt(0);
            while (length++ < 4)
                fcci >>= 8;

            Write(fcci, 32);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (BitStreamException e) {
            e.printStackTrace();
        }
    }

    public void append(BattlenetBitStream other) throws BitStreamException {
        WriteBytes(other.GetWriteBuffer(), other.GetSize());
    }

    public void Write(int value, int bitCount) throws BitStreamException {
        if (_writePos + bitCount >= 8 * 0x4000) // MaxSize
            throw new BitStreamException(false, bitCount, _writePos, 0x4000 * 8);

        while (bitCount != 0) {
            int bitPos = (_writePos & 7);
            int bitsLeftInByte = 8 - bitPos;
            if (bitsLeftInByte >= bitCount)
                bitsLeftInByte = bitCount;

            byte firstHalf = (byte)(~(((byte)(1 << bitsLeftInByte) - 1) << bitPos));
            byte secondHalf = (byte)((((byte)(1 << bitsLeftInByte) - 1) & (byte)(value >> bitCount)) << bitPos);

            _buffer.position(_writePos >> 3);
            if (_buffer.capacity() > (_writePos >> 3))
                _buffer.put((byte)((_buffer.get(_writePos >> 3) & firstHalf) | secondHalf));
            else
                _buffer.put(secondHalf);

            _writePos += bitsLeftInByte;
        }
    }

    public void WriteBytes(byte[] data, int count) throws BitStreamException {
        AlignToNextByte();
        if (count == 0 || data == null)
            return;

        if ((_writePos >> 3) + count > MaxSize)
            throw new BitStreamException(false, count * 8, _writePos, MaxSize * 8);

        // _buffer.resize(_buffer.size() + count);
        _buffer.position(_writePos >> 3);
        _buffer.put(data, 0, count);
        _writePos += count * 8;
    }

    public int GetSize() { return _buffer.capacity(); }
    public void FinishReading() { _readPos = _numBits; }
    public byte[] GetReadBuffer() {
        byte[] ret = new byte[_readPos >> 3];
        _buffer.position(0);
        _buffer.get(ret);
        return ret;
    }
    public byte[] GetWriteBuffer() {
        byte[] ret = new byte[_writePos >> 3];
        _buffer.position(0);
        _buffer.get(ret);
        return ret;
    }

    public boolean IsRead() { return _readPos >= _numBits; }

    public static final class BitStreamException extends Exception {
        public BitStreamException(boolean isReadOperation, int operationSize, int position, int capacity)
        {
            super(String.format("Attempt to %s more bits (%u) %s stream than %s (%u)",
                    isReadOperation ? "read" : "write",
                    operationSize + position,
                    isReadOperation ? "from" : "to",
                    isReadOperation ? "exists" : "allowed",
                    capacity));
        }
    }
}
