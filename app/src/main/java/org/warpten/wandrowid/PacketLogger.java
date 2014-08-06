package org.warpten.wandrowid;

import android.os.Environment;

import org.warpten.wandrowid.network.WorldPacket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Outputs PKT 3.1 .pkt files for use with WPP.
 */
public class PacketLogger {
    public static final String CLIENT_TO_SERVER = "CMSG";
    public static final String SERVER_TO_CLIENT = "SMSG";

    private static String fileName;
    private static FileOutputStream fileStream;
    private static int StartTime;

    private PacketLogger() { }

    public static boolean Enabled = false;

    public static boolean Initialize()
    {
        if (!G.GetBooleanSetting("pref_log_pkt", false))
            return false;

        try {
            StartTime = (int)(System.currentTimeMillis() / 1000);
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-hhmmss_");
            fileName = format.format(new Date()) + G.ClientVersion() + ".pkt";
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + fileName);
            file.createNewFile();
            fileStream = new FileOutputStream(file);
            Enabled = true;

            WriteHeader();
        } catch (IOException e) {
            e.printStackTrace();
            if (fileStream != null)
                fileStream.close();
        } finally {
            return Enabled;
        }
    }

    private static void WriteHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(66);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.put((byte)'P'); header.put((byte)'K'); header.put((byte)'T');
        header.putShort((short)0x0301); // 3.1
        header.put((byte)'J'); // Java Sniffer Client
        header.putInt(G.ClientVersion());
        header.put((byte)'e'); header.put((byte)'n'); header.put((byte)'U'); header.put((byte)'S');
        header.put(G.SessionKey.toByteArray(40));
        header.putInt(StartTime);
        header.putInt(getMSTime()); // SniffStartTicks
        header.putInt(0); // OptionalDataSize, 0

        byte[] headerBytes = new byte[66];
        header.flip();
        header.get(headerBytes);

        fileStream.write(headerBytes);
        fileStream.flush();
    }

    public static void LogPacket(WorldPacket packet, String direction) {
        try {
            if (!G.GetBooleanSetting("pref_log_pkt", false) || !Enabled)
                return;
            byte[] body = packet.GetBody();

            ByteBuffer packetHeader = ByteBuffer.allocate(6 * 4);
            packetHeader.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < 4; ++i)
                packetHeader.put((byte)direction.charAt(i));
            packetHeader.putInt(0); // Connection ID
            packetHeader.putInt(getMSTime());
            packetHeader.putInt(0); // OptionalDataSize
            packetHeader.putInt(body.length + 4);
            packetHeader.putInt(packet.GetOpcode());

            byte[] packetHeaderBytes = new byte[6 * 4];
            packetHeader.flip();
            packetHeader.get(packetHeaderBytes);

            fileStream.write(packetHeaderBytes);
            if (body.length != 0)
                fileStream.write(body);
            fileStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (fileStream != null)
                    fileStream.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    private static int getMSTime() {
        return (int)(System.currentTimeMillis() - StartTime * 1000);
    }
}
