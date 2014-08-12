package org.warpten.wandrowid.network.battlenet;


import org.warpten.wandrowid.G;
import org.warpten.wandrowid.crypto.BigNumber;
import org.warpten.wandrowid.network.GamePacket;
import org.warpten.wandrowid.network.GameSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class BattlenetSocket implements GameSocket {
    private BigNumber N;
    private static BigNumber g = new BigNumber("2", 16);
    private BigNumber K;
    private boolean _authed = false;
    private static HashMap<BattlenetPacketHeader, BattlenetPacket> packetHandlers;
    private static volatile SocketChannel socket;
    private Thread socketThread;

    public BattlenetSocket() throws NoSuchAlgorithmException
    {
        packetHandlers = new HashMap<BattlenetPacketHeader, BattlenetPacket>();

        final byte[] nBytes = new byte[] {
            (byte)0xAB, (byte)0x24, (byte)0x43, (byte)0x63, (byte)0xA9, (byte)0xC2, (byte)0xA6, (byte)0xC3,
            (byte)0x3B, (byte)0x37, (byte)0xE4, (byte)0x61, (byte)0x84, (byte)0x25, (byte)0x9F, (byte)0x8B,
            (byte)0x3F, (byte)0xCB, (byte)0x8A, (byte)0x85, (byte)0x27, (byte)0xFC, (byte)0x3D, (byte)0x87,
            (byte)0xBE, (byte)0xA0, (byte)0x54, (byte)0xD2, (byte)0x38, (byte)0x5D, (byte)0x12, (byte)0xB7,
            (byte)0x61, (byte)0x44, (byte)0x2E, (byte)0x83, (byte)0xFA, (byte)0xC2, (byte)0x21, (byte)0xD9,
            (byte)0x10, (byte)0x9F, (byte)0xC1, (byte)0x9F, (byte)0xEA, (byte)0x50, (byte)0xE3, (byte)0x09,
            (byte)0xA6, (byte)0xE5, (byte)0x5E, (byte)0x23, (byte)0xA7, (byte)0x77, (byte)0xEB, (byte)0x00,
            (byte)0xC7, (byte)0xBA, (byte)0xBF, (byte)0xF8, (byte)0x55, (byte)0x8A, (byte)0x0E, (byte)0x80,
            (byte)0x2B, (byte)0x14, (byte)0x1A, (byte)0xA2, (byte)0xD4, (byte)0x43, (byte)0xA9, (byte)0xD4,
            (byte)0xAF, (byte)0xAD, (byte)0xB5, (byte)0xE1, (byte)0xF5, (byte)0xAC, (byte)0xA6, (byte)0x13,
            (byte)0x1C, (byte)0x69, (byte)0x78, (byte)0x64, (byte)0x0B, (byte)0x7B, (byte)0xAF, (byte)0x9C,
            (byte)0xC5, (byte)0x50, (byte)0x31, (byte)0x8A, (byte)0x23, (byte)0x08, (byte)0x01, (byte)0xA1,
            (byte)0xF5, (byte)0xFE, (byte)0x31, (byte)0x32, (byte)0x7F, (byte)0xE2, (byte)0x05, (byte)0x82,
            (byte)0xD6, (byte)0x0B, (byte)0xED, (byte)0x4D, (byte)0x55, (byte)0x32, (byte)0x41, (byte)0x94,
            (byte)0x29, (byte)0x6F, (byte)0x55, (byte)0x7D, (byte)0xE3, (byte)0x0F, (byte)0x77, (byte)0x19,
            (byte)0xE5, (byte)0x6C, (byte)0x30, (byte)0xEB, (byte)0xDE, (byte)0xF6, (byte)0xA7, (byte)0x86
        };

        N = new BigNumber(nBytes);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        digest.update(nBytes);
        digest.update(g.toByteArray(1));
        K = new BigNumber(digest.digest());
    }

    public void SendPacket(GamePacket packet)
    {
        if (!(packet instanceof BattlenetClientPacket))
            return;

        try {
            ByteBuffer data = packet.ToByteBuffer();
            if (data != null)
                socket.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect()
    {
        socketThread = new Thread(new BattlenetSocketRunnable());
        socketThread.start();
    }

    public void close()
    {
        try {
            socket.close();
            socketThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final class BattlenetSocketRunnable implements Runnable {
        private BattlenetSocketRunnable() { }

        @Override
        public void run() {
            try {
                internalRun();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void internalRun() throws Exception {
            socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress(G.RealmAddress, 1119)); // TODO fix port

            long timer = System.currentTimeMillis();
            while (!socket.finishConnect())
                if ((System.currentTimeMillis() - timer) >= 10000)
                    throw new TimeoutException("Timed out while connecting to realm.");

            ByteBuffer buffer = ByteBuffer.allocate(0x1000); // MaxSize
            while (!Thread.currentThread().isInterrupted()) {
                buffer.clear();
                int readSize = 1;
                while (readSize > 0)
                    readSize = socket.read(buffer);

                if (readSize == -1)
                    break;

                BattlenetBitStream packet = new BattlenetBitStream(buffer.position());
                byte[] bufferData = new byte[buffer.position()];

                buffer.flip();
                buffer.get(bufferData);

                BattlenetCrypto.Decrypt(bufferData);
                packet.WriteBytes(bufferData, bufferData.length);

                while (!packet.IsRead()) {
                    try {
                        BattlenetPacketHeader header = new BattlenetPacketHeader();
                        header.Opcode = (int)packet.Read(6);
                        if (packet.ReadBool())
                            header.Channel = (int)packet.Read(4);

                        if (header.Channel != BattlenetPacketHeader.CHANNEL_AUTHENTICATION && !_authed)
                            break; // not authed

                        BattlenetPacket handler = packetHandlers.get(header);
                        if (handler != null && handler.Interprete(BattlenetSocket.this))
                            break;

                        packet.AlignToNextByte();

                    } catch (BattlenetBitStream.BitStreamException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
