package org.warpten.wandrowid.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.preference.PreferenceFragment;

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

    private static final String DUMP_PACKETS_KEY = "pref_log_pkt";
    private static final String TIMESTAMP_CHAT   = "msg_timestamp";

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
        if (key.equals(DUMP_PACKETS_KEY))
            PacketLogger.Enabled = settings.getBoolean(key, false);
        // else if (key.equals(TIMESTAMP_CHAT))
        //     G.MarkMessageTime = settings.getBoolean(key, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences
        addPreferencesFromResource(R.xml.pref_app);
    }
}
