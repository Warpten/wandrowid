package org.warpten.wandrowid.network;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.util.EncodingUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class AuthPacket {
    private ByteBuffer data;

    public int GetOpcode() { return data.get(0); }
    public int GetDataSize() { return data.limit(); }

    /*
     * Constructs a new AuthPacket.
     */
    public AuthPacket(AuthOpcodes opcode, int size)
    {
        data = ByteBuffer.allocate(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    // This is for server opcodes
    public AuthPacket(ByteBuffer buffer)
    {
        data = buffer.duplicate();
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.flip();
        data.get();
    }

    public AuthPacket(byte[] bdata)
    {
        data = ByteBuffer.wrap(bdata);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.get(); // Advance pointer to actual data start
    }

    public void WriteUint32(int number)   { data.putInt(number); }
    public void WriteUint16(short number) { data.putShort(number); }
    public void WriteFloat(float number)  { data.putFloat(number);}
    public void WriteByte(byte number)    { data.put(number); }
    public void WriteBytes(byte[] ip)     { data.put(ip); }

    public void WriteAsciiCString(String str)
    {
        try {
            data.put(EncodingUtils.getAsciiBytes(str));
            data.put((byte)0);
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }
    }

    public void WriteAsciiString(String str)
    {
        try {
            data.put(EncodingUtils.getAsciiBytes(str));
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }
    }

    public int GetWritePos() { return data.position(); }
    public void WriteByte(int writePos, byte value)
    {
        if (writePos <= data.capacity())
            data.put(writePos, value);
    }

    public void WriteIP()
    {
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.isLoopbackAddress())
                        continue;

                    // 3.3.5 doesn't want IPv6
                    if (!InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()))
                        continue;

                    data.put(inetAddress.getAddress());
                    return;
                }
            }
        }
        catch (SocketException ex) {}
    }

    public byte ReadByte()
    {
        byte retVal = data.get();
        return retVal;
    }

    public byte ReadByte(int offset)
    {
        return data.get(offset);
    }

    public byte[] ReadBytes(int count)
    {
        byte[] retVal = new byte[count];
        data.get(retVal);
        return retVal;
    }

    public short ReadUint16()
    {
        return data.getShort();
    }

    public int ReadUint32()
    {
        return data.getInt();
    }

    public float ReadFloat()
    {
        return data.getFloat();
    }

    public String ReadAsciiString(int length)
    {
        byte[] stringBytes = new byte[length];
        data.get(stringBytes);
        return EncodingUtils.getAsciiString(stringBytes, 0, length);
    }

    public String ReadAsciiCString()
    {
        int position = data.position();

        while (data.get() != 0);

        byte[] stringBytes = new byte[data.position() - position - 1];
        data.position(position); // reset to position
        data.get(stringBytes); // read correct amount of bytes
        data.get(); // Skip terminary 0
        return EncodingUtils.getAsciiString(stringBytes, 0, stringBytes.length);
    }

    /**
     * NOTE: This function can only be used if you are DONE with writing your packet!
     */
    public ByteBuffer ToSendableData()
    {
        ByteBuffer copy = data.duplicate();
        copy.order(ByteOrder.LITTLE_ENDIAN);
        copy.flip();
        return copy;
    }
}
