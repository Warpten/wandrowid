package org.warpten.wandrowid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.warpten.wandrowid.crypto.BigNumber;
import org.warpten.wandrowid.fragments.CharEnumStruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that stores gobal data. (Just like R assets resources) (less typing than Globals)
 */
public class G {
    private G() { }

    public static String RealmName = "Empty";
    public static String Username = "Empty";
    public static String Password = "Empty";

    public static SharedPreferences Preferences;
    public static boolean GetBooleanSetting(String key, boolean defaultValue) {
        return Preferences.getBoolean(key, defaultValue);
    }

    public static String GetStringSetting(String key) {
        return Preferences.getString(key, "");
    }

    public static GameSocket      Socket;
    public static WorldSocket     WorldSocket() { return (WorldSocket)Socket; }
    public static GruntSocket     GruntSocket() { return (GruntSocket)Socket; }

    public static boolean IsWoTLK = false;
    public static boolean IsCataclysm = false;
    public static boolean IsRetail = false;

    public static BigNumber SessionKey;

    public static byte[] M2;

    public static Context Context;
    public static String GetLocalizedString(int identifier) { return Context.getString(identifier); }

    public static void Log(String message)
    {
        if (GetBooleanSetting("logcat_logging", true))
            Log.i("Wandrowid", message);
    }

    // World Data
    public static String RealmAddress = "0.0.0.0";
    public static int RealmPort = 8085;

    public static void WoTLK() {
        IsWoTLK = true;
        IsCataclysm = false;
        IsRetail = false;
    }

    public static void Cataclysm() {
        IsWoTLK = false;
        IsCataclysm = true;
        IsRetail = false;
    }

    public static int ClientVersion() {
        if (IsWoTLK)
            return 12340;
        if (IsCataclysm)
            return 15595;
        return 0; // Not intended for retail
    }

    public static CharEnumStruct CharacterData;

    public static List<Bundle> ChatMessageQueue = new ArrayList<Bundle>();
    public static List<Bundle> ReadyMessageQueue = new ArrayList<Bundle>();
    public static List<String> JoinedChannels = new ArrayList<String>();
    public static Map<Long, String> CharacterCache = new HashMap<Long, String>();

    public static void EnqueueChatMessage(Bundle data)
    {
        ChatMessageQueue.add(data);
    }

    public static void TryFlushReadyMessages()
    {
        for (int i = 0; i < ChatMessageQueue.size(); ++i) {
            Bundle bundle = ChatMessageQueue.get(i);

            if (bundle.getLong("receiverGuid") != 0L
                && !CharacterCache.containsKey(bundle.getLong("receiverGuid")))
                    continue;

            if (bundle.getLong("senderGuid") != 0L
                && !CharacterCache.containsKey(bundle.getLong("senderGuid")))
                continue;

            bundle.putString("receiverName", CharacterCache.get(bundle.getLong("receiverGuid")));
            bundle.putString("senderName", CharacterCache.get(bundle.getLong("senderGuid")));

            ReadyMessageQueue.add(bundle);
            ChatMessageQueue.remove(i);
            --i;
        }

        if (ReadyMessageQueue.isEmpty())
            return;

        // Inform GUI that shit is now available
        WorldSocket().TryFlushReadyMessages();
    }

    public static int CurrentCharacterIndex = -1;

    public static long GetGuidForActiveCharacter()
    {
        return new ObjectGuid(CharacterData.CharGuids[CurrentCharacterIndex]).GUID;
    }

    public static int GetLanguageForActiveCharacter()
    {
        // TODO: probably return orcish or common rather than all this elaborate stuff
        switch (CharacterData.CharRaces[CurrentCharacterIndex])
        {
            case 1: return 7; // Common
            case 2: return 1; // Orcish
            case 3: return 6; // Dwarvish
            case 4: return 2; // Darnassian
            case 5: return 33; // GutterSpeak
            case 6: return 3; // Taurahe
            case 7: return 13; // Gnomish
            case 8: return 14; // Troll
            case 9: return 0; // Goblins, universal for now
            case 10: return 10; // Thalassian
            case 11: return 35; // Draenei
            case 22: return 0; // Worgens, universal for now
            default:
                return 0; // Universal
        }
    }

    public static boolean IsInGuild()
    {
        return CharacterData.InGuild[CurrentCharacterIndex];
    }

}
