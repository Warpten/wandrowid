package org.warpten.wandrowid.network.battlenet;

public class BattlenetAuthComplete extends BattlenetServerPacket {
    public BattlenetAuthComplete() {
        super(new BattlenetPacketHeader(BattlenetPacketHeader.SMSG_AUTH_COMPLETE, BattlenetPacketHeader.CHANNEL_AUTHENTICATION));
        ErrorType = 0;
        PingTimeout = 120000;
        Threshold = 25000000;
        Rate = 1000;
        FirstName = "";
        LastName = "";
        AccountId = 0;
        Region = 2;
        GameAccountRegion = 2;
        GameAccountName = "";
        GameAccountFlags = 0;
    }

    public boolean Read(BattlenetSocket socket) {
        try {
            boolean isResultNull = Stream.ReadBool();
            if (isResultNull)
            {
                int moduleCount = (int)Stream.Read(3);
                for (int i = 0; i < moduleCount; ++i) {
                    BattlenetModuleInfo moduleInfo = new BattlenetModuleInfo();
                    moduleInfo.Type = new String(Stream.ReadBytes(4));
                    moduleInfo.Region = Stream.ReadFourCC();
                    moduleInfo.ModuleId = (int)Stream.Read(32);
                    moduleInfo.DataSize = (int)Stream.Read(10);
                    moduleInfo.Data = Stream.ReadBytes(moduleInfo.DataSize); // Might not be correct
                }
            }
            return true;
        } catch (BattlenetBitStream.BitStreamException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int ErrorType;
    public int Result;
    public int PingTimeout;
    public int Threshold;
    public int Rate;
    public String FirstName;
    public String LastName;
    public int AccountId;
    public byte Region;
    public byte GameAccountRegion;
    public String GameAccountName;
    public long GameAccountFlags;

}
