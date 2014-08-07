package org.warpten.wandrowid.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.preference.PreferenceFragment;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.PacketLogger;
import org.warpten.wandrowid.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String SETTING_DUMPPKT        = "pref_log_pkt";
    public static final String SETTING_TIMESTAMP      = "msg_timestamp";
    public static final String SETTING_AUTOJOINTOGGLE = "subsettings_autojoin";
    public static final String SETTING_AUTOJOINREALM  = "autojoin_realm";
    public static final String SETTING_AUTOJOINUSER   = "autojoin_username";

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences settings, String key)
    {
        if (key.equals(SETTING_DUMPPKT))
            PacketLogger.Enabled = settings.getBoolean(key, false);
        else if (key.equals(SETTING_AUTOJOINTOGGLE)) {
            Preference pref = findPreference(key);
            pref.setSummary(settings.getBoolean(key, false) ? R.string.pref_autojoin_enabled : R.string.pref_autojoin_disabled);
        } else if (key.equals(SETTING_AUTOJOINREALM) || key.equals(SETTING_AUTOJOINUSER)) {
            Preference pref = findPreference(key);
            pref.setSummary(settings.getString(key, G.GetLocalizedString(R.string.pref_autojoin_noconfig)));
            ((CheckBoxPreference)findPreference(SETTING_AUTOJOINTOGGLE)).setChecked(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences
        addPreferencesFromResource(R.xml.pref_app);
    }
}
