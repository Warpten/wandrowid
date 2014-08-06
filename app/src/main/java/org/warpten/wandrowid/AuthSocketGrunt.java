package org.warpten.wandrowid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.warpten.wandrowid.crypto.BigNumber;
import org.warpten.wandrowid.crypto.CryptoUtils;
import org.warpten.wandrowid.network.AuthOpcodes;
import org.warpten.wandrowid.network.AuthPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

/*
 * This class is an extension of GameSocket, and handles all Grunt related data.
 * It works for both 3.3.5 and 4.3.4
 */
public class AuthSocketGrunt implements GameSocket {
    protected volatile SocketChannel socket;

    private Thread listenerThread;

    private volatile Queue<AuthPacket> _writeQueue;

    protected BigNumber _passwordHash;
    protected byte[] _userHash;

    private Handler _interface;

    public AuthSocketGrunt(Handler interfaceHandler) {
        _writeQueue = new LinkedList<AuthPacket>();
        _interface = interfaceHandler;
    }

    public void SendPacket(AuthPacket packet)
    {
        _writeQueue.add(packet);
    }

    public final void close()
    {
        listenerThread.interrupt();
        incomingMessageHandler.removeCallbacksAndMessages(null);
    }

    public final void SendMessage(String text, int progress)
    {
        if (progress == -1)
            close();

        Message msg = _interface.obtainMessage();
        msg.arg2 = 2; // auth progress display update
        msg.arg1 = progress == -1 ? 0 : progress;
        msg.obj = text;
        msg.sendToTarget();
    }

    public final void SendMessage(AuthPacket packet)
    {
        Message msg = _interface.obtainMessage();
        msg.obj = packet;
        msg.arg2 = 1; // realmlist data
        msg.sendToTarget();
    }

