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
                        ReplaceFragment(RealmlistFragment.newInstance(realmPacket), "RealmlistFragment");
                        setTitle("Select a realm to log into.");
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
                    ReplaceFragment(new CharEnumFragment(), "CharEnumFragment");
                    setTitle("Select a character to log into");
                    break;
                case 4: // New chat message
                {
                    for (Bundle data : G.ReadyMessageQueue)
                        chatWindow.HandleDataBundle(data);
                    G.ReadyMessageQueue.clear();
                    break;
                }
                case 5: // Channel joined
                {
                    OnJoinedChannel((String)msg.obj);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        G.Preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);
        ReplaceFragment(new AuthFragment(), "AuthFragment");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (chatWindow == null) {
            menu.findItem(R.id.action_join_channel).setVisible(false);
            menu.findItem(R.id.action_whisper).setVisible(false);
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
            ReplaceFragmentAndBackstack(new SettingsFragment(), "SettingsFragment");
            return true;
        } else if (id == R.id.action_join_channel) {
            JoinChannelDialog dialog = new JoinChannelDialog();
            dialog.show(getFragmentManager(), "JoinChannelDialog");
            return true;
        } else if (id == R.id.action_leave_channel) {
            chatWindow.mAdapter.RemoveCurrentPage();
            return true;
        } else if (id == R.id.action_whisper) {
            WhisperDialog dialog = new WhisperDialog();
            dialog.show(getFragmentManager(), "WhisperDialog");
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
            MakeToast("Make sure that either WiFi or 3G is enabled.", false);
            return;
        }

        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.fragment_main, null, false);

        G.RealmName = ((EditText)findViewById(R.id.realmAddressBox)).getText().toString();
        G.Username = ((EditText)findViewById(R.id.userNameBox)).getText().toString();
        G.Password = ((EditText)findViewById(R.id.passwordBox)).getText().toString(); // TODO clean this up after a while, its not needed once hash has been generated

        RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup);
        int gameVersion = group.indexOfChild(findViewById(group.getCheckedRadioButtonId()));

        switch (gameVersion)
        {
            case 1: // Cataclysm
                G.Cataclysm();
                if (G.Username.indexOf("@") != -1) { // Battle.NET
                    MakeToast("Battle.NET login is not implemented yet.", true);
                    break;
                }
                // no break
            case 0: // WoTLK
                if (gameVersion == 0)
                    G.WoTLK();
                try {
                    G.Socket = new AuthSocketGrunt(InterfaceHandler);
                    G.Socket.connect();
                } catch (Exception e) {
                    G.Socket.close();
                    MakeToast("Unknown error while connecting.", false);
                }
                break;
            case 2: // Retail
                MakeToast("Not implemented!", true);
                break;
            default:
                MakeToast("Please select a version.", true);
        }
    }

    public void OnRealmListRefreshRequest(View view)
    {
        G.Socket.SendRealmList();
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
        MakeToast("Connecting to " + realmName + ".", true);
        G.Socket.close();
        G.Socket = null;
        G.Socket = new WorldSocket(InterfaceHandler);
        G.Socket.connect();
    }

    public void OnCharacterSelected(String charName, byte[] charGuid)
    {
        ((WorldSocket) G.Socket).opcodeHandlers.SendPlayerLogin(charName, charGuid);
        chatWindow = new ChatPagerFragment();
        ReplaceFragment(chatWindow, "ChatPagerFragment");
        invalidateOptionsMenu(); // re-trigger onCreateOptionsMenu
    }

    public void OnChatMessage(View view) {
        String message = ((EditText)findViewById(R.id.chatTextInputBox)).getText().toString();
        if (chatWindow.OnMessageChat(message)) // If sent, clear input
            ((EditText)findViewById(R.id.chatTextInputBox)).setText("");
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
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0)
            fm.popBackStack();
        // Don't super
    }

    public void ReplaceFragment(Fragment fragment, String key) {
        // Not sure if needed, just in case
        if (currentFragment != null)
            getSupportFragmentManager().beginTransaction()
                    .remove(currentFragment)
                    .replace(R.id.container, fragment, key)
                    .commit();
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment, key)
                    .commit();
        currentFragment = fragment;
    }

    public void ReplaceFragmentAndBackstack(Fragment fragment, String key)
    {
        // Not sure if needed, just in case
        if (currentFragment != null)
            getSupportFragmentManager().beginTransaction()
                    .remove(currentFragment)
                    .replace(R.id.container, fragment, key)
                    .addToBackStack(null)
                    .commit();
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment, key)
                    .addToBackStack(null)
                    .commit();
        currentFragment = fragment;
    }
}
