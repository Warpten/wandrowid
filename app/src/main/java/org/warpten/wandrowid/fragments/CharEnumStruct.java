package org.warpten.wandrowid.fragments;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.R;

public class CharEnumStruct {
    public String[] CharNames;
    public byte[][] CharGuids;
    public int[] CharLevels;
    public int[] CharGenders;
    public int[] CharClasses;
    public int[] CharRaces;
    public boolean[] InGuild;

    public CharEnumStruct(int charCount)
    {
        CharNames = new String[charCount];
        CharGuids = new byte[charCount][8];
        CharLevels = new int[charCount];
        CharGenders = new int[charCount];
        CharClasses = new int[charCount];
        CharRaces = new int[charCount];
        InGuild = new boolean[charCount];
    }

    public int GetFactionResourceID(int index)
    {
        int charClass = CharRaces[index];
        switch (charClass) {
            case 1: // Human
            case 3: // Dwarf
            case 4: // Nightelf
            case 7: // Gnome
            case 11: // Dranei
            case 22: // Worgen
                return R.drawable.alliance;
            default:
                return R.drawable.horde;
        }
    }

    public int GetRaceResourceID(int index)
    {
        boolean isMale = CharGenders[index] == 0;
        switch (CharRaces[index])
        {
            case 1: return isMale ? R.drawable.human_male : R.drawable.human_female;
            case 2: return isMale ? R.drawable.orc_male : R.drawable.orc_female;
            case 3: return isMale ? R.drawable.dwarf_male : R.drawable.dwarf_female;
            case 4: return isMale ? R.drawable.nightelf_male : R.drawable.nightelf_female;
            case 5: return isMale ? R.drawable.undead_male : R.drawable.undead_female;
            case 6: return isMale ? R.drawable.tauren_male : R.drawable.tauren_female;
            case 7: return isMale ? R.drawable.gnome_male : R.drawable.gnome_female;
            case 8: return isMale ? R.drawable.troll_male : R.drawable.troll_female;
            case 9: return isMale ? R.drawable.goblin_male : R.drawable.goblin_female;
            case 10: return isMale ? R.drawable.bloodelf_male : R.drawable.bloodelf_female;
            case 11: return isMale ? R.drawable.draenei_male : R.drawable.draenei_female;
            case 22: return isMale ? R.drawable.worgen_male : R.drawable.worgen_female;
            default:
                return 0;
        }
    }

    public int GetClassResourceID(int index)
    {
        switch (CharClasses[index]) {
            case 1: return R.drawable.warrior;
            case 2: return R.drawable.paladin;
            case 3: return R.drawable.hunter;
            case 4: return R.drawable.rogue;
            case 5: return R.drawable.priest;
            case 6: return R.drawable.dk;
            case 7: return R.drawable.shaman;
            case 8: return R.drawable.mage;
            case 9: return R.drawable.warlock;
            case 11: return R.drawable.druid;
            default: // Should never happen
                return 0;
        }
    }

    public String GetDescription(int index)
    {
        return String.format(G.GetLocalizedString(R.string.char_enum_info_text),
            CharLevels[index], GetRaceName(CharRaces[index]), GetClassName(CharClasses[index]));
    }

    private static String GetClassName(int race) {
        switch (race) {
            case 1: return G.GetLocalizedString(R.string.warrior);
            case 2: return G.GetLocalizedString(R.string.paladin);
            case 3: return G.GetLocalizedString(R.string.hunter);
            case 4: return G.GetLocalizedString(R.string.rogue);
            case 5: return G.GetLocalizedString(R.string.priest);
            case 6: return G.GetLocalizedString(R.string.deathknight);
            case 7: return G.GetLocalizedString(R.string.shaman);
            case 8: return G.GetLocalizedString(R.string.mage);
            case 9: return G.GetLocalizedString(R.string.warlock);
            case 11: return G.GetLocalizedString(R.string.druid);
            default: // Should never happen
                return "0xB16B00B5";
        }
    }

    // TODO: account for locales (Humain / Humaine, ffs)
    private static String GetRaceName(int race) {
        switch (race) {
            case 1: return G.GetLocalizedString(R.string.human);
            case 2: return G.GetLocalizedString(R.string.orc);
            case 3: return G.GetLocalizedString(R.string.dwarf);
            case 4: return G.GetLocalizedString(R.string.nightelf);
            case 5: return G.GetLocalizedString(R.string.undead);
            case 6: return G.GetLocalizedString(R.string.tauren);
            case 7: return G.GetLocalizedString(R.string.gnome);
            case 8: return G.GetLocalizedString(R.string.troll);
            case 9: return G.GetLocalizedString(R.string.goblin);
            case 10: return G.GetLocalizedString(R.string.bloodelf);
            case 11: return G.GetLocalizedString(R.string.draenei);
            case 22: return G.GetLocalizedString(R.string.worgen);
            default:
                return "0xCAFEBABE";
        }
    }
}