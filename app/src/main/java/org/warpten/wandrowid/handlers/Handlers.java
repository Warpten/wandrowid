package org.warpten.wandrowid.handlers;

import org.warpten.wandrowid.network.WorldPacket;
import org.warpten.wandrowid.WorldSocket;

// An empty interface used to store Handlers.
public abstract class Handlers {
    protected WorldSocket socket;

    public Handlers(WorldSocket socket)
    {
        this.socket = socket;
    }

    public abstract void CallHandler(WorldPacket opc);
    public abstract void SendPlayerLogin(String charName, byte[] guid);
    public abstract void SendChannelJoin(int channelId, String channelName, String channelPassword);
    public abstract void SendMessageChat(int type, String... args);
    public abstract void SendLeaveChannel(int channelId, String channelName);
}
