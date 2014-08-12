package org.warpten.wandrowid.network.battlenet;

/**
 * Created by perquet on 12/08/14.
 */
public class BattlenetModuleInfo {
    public BattlenetModuleInfo()
    {
        Type = "";
        Region = "EU";
        DataSize = 0;
    }

    public BattlenetModuleInfo(BattlenetModuleInfo other) {
        Type = other.Type;
        Region = other.Region;
        ModuleId = other.ModuleId;
        DataSize = other.DataSize;
        Data = other.Data;
    }

    public String Type;
    public String Region;
    public int ModuleId;
    public int DataSize;
    public byte[] Data;
}
