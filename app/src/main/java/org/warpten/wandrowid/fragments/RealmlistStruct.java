package org.warpten.wandrowid.fragments;

import android.os.Parcel;
import android.os.Parcelable;

import org.warpten.wandrowid.network.AuthPacket;

import java.util.ArrayList;

/**
 * This class stores realm data internally, so that it can be later accessed by RealmlistAdapter.
 * It works on both 3.3.5 and 4.3.4
 */
// TODO according to gradle this uses unchecked/unsafe operations
public class RealmlistStruct implements Parcelable {
    public ArrayList<Byte> realmTypes;
    public ArrayList<Byte> realmFlags;
    public ArrayList<String> realmNames;
    public ArrayList<String> realmAddresses;
    public ArrayList<Integer> realmPorts;
    public ArrayList<String> realmVersions;

    @Override
    public int describeContents() { return 0; }

    public RealmlistStruct(AuthPacket opcode) {
        UpdateData(opcode);
    }

    public void UpdateData(AuthPacket opcode) {
        opcode.ReadUint16(); // pkt.size() + RealmListSizeBuffer.size()
        opcode.ReadUint32(); // Always 0x00
        short realmCount = opcode.ReadUint16(); // Short since 2.x

        // Initialize member data
        realmTypes = new ArrayList<Byte>(realmCount);
        realmFlags = new ArrayList<Byte>(realmCount);
        realmNames = new ArrayList<String>(realmCount);
        realmAddresses = new ArrayList<String>(realmCount);
        realmPorts = new ArrayList<Integer>(realmCount);
        realmVersions = new ArrayList<String>(realmCount);

        int writeIndex = 0;
        for (short i = 0; i < realmCount; ++i) {
            byte realmType = opcode.ReadByte();
            boolean isLocked  = (opcode.ReadByte() != 0);
            if (!isLocked) {
                realmTypes.add(realmType);

                byte realmFlag = opcode.ReadByte();
                realmFlags.add(realmFlag);

                realmNames.add(opcode.ReadAsciiCString());

                String[] tokens = opcode.ReadAsciiCString().split(":");
                realmAddresses.add(tokens[0]);
                realmPorts.add(Integer.parseInt(tokens.length > 1 ? tokens[1] : "8085"));

                opcode.ReadFloat(); // Population, skip
                opcode.ReadByte(); // Load, skip
                opcode.ReadByte(); // Timezone, skip
                opcode.ReadByte(); // Unk skipped, set to 0x2C in 2.x and upper

                if ((realmFlag & 4) != 0) {
                    byte[] d = opcode.ReadBytes(3);
                    int build = (int) opcode.ReadUint16();
                    String version = Integer.toString(d[0]) + "." + Integer.toString(d[1]) + "." +
                            Integer.toString(d[2]) + "." + Integer.toString(build);
                    realmVersions.add(version);
                } else
                    realmVersions.add("No server-specific version.");
            } else { // Skip everything
                boolean withVersion = ((opcode.ReadByte() & 4) != 0);
                opcode.ReadAsciiCString();
                opcode.ReadAsciiCString();
                opcode.ReadFloat();
                opcode.ReadByte();
                opcode.ReadByte();
                opcode.ReadByte();
                if (!withVersion)
                    continue;
                opcode.ReadBytes(3);
                opcode.ReadUint16();
            }
        }
    }

    private RealmlistStruct(Parcel in)
    {
        int realmCount = in.readInt();

        realmTypes = new ArrayList<Byte>(realmCount);
        realmFlags = new ArrayList<Byte>(realmCount);
        realmNames = new ArrayList<String>(realmCount);
        realmAddresses = new ArrayList<String>(realmCount);
        realmPorts = new ArrayList<Integer>(realmCount);
        realmVersions = new ArrayList<String>(realmCount);

        realmTypes = (ArrayList<Byte>)in.readSerializable();
        realmFlags = (ArrayList<Byte>)in.readSerializable();
        in.readStringList(realmNames);
        in.readStringList(realmAddresses);
        realmPorts = (ArrayList<Integer>)in.readSerializable();
        in.readStringList(realmVersions);
    }

    public String GetVersionString(int position)
    {
        return realmVersions.get(position);
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        // Write size
        out.writeInt(realmTypes.size());
        out.writeSerializable(realmTypes);
        out.writeSerializable(realmFlags);
        out.writeStringList(realmNames);
        out.writeStringList(realmAddresses);
        out.writeSerializable(realmPorts);
        out.writeStringList(realmVersions);
    }

    public static final Parcelable.Creator<RealmlistStruct> CREATOR = new Parcelable.Creator<RealmlistStruct>() {
        @Override
        public RealmlistStruct createFromParcel(Parcel in)
        {
            return new RealmlistStruct(in);
        }

        @Override
        public RealmlistStruct[] newArray(int size)
        {
            return new RealmlistStruct[size];
        }
    };
}
