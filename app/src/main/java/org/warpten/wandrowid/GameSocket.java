package org.warpten.wandrowid;

/**
 * An abstract interface solely used for code purpose.
 */
public interface GameSocket {
    public void connect();
    public void close();

    // TODO: Remove this one, so WorldSocket doesn't have to inherit it.
    // TODO: Make a generic SendPacket(WorldPacket) exposed.
    public void SendRealmList();
}