    private Handler incomingMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            Bundle data = msg.getData();
            HandleDataBundle(data);
        }
    };

    public void OnConnectionError(String message)
    {
        SendMessage(message, -1);
    }

    public void connect()
    {
        try {
            SendMessage("Connecting to server...", 0);

            _userHash = CryptoUtils.SHA1(G.Username);
            _passwordHash = new BigNumber(CryptoUtils.SHA1((G.Username + ":" + G.Password).toUpperCase()));

            _writeQueue.add(SendAuthChallenge());
            listenerThread = new Thread(new AuthSocketThread());
            listenerThread.start();
        } catch (Exception e) {
            // Inform GUI
        }

    }

    public AuthPacket SendAuthChallenge()
    {
        // Size can be approximate as long as it is larger than final packet size
        AuthPacket challenge = new AuthPacket(AuthOpcodes.LOGON_CHALLENGE, G.Username.length() + 50);
        challenge.WriteByte((byte)0x00); // LOGON_CHALLENGE
        challenge.WriteByte((byte)8); // Error
        challenge.WriteByte((byte)(G.Username.length() + 30)); // Size
        challenge.WriteBytes(new byte[] { 0, 87, 111, 87, 0 });   // WoW ( WoW)
        challenge.WriteBytes(new byte[] { 3, 3, 5 });           // Game Version (3.3.5)
        challenge.WriteUint16((short)12340);                      // Build (12340)
        challenge.WriteBytes(new byte[] { 54, 56, 120, 0 });   // 68x (x86 )
        challenge.WriteBytes(new byte[] { 110, 105, 87, 0 });  // niW (Win )
        challenge.WriteBytes(new byte[] { 82, 70, 114, 102 }); // RFrf (frFR)
        challenge.WriteUint32(0x3C); // Time zone bias (60)
        challenge.WriteIP();
        challenge.WriteByte((byte) G.Username.length());
        challenge.WriteAsciiString(G.Username);
        SendMessage("Sending AUTH_LOGON_CHALLENGE", 10);
        return challenge;
    }

    public void SendAuthProof(byte[] A, byte[] M1, byte[] CRC)
    {
        AuthPacket proof = new AuthPacket(AuthOpcodes.LOGON_PROOF, 32 + 20 + 20 + 3);
        proof.WriteByte((byte)0x01); // LOGON_PROOF
        proof.WriteBytes(A);
        proof.WriteBytes(M1);
        proof.WriteBytes(CRC);
        proof.WriteByte((byte)0);
        proof.WriteByte((byte)0);
        SendMessage("Sending AUTH_LOGON_PROOF", 50);
        SendPacket(proof);
    }

    public void SendRealmList()
    {
        AuthPacket realmList = new AuthPacket(AuthOpcodes.REALM_LIST, 5);
        realmList.WriteByte((byte)0x10); // REALM_LIST
        realmList.WriteUint32(0);
        SendMessage("Authentified, requesting realm list.", 90);
        SendPacket(realmList);
    }

    public void HandleLogonChallenge(AuthPacket opcode) throws Exception
    {
        SendMessage("Received AUTH_LOGON_CHALLENGE", 20);
        opcode.ReadByte(); // Skipped, always 0x00
        byte errorCode = opcode.ReadByte();

        if (errorCode != 0x00) {
            switch (errorCode)
            {
                case 0x04: // AUTH_RESULT_NO_MATCH
                    SendMessage("Unknown account name.", -1);
                    break;
                case 0x06: // AUTH_RESULT_ACCOUNT_IN_USE
                    SendMessage("Account is already logged in.", -1);
                    break;
                case 0x09: // AUTH_RESULT_WRONG_BUILD_NUMBER
                    SendMessage("Invalid game version selected.", -1);
                    break;
                default:
                    SendMessage("Unknown error.", -1);
                    break;
            }
            return;
        }

        BigNumber k = new BigNumber("3", 10);

        // Server public key B
        BigNumber B = new BigNumber(opcode.ReadBytes(32));

        opcode.ReadByte(); // Skip gLen
        BigNumber g = new BigNumber(opcode.ReadBytes(1));

        // Modulus N
        opcode.ReadByte(); // Skip nLen
        BigNumber N = new BigNumber(opcode.ReadBytes(32));

        // Read salt
        byte[] saltArray = opcode.ReadBytes(32);
        BigNumber salt = new BigNumber(saltArray);

        opcode.ReadBytes(16); // Skip unk3
        opcode.ReadByte(); // SecurityFlags

        BigNumber x = new BigNumber(CryptoUtils.SHA1(salt.toByteArray(32), _passwordHash.toByteArray(20)));

        BigNumber A;
        BigNumber a;
        do
        {
            a = BigNumber.setRandBytes(19);
            A = g.modPow(a, N);
        } while (A.modPow(BigNumber.ONE, N) == BigNumber.ZERO);

        // Aaaand u fails again. Sigh.
        BigNumber u = new BigNumber(CryptoUtils.SHA1(A.toByteArray(32), B.toByteArray(32)));

        // Session Key
        // S = ((B + k * (N - g.ModPow(x, N))) % N).ModPow(a.add(u.multiply(x)), N);
        BigNumber S = B.add(k.multiply(N.substract(g.modPow(x, N)))
                .mod(N))
                .modPow(a.add(u.multiply(x)), N);

        byte[] keyHash;
        byte[] sData = S.toByteArray(32);

        byte[] keyData = new byte[40];
        byte[] temp = new byte[16];

        for (int i = 0; i < 16; ++i)
            temp[i] = sData[i * 2];
        keyHash = CryptoUtils.SHA1(temp);
        for (int i = 0; i < 20; ++i)
            keyData[i * 2] = keyHash[i];

        for (int i = 0; i < 16; ++i)
            temp[i] = sData[i * 2 + 1];
        keyHash = CryptoUtils.SHA1(temp);
        for (int i = 0; i < 20; ++i)
            keyData[i * 2 + 1] = keyHash[i];

        G.SessionKey = new BigNumber(keyData);

        // Generate crypto proof now
        byte[] gNhash = new byte[20];

        byte[] nHash = new BigNumber(CryptoUtils.SHA1(N.toByteArray(32))).toByteArray();
        for (int i = 0; i < 20; ++i)
            gNhash[i] = nHash[i];

        byte[] gHash = new BigNumber(CryptoUtils.SHA1(g.toByteArray())).toByteArray(20);
        for (int i = 0; i < 20; ++i)
            gNhash[i] ^= gHash[i];

        // This fails once again...
        BigNumber M1 = new BigNumber(CryptoUtils.SHA1(
                gNhash,
                _userHash,
                salt.toByteArray(32),
                A.toByteArray(32),
                B.toByteArray(32),
                G.SessionKey.toByteArray(40)
        ));

        // Expected server proof
        G.M2 = CryptoUtils.SHA1(
                A.toByteArray(32),
                M1.toByteArray(20),
                G.SessionKey.toByteArray(40)
        );

        SendAuthProof(A.toByteArray(32), M1.toByteArray(20), new byte[20]);
    }

    public void HandleAuthProof(AuthPacket opcode)
    {
        SendMessage("Received AUTH_LOGON_PROOF", 70);

        byte errorCode = opcode.ReadByte();
        if (errorCode != 0x00) {
            switch (errorCode)
            {
                case 0x0A: // UPDATE_CLIENT
                    SendMessage("Client update requested.", -1);
                    break;
                case 0x04: // NO_MATCH
                case 0x05: // UNKNOWN
                    SendMessage("Invalid password, account, or auth error.", -1);
                    break;
                case 0x09: // WRONG_BUILD
                    SendMessage("Invalid version.", -1);
                    break;
                default:
                    SendMessage("Unknown protocol error.", -1);
            }
            return;
        }

        byte[] M2 = opcode.ReadBytes(20);
        Log.i("WandroWid", "SRP6: Received M2 = " + CryptoUtils.convertToHex(M2));
        if (Arrays.equals(M2, G.M2))
            SendRealmList();
        else
            SendMessage("Protocol proofs do not match.", -1);
    }

    public void HandleRealmlist(AuthPacket opcode)
    {
        SendMessage("Received AUTH_REALMLIST", 100);
        SendMessage(opcode);
    }

    public void HandleDataBundle(Bundle b)
    {
        AuthPacket opcode = new AuthPacket(b.getByteArray("AuthPacket"));
        Log.i("WandroWid", "[AuthServer: S->C] Received 0x" + Integer.toString(opcode.GetOpcode(), 16));
        switch (opcode.GetOpcode())
        {
            case 0x00: // AUTH_LOGON_CHALLENGE
                try {
                    HandleLogonChallenge(opcode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 0x01: // AUTH_LOGON_PROOF
                HandleAuthProof(opcode);
                break;
            case 0x010: // REALMLIST
                HandleRealmlist(opcode);
                break;
        }
    }

    class AuthSocketThread implements Runnable {
        private static final int port = 3724;

        public AuthSocketThread() { }

        @Override
        public void run() {
            try {
                internalRun();
            } catch (TimeoutException e) {
                OnConnectionError("Timed out while connecting.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                OnConnectionError("Unknown error.");
                Thread.currentThread().interrupt();
            }
        }

        private void internalRun() throws Exception
        {
            // Non blocking socket
            socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress(G.RealmName, port));

            long timer = System.currentTimeMillis();
            while (!socket.finishConnect())
                if ((System.currentTimeMillis() - timer) >= 10000)
                    throw new TimeoutException("Timed out while connecting.");

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PushCommand();

                    ByteBuffer buffer = ByteBuffer.allocate(512);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    int packetSize = 0;
                    int readSize = 0;
                    while ((readSize = socket.read(buffer)) > 0) // Returns the number of bytes read
                        packetSize += readSize;

                    // End of stream
                    if (readSize == -1)
                        break;

                    // Nothing read yet (readSize is always == 0 here)
                    if (packetSize == 0)
                        continue;

                    // This will be propagated to the handler
                    Message commandMessage = new Message();
                    Bundle commandBundle = new Bundle();

                    buffer.flip(); // Make ready for reading
                    byte[] packet = new byte[packetSize];
                    buffer.get(packet);
                    Log.d("WandroWid", "Received " + Arrays.toString(packet));
                    commandBundle.putByteArray("AuthPacket", packet);

                    commandMessage.setData(commandBundle);
                    incomingMessageHandler.sendMessage(commandMessage);
                } catch (Exception e) {
                    Log.i("WandroWid", "Closing auth socket.");
                    throw e;
                }
            }
        }

        /**
         * Sends the next available opcode to the server.
         * @throws java.io.IOException
         */
        private void PushCommand() throws IOException
        {
            if (_writeQueue.isEmpty())
                return;

            AuthPacket pkt = _writeQueue.poll();
            if (pkt == null || pkt.GetDataSize() == 0)
                return;

            Log.i("WandroWid", "[AuthServer: C->S] Sending 0x" + Integer.toString(pkt.GetOpcode(), 16));
            int writeCount = socket.write(pkt.ToSendableData());
        }
    }
}
