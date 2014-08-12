package org.warpten.wandrowid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.util.ByteArrayBuffer;
import org.warpten.wandrowid.crypto.ARC4;
import org.warpten.wandrowid.handlers.Handlers;
import org.warpten.wandrowid.handlers.cataclysm.CataclysmHandlers;
import org.warpten.wandrowid.handlers.wotlk.WoTLKHandlers;
import org.warpten.wandrowid.network.GamePacket;
import org.warpten.wandrowid.network.GameSocket;
import org.warpten.wandrowid.network.WorldPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

/**
 * This class connects to the world server.
 * NOTE: Starting from 4.3.4 client and server exchange some strings.
 *       These are used to determine if this is a WoTLK or a Cataclysm
 *       server.
 */
public class WorldSocket implements GameSocket {
    // TODO: Remove this, see GameSocket.java
    public void SendRealmList() { }

    private Thread socketThread;
    protected volatile SocketChannel socket;
    public volatile Handlers opcodeHandlers;

    private Handler _interfaceHandler;

    private volatile Queue<WorldPacket> _sendQueue;

    public void SendPacket(GamePacket sendData)
    {
        _sendQueue.add((WorldPacket)sendData);
    }

    // private ServiceCommunicator serviceComm = new ServiceCommunicator();

    public WorldSocket(Handler interfaceHandler) {
        _interfaceHandler = interfaceHandler;

        _sendQueue = new LinkedList<WorldPacket>();

        if (G.IsCataclysm)
            opcodeHandlers = new CataclysmHandlers(this);
        else if (G.IsWoTLK)
            opcodeHandlers = new WoTLKHandlers(this);
    }

    public void connect(/*Context activityContext*/) {
        socketThread = new Thread(new WorldSocketThread());
        socketThread.start();

        // Start the service
        /*Intent intent = new Intent(activityContext, SocketService.class);
        intent.putExtra("messageHandler", serviceComm);
        activityContext.startService(intent);*/
    }

    public final void close()
    {
        socketThread.interrupt();
        incomingMsgHandler.removeCallbacksAndMessages(null);
    }

    public Handler incomingMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            byte[] data = bundle.getByteArray("worldPacket");
            short opcode = bundle.getShort("opcode");

