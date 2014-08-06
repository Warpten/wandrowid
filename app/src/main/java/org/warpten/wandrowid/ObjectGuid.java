package org.warpten.wandrowid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by perquet on 25/07/14.
 */
public class ObjectGuid {
    public long GUID;

    public static final int HIGHGUID_ITEM           = 0x400;                       // blizz 4000
    public static final int HIGHGUID_CONTAINER      = 0x400;                       // blizz 4000
    public static final int HIGHGUID_PLAYER         = 0x000;                       // blizz 0000
    public static final int HIGHGUID_GAMEOBJECT     = 0xF11;                       // blizz F110
    public static final int HIGHGUID_TRANSPORT      = 0xF12;                       // blizz F120 (for GAMEOBJECT_TYPE_TRANSPORT)
    public static final int HIGHGUID_UNIT           = 0xF13;                       // blizz F130
    public static final int HIGHGUID_PET            = 0xF14;                       // blizz F140
    public static final int HIGHGUID_VEHICLE        = 0xF15;                       // blizz F550
    public static final int HIGHGUID_DYNAMICOBJECT  = 0xF10;                       // blizz F100
    public static final int HIGHGUID_CORPSE         = 0xF101;                      // blizz F100
    public static final int HIGHGUID_AREATRIGGER    = 0xF102;                      // blizz F100
    public static final int HIGHGUID_BATTLEGROUND   = 0x1F1;                       // new 4.x
    public static final int HIGHGUID_MO_TRANSPORT   = 0x1FC;                       // blizz 1FC0 (for GAMEOBJECT_TYPE_MO_TRANSPORT)
    public static final int HIGHGUID_GROUP          = 0x1F5;
    public static final int HIGHGUID_GUILD          = 0x1FF;                        // new 4.x

    public ObjectGuid(byte[] guidBytes) {
        GUID = ByteBuffer.wrap(guidBytes).order(ByteOrder.LITTLE_ENDIAN).getLong(0);
    }
    public ObjectGuid(long guid) { GUID = guid; }

    public byte[] asByteArray()
    {
        byte[] ret = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(GUID);
        buffer.flip();
        buffer.get(ret);
        return ret;
    }

    public boolean equals(ObjectGuid other) { return GUID == other.GUID; }

    public boolean Is(int type)
    {
        return HIPART() == type && GUID != 0;
    }

    public int HIPART()
    {
        int t = (int)((GUID >> 48) & 0x0000FFFF);
        if (t == HIGHGUID_CORPSE || t == HIGHGUID_AREATRIGGER)
            return t;
        return ((t >> 4) & 0x00000FFF);
    }

    public boolean IsGuidHaveEnPart()
    {
        switch (HIPART())
        {
            case HIGHGUID_ITEM:
            case HIGHGUID_PLAYER:
            case HIGHGUID_DYNAMICOBJECT:
            case HIGHGUID_CORPSE:
            case HIGHGUID_GROUP:
            case HIGHGUID_GUILD:
                return false;
            case HIGHGUID_GAMEOBJECT:
            case HIGHGUID_TRANSPORT:
            case HIGHGUID_UNIT:
            case HIGHGUID_PET:
            case HIGHGUID_VEHICLE:
            case HIGHGUID_MO_TRANSPORT:
            case HIGHGUID_AREATRIGGER:
            default:
                return true;
        }
    }
}
