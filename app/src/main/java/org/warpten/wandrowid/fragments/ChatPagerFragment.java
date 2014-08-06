package org.warpten.wandrowid.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.commonsware.cwac.pager.PageDescriptor;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.R;
import org.warpten.wandrowid.WorldSocket;
import org.warpten.wandrowid.handlers.Handlers;

import java.util.ArrayList;

public class ChatPagerFragment extends Fragment {

    private ViewPager mPager;
    public ChatPagerAdapter mAdapter;
    // private PagerSlidingTabStrip mStrip;

    public ChatPagerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        mPager = (ViewPager)view.findViewById(R.id.pager);

        ArrayList<PageDescriptor> descriptors = new ArrayList<PageDescriptor>();
        mAdapter = new ChatPagerAdapter(mPager, getActivity().getSupportFragmentManager(), descriptors);

        mPager.setAdapter(mAdapter);

        // Uncomment when NPE on empty pager is fixed (should gracefully ignore)
        // or make a custom scroll listener ?
        // See https://github.com/romainguefveneu/PagerSlidingTabStrip/commit/312cd015fce3428dd7771150a84b8615f4231dde#diff-f09fce23197350fb33b00423e189e98eR401
        // Aka overwrite pagerslidingtabstrip with all our might glory needs
        // mStrip = (PagerSlidingTabStrip)view.findViewById(R.id.titlestrip);
        // mStrip.setViewPager(mPager);

        PagerTabStrip strip = (PagerTabStrip)view.findViewById(R.id.titlestrip);
        strip.setTabIndicatorColor(Color.CYAN);

        if (G.IsInGuild())
            OpenGuildChatTab();

        OpenLocalChat();

        return view;
    }

    public void HandleDataBundle(Bundle data)
    {
        mAdapter.HandleIncomingPacket(data);
    }

    public boolean OnMessageChat(String message) {
        String frameName = mAdapter.getCurrentFragment().FrameName;
        Handlers handlers = ((WorldSocket) G.Socket).opcodeHandlers;
        switch (mAdapter.getCurrentFragment().MessageType) {
            case ChatMessageType.Channel:
                handlers.SendMessageChat(ChatMessageType.Channel, frameName, message);
                return true;
            case ChatMessageType.Guild:
                handlers.SendMessageChat(ChatMessageType.Guild, message);
                return true;
            case ChatMessageType.Whisper:
                handlers.SendMessageChat(ChatMessageType.Whisper, frameName, message);
                return true;
            default:
                return false;
        }
    }

    public void OpenGuildChatTab()
    {
        mAdapter.AddPage(ChatMessageType.Guild, "Guild");
    }
    public void OpenLocalChat()
    {
        mAdapter.AddPage(ChatMessageType.Say, "Local chat");
    }
}