            WorldPacket recvData = new WorldPacket(opcode, data);
            opcodeHandlers.CallHandler(recvData);
        }
    };

    private class WorldSocketThread implements Runnable {
        private ByteArrayBuffer incomingDataBuffer;
        private int decryptedHeaderSize;

        WorldSocketThread()
        {
            incomingDataBuffer = new ByteArrayBuffer(0x200);
            decryptedHeaderSize = 0;
        }

        @Override
        public void run() {
            try {
                internalRun();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i("WandroWid", "Went out of the thread, something went bad. Breakpoint me!");
        }

        private void internalRun() throws Exception {
            socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress(G.RealmAddress, G.RealmPort));

            long timer = System.currentTimeMillis();
            while (!socket.finishConnect())
                if ((System.currentTimeMillis() - timer) >= 10000)
                    throw new TimeoutException("Timed out while connecting to realm.");

            PacketLogger.Initialize();

            while (!Thread.currentThread().isInterrupted()) {
                TrySendPacket();

                ByteBuffer buffer = ByteBuffer.allocateDirect(0x100);
                int readSize = 1;
                while (readSize > 0)
                    readSize = socket.read(buffer);

                if (readSize == -1) {
                    // Inform GUI
                    break;
                }

                buffer.flip(); // Make ready for reading
                for (int i = 0, l = buffer.remaining(); i < l; ++i)
                    incomingDataBuffer.append(buffer.get());

                // Do not do anything until we at least have a complete header
                if (incomingDataBuffer.length() <= 4)
                    continue;

                byte[] dataBuffer = incomingDataBuffer.toByteArray();
                ByteBuffer workBuffer = ByteBuffer.wrap(dataBuffer);

                int opcodeSize = -1;
                int opcode = -1;

                /// if the packet failed to be handled previous iteration
                /// because the socket didnt pick up the whole body,
                /// we still had the header encrypted, in which case
                /// decryptedHeaderSize = 0.
                /// Otherwise, the decrypted header is in the buffer
                byte[] smsgHeader;
                if (decryptedHeaderSize == 0)
                    smsgHeader = ARC4.DecryptPacketHeader(dataBuffer);
                else
                    smsgHeader = Arrays.copyOfRange(dataBuffer, 0, decryptedHeaderSize);

                // Read size and opcode
                if (smsgHeader.length == 4) {
                    opcodeSize = (((smsgHeader[0] & 0xFF) << 8) | (smsgHeader[1] & 0xFF)) + 2;
                    opcode = ((smsgHeader[3] & 0xFF) << 8) | (smsgHeader[2] & 0xFF);
                } else { // if (smsgHeader.length == 5)
                    opcodeSize = (((smsgHeader[0] & 0x7F) << 16) | ((smsgHeader[1] & 0xFF) << 8) | (smsgHeader[2] & 0xFF)) + 2;
                    opcode = (((smsgHeader[4] & 0xFF) << 8) | (smsgHeader[3] & 0xFF));
                }

                /// At this point, if the header was decrypted this pass,
                /// update decryptedHeaderSize so we do not decrypt next
                /// loop in case the packet is still not complete
                if (decryptedHeaderSize == 0) {
                    System.arraycopy(smsgHeader, 0, dataBuffer, 0, smsgHeader.length);
                    decryptedHeaderSize = smsgHeader.length;
                }

                // Something terrible happened...
                if (opcode <= 0)
                    continue;

                // DebugDump(dataBuffer);

                opcodeSize -= 4; // Now actual body size

                // Set position to beginning of data
                workBuffer.position(smsgHeader.length);

                // If buffer doesnt contain the whole opcode, try again next iteration
                // Since incomingDataBuffer contains all the magic
                if (workBuffer.remaining() < opcodeSize) {
                    // incomingDataBuffer doesnt mirror changes to dataBuffer on its own.
                    System.arraycopy(smsgHeader, 0, incomingDataBuffer.buffer(), 0, smsgHeader.length);
                    continue;
                }

                // Extract the whole packet (minus size and opc)
                byte[] pktBodyBuffer = new byte[opcodeSize];
                workBuffer.get(pktBodyBuffer);

                // Extract all bytes of the remains and put them back to the global buffer
                byte[] remainingArray = new byte[workBuffer.remaining()];
                workBuffer.get(remainingArray);
                incomingDataBuffer = new ByteArrayBuffer(remainingArray.length);
                incomingDataBuffer.append(remainingArray, 0, remainingArray.length);
                decryptedHeaderSize = 0;

                Message msg = incomingMsgHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putByteArray("worldPacket", pktBodyBuffer);
                bundle.putShort("opcode", (short)opcode);
                msg.setData(bundle);
                msg.sendToTarget();
            }
        }

        private void TrySendPacket()
        {
            if (!_sendQueue.isEmpty()) {
                WorldPacket packet = _sendQueue.poll();

                try {
                    PacketLogger.LogPacket(packet, PacketLogger.CLIENT_TO_SERVER);
                    // TODO: Deflate packets if needed, before ARC4'ing
                    ByteBuffer data = packet.ToByteBuffer();
                    while (data.remaining() > 0)
                        socket.write(data);

                    G.Log("[WorldServer: C->S] Sending " + packet.GetOpcodeForLogging());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void DebugDump(byte[] dataBuffer)
        {
            G.Log("[DEBUG] Received data buffer: " + Arrays.toString(dataBuffer));
        }
    }

    public void DisplayCharEnum() {
        Message msg = _interfaceHandler.obtainMessage();
        msg.arg2 = 3;
        msg.sendToTarget();
    }

    public void TryFlushReadyMessages() {
        Message msg = _interfaceHandler.obtainMessage();
        msg.arg2 = 4;
        msg.sendToTarget();
    }

    public void OnJoinedChannel(String channelName) {
        Message msg = _interfaceHandler.obtainMessage();
        msg.arg2 = 5;
        msg.obj = channelName;
        msg.sendToTarget();
    }
}
