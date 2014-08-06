package org.warpten.wandrowid.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.R;

public class ChatWindowFragment extends Fragment {
    public String FrameName;
    public int MessageType;
    private TextView messageWindow;
    private ScrollView scroller;
    private ChatWindowDescriptor descriptor;
    public String contentBuffer = null;

    public static ChatWindowFragment initFromState(ChatWindowDescriptor description)
    {
        ChatWindowFragment fragment = new ChatWindowFragment();
        Bundle args = new Bundle();
        args.putParcelable("dataBundle", description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getString("contents") != null) {
            contentBuffer = savedInstanceState.getString("contents");
            int crlf = contentBuffer.lastIndexOf('\n');
            if (crlf != -1)
                contentBuffer = contentBuffer.substring(crlf);
        }

        if (getArguments() != null) {
            descriptor = getArguments().getParcelable("dataBundle");
            MessageType = descriptor.GetType();
            FrameName = descriptor.getTitle();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_window, container, false);
        messageWindow = (TextView)view.findViewById(R.id.chatwindow);
        scroller = (ScrollView)view.findViewById(R.id.scrollView);
        switch (MessageType)
        {
            case ChatMessageType.Whisper:
                messageWindow.setTextColor(Color.rgb(255, 128, 255));
                break;
            case ChatMessageType.Channel:
                messageWindow.setTextColor(Color.rgb(255, 192, 192));
                break;
            case ChatMessageType.Party:
                messageWindow.setTextColor(Color.rgb(170, 170, 255));
                break;
            case ChatMessageType.PartyLeader:
                messageWindow.setTextColor(Color.rgb(118, 200, 255));
                break;
            case ChatMessageType.Raid:
                messageWindow.setTextColor(Color.rgb(255, 172, 0));
                break;
            case ChatMessageType.RaidLeader:
                messageWindow.setTextColor(Color.rgb(255, 72, 9));
                break;
            case ChatMessageType.RaidWarning:
                messageWindow.setTextColor(Color.rgb(255, 72, 0));
                break;
            case ChatMessageType.Guild:
                messageWindow.setTextColor(Color.rgb(64, 255, 64));
                break;
            default:
                messageWindow.setTextColor(Color.WHITE);
                break;
        }

        if (contentBuffer != null)
            messageWindow.setText(contentBuffer);
        contentBuffer = null;

        return view;
    }

    public void UpdateData(Bundle data)
    {
        String result = null;
        switch (MessageType) {
            case ChatMessageType.Whisper:
            case ChatMessageType.WhisperInform:
                String whoSaid = (data.getInt("chatMessageType") == ChatMessageType.Whisper
                        ? "You said" : "Peer said:");
                if (!G.GetBooleanSetting("msg_timestamp", false))
                    result = String.format("%n%s %s", whoSaid, data.getString("message"));
                else
                    result = String.format("%n[%s] %s %s",
                            DateFormat.format("HH:mm:ss", data.getLong("timestamp")),
                            whoSaid, data.getString("message"));
                break;
            default:
                if (!G.GetBooleanSetting("msg_timestamp", false))
                    result = String.format("%n(%s) %s", data.getString("senderName"), data.getString("message"));
                else
                    result = String.format("%n[%s] (%s) %s",
                            DateFormat.format("HH:mm:ss", data.getLong("timestamp")),
                            data.getString("senderName"), data.getString("message"));
                break;
        }
        messageWindow.append(result);
        scroller.fullScroll(View.FOCUS_DOWN);
    }
}
