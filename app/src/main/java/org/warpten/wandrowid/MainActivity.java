package org.warpten.wandrowid;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.warpten.wandrowid.fragments.CharEnumFragment;
import org.warpten.wandrowid.fragments.ChatMessageType;
import org.warpten.wandrowid.fragments.ChatPagerFragment;
import org.warpten.wandrowid.fragments.RealmlistFragment;
import org.warpten.wandrowid.fragments.SettingsFragment;
import org.warpten.wandrowid.network.AuthPacket;

public class MainActivity extends FragmentActivity implements OnAuthFragmentListener, OnCharEnumFragmentListener {
    // Current fragment on screen
    private Fragment currentFragment = null;

    // This is a safe constant access to the chat fragment so that messages can still be processed
    // It's set once authed
    private ChatPagerFragment chatWindow = null;

    private Handler InterfaceHandler = new Handler() {
        public void handleMessage(Message msg)
        {
            switch (msg.arg2) // Command Identifier
            {
                case 1: // Updating Realmlist data
                {
                    RealmlistFragment fragment = (RealmlistFragment)getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_realm_selector);
                    AuthPacket realmPacket = (AuthPacket) msg.obj;
                    if (fragment != null) { // Updating display
                        fragment.UpdateData(realmPacket);
                    } else {
                        ReplaceFragment(RealmlistFragment.newInstance(realmPacket), TAG_REALMLISTFRAGMENT);
                        setTitle(R.string.realm_list_title);
                    }
                    break;
                }
                case 2: // Auth progress display
                {
                    ProgressBar progressBar = ((ProgressBar)findViewById(R.id.authProgressBar));
                    if (progressBar != null)
                        progressBar.setProgress(msg.arg1);

                    String statusText = (String) msg.obj;
                    if (statusText == null)
                        statusText = "";

                    TextView t = (TextView)findViewById(R.id.authProgressInfo);
                    if (t != null)
                        t.setText(statusText);
                    break;
                }
                case 3: // Display char enum
                    ReplaceFragment(new CharEnumFragment(), TAG_CHARENUMFRAGMENT);
                    setTitle(R.string.char_enum_title);
                    break;
                case 4: // New chat message
                    for (Bundle data : G.ReadyMessageQueue)
                        chatWindow.HandleDataBundle(data);
                    G.ReadyMessageQueue.clear();
                    break;
                case 5: // Channel joined
                    OnJoinedChannel((String)msg.obj);
                    break;
            }
        }
    };

    public static final String TAG_AUTHFRAGMENT      = "AuthFragment";
    public static final String TAG_SETTINGSFRAGMENT  = "SettingsFragment";
    public static final String TAG_JOINCHANNELDIALOG = "JoinChannelDialog";
    public static final String TAG_WHISPERDIALOG     = "WhisperDialog";
    public static final String TAG_CHATPAGERFRAGMENT = "ChatPagerFragment";
    public static final String TAG_CHARENUMFRAGMENT  = "CharEnumFragment";
    public static final String TAG_REALMLISTFRAGMENT = "RealmlistFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable settings loading
        G.Preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Enable localization
        G.Context = this;

        setContentView(R.layout.activity_main);

        // Do not re-inset AuthFragment on orientation change if not on this screen.
        if (savedInstanceState == null)
            ReplaceFragment(new AuthFragment(), TAG_AUTHFRAGMENT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (chatWindow == null) {
            menu.findItem(R.id.action_join_channel).setVisible(false);
            menu.findItem(R.id.action_whisper).setVisible(false);
            menu.findItem(R.id.action_leave_channel).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            ReplaceFragmentAndBackstack(new SettingsFragment(), TAG_SETTINGSFRAGMENT);
            return true;
        } else if (id == R.id.action_join_channel) {
            JoinChannelDialog dialog = new JoinChannelDialog();
            dialog.show(getFragmentManager(), TAG_JOINCHANNELDIALOG);
            return true;
        } else if (id == R.id.action_leave_channel) {
            chatWindow.mAdapter.RemoveCurrentPage();
            return true;
        } else if (id == R.id.action_whisper) {
            WhisperDialog dialog = new WhisperDialog();
            dialog.show(getFragmentManager(), TAG_WHISPERDIALOG);
        }
        return super.onOptionsItemSelected(item);
    }

    private static boolean IsConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (networkInfo == null || !networkInfo.isAvailable() || !networkInfo.isConnected())
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        }
        return networkInfo == null ? false : networkInfo.isConnected();
    }

    public void OnConnexionRequest(View view) {
        if (!IsConnected(this)) {
            MakeToast(R.string.error_enable_device_network, false);
            return;
        }

        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.fragment_main, null, false);

        G.RealmName = ((EditText)findViewById(R.id.realmAddressBox)).getText().toString();
        G.Username = ((EditText)findViewById(R.id.userNameBox)).getText().toString();
        G.Password = ((EditText)findViewById(R.id.passwordBox)).getText().toString();

        RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup);
        int gameVersion = group.indexOfChild(findViewById(group.getCheckedRadioButtonId()));

        switch (gameVersion)
        {
            case 1: // Cataclysm
                G.Cataclysm();
                if (G.Username.contains("@")) { // Battle.NET
                    MakeToast(R.string.error_no_battlenet, true);
                    break;
                }
                // no break
            case 0: // WoTLK
                if (gameVersion == 0) // Needed due to absense of break
                    G.WoTLK();
                try {
                    G.Socket = new GruntSocket(InterfaceHandler);
                    G.Socket.connect();
                } catch (Exception e) {
                    G.Socket.close();
                    MakeToast(R.string.error_auth_unk, false);
                }
                break;
            case 2: // Retail
                MakeToast(R.string.error_nyi, true);
                break;
            default:
                MakeToast(R.string.error_select_version, true);
                break;
        }
    }

    public void OnRealmListRefreshRequest(View view)
    {
        G.Socket.SendRealmList();
    }

    public void MakeToast(int msg, boolean shortToast)
    {
        MakeToast(G.GetLocalizedString(msg), shortToast);
    }

    public void MakeToast(String msg, boolean shortToast)
    {
        Toast.makeText(this, msg, shortToast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

    /**
     * This function is called from the activity once a realm has been selected.
     */
    public void OnRealmSelected(String realmName, String address, int port)
    {
        G.RealmAddress = address;
        G.RealmPort = port;
        MakeToast(String.format(G.GetLocalizedString(R.string.info_connecting_to_realm), realmName), true);
        G.Socket.close();
        G.Socket = null;
        G.Socket = new WorldSocket(InterfaceHandler);
        G.Socket.connect();
    }

    public void OnCharacterSelected(String charName, byte[] charGuid)
    {
        ((WorldSocket) G.Socket).opcodeHandlers.SendPlayerLogin(charName, charGuid);
        chatWindow = new ChatPagerFragment();
        ReplaceFragment(chatWindow, TAG_CHATPAGERFRAGMENT);
        invalidateOptionsMenu(); // re-trigger onCreateOptionsMenu
    }

    public void OnChatMessage(View view) {
        String message = ((EditText)findViewById(R.id.chatTextInputBox)).getText().toString();
        if (chatWindow.OnMessageChat(message)) // If sent, clear input
            ((EditText)findViewById(R.id.chatTextInputBox)).setText(R.string.emptystring);
    }

    public void OnJoinedChannel(String channelName) {
        // Create a frame for this new message
        chatWindow.mAdapter.AddPage(ChatMessageType.Channel, channelName);
        G.JoinedChannels.add(channelName);
    }

    /**
     * A class that binds the login form fragment to the activity.
     */
    public static class AuthFragment extends Fragment {
        public AuthFragment() { }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_main, container, false);
            if (G.GetBooleanSetting(SettingsFragment.SETTING_AUTOJOINTOGGLE, false)) {
                ((EditText)view.findViewById(R.id.realmAddressBox)).setText(G.GetStringSetting(SettingsFragment.SETTING_AUTOJOINREALM));
                ((EditText)view.findViewById(R.id.userNameBox)).setText(G.GetStringSetting(SettingsFragment.SETTING_AUTOJOINUSER));
                ((EditText)view.findViewById(R.id.passwordBox)).setText(G.GetStringSetting(SettingsFragment.SETTING_AUTOJOINPASSWORD));
                String realmVerString = G.GetStringSetting(SettingsFragment.SETTING_AUTOJOINREALMVER);
                if (realmVerString.equals("12340"))
                    ((RadioButton)view.findViewById(R.id.wotlkButton)).setChecked(true);
                else if (realmVerString.equals("15595"))
                    ((RadioButton)view.findViewById(R.id.cataclysmButton)).setChecked(true);
            }
            return view;
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0)
            fm.popBackStack();
        // Don't super
    }

    public void ReplaceFragment(Fragment fragment, String tag) {
        // Not sure if needed, just in case
        if (currentFragment != null)
            getSupportFragmentManager().beginTransaction()
                    .remove(currentFragment)
                    .replace(R.id.container, fragment, tag)
                    .commit();
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment, tag)
                    .commit();
        currentFragment = fragment;
    }

    public void ReplaceFragmentAndBackstack(Fragment fragment, String tag)
    {
        // Not sure if needed, just in case
        if (currentFragment != null)
            getSupportFragmentManager().beginTransaction()
                    .remove(currentFragment)
                    .replace(R.id.container, fragment, tag)
                    .addToBackStack(null)
                    .commit();
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment, tag)
                    .addToBackStack(null)
                    .commit();
        currentFragment = fragment;
    }
}
