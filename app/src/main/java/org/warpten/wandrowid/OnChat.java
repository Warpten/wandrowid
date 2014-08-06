package org.warpten.wandrowid;

/**
 * Created by perquet on 25/07/14.
 */
public interface OnChat {
    public void PropagateMessage(String type, String name, String[] message);
}
