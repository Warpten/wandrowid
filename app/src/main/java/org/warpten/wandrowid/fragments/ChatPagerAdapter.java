package org.warpten.wandrowid.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.v4.ArrayPagerAdapter;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.R;

import java.util.ArrayList;

// Future: use the RetentionStrategy flavor (NYI in api)
public class ChatPagerAdapter extends ArrayPagerAdapter<ChatWindowFragment> {
    private ViewPager pager;

    public ChatPagerAdapter(ViewPager mPager, FragmentManager fm, ArrayList<PageDescriptor> descriptors) {
        super(fm, descriptors, NEVERDESTROY);
        this.pager = mPager;
    }

    @Override
    protected ChatWindowFragment createFragment(PageDescriptor desc) {
        return ChatWindowFragment.initFromState((ChatWindowDescriptor) desc);
    }

    /*
     * Bundle data Message data object
     * Forwards the bundle to the appropriate fragment window
     */
    public void HandleIncomingPacket(Bundle data) {
        long thisGuid = G.GetGuidForActiveCharacter();

        long receiverGuid = data.getLong("receiverGuid");
        long senderGuid = data.getLong("senderGuid");
        String channelName = data.getString("channelName");
        int messageType = data.getInt("chatMessageType");

        //! Code duplicated for simplicity of brain processing!
        for (int i = 0; i < getCount(); ++i) {
            ChatWindowFragment fragment = getExistingFragment(i);
            switch (fragment.MessageType)
            {
                // Also contains WHISPER_INFORM (basically server sending back OUR message)
                case ChatMessageType.Whisper:
                    if (!(receiverGuid == thisGuid || senderGuid == thisGuid))
                        continue;

                    if (messageType != ChatMessageType.Whisper &&
                        messageType != ChatMessageType.WhisperInform)
                        continue;

                    fragment.UpdateData(data);
                    return;
                case ChatMessageType.Guild:
                    if (messageType != ChatMessageType.Guild)
                        continue;

                    fragment.UpdateData(data);
                    return;
                case ChatMessageType.Channel:
                    if (fragment.MessageType != messageType ||
                        !channelName.equals(fragment.FrameName))
                        continue;

                    fragment.UpdateData(data);
                    return;
                case ChatMessageType.Say:
                    if (messageType != ChatMessageType.Yell &&
                        messageType != ChatMessageType.Say)
                        continue;

                    fragment.UpdateData(data);
                    return;
            }
        }

        // Fragment not found, let's create a new one with the required attributes
        String peerName = G.CharacterCache.get(senderGuid);
        if (senderGuid == G.GetGuidForActiveCharacter())
            peerName = G.CharacterCache.get(receiverGuid);

        switch (messageType) {
            case ChatMessageType.Whisper:
            case ChatMessageType.WhisperInform:
                AddPage(ChatMessageType.Whisper, peerName);
                HandleIncomingPacket(data); // Update again
                break;
            case ChatMessageType.Channel:
                AddPage(messageType, channelName);
                HandleIncomingPacket(data); // Update again
                break;
            case ChatMessageType.Guild:
                AddPage(messageType, G.GetLocalizedString(R.string.channel_guild));
                HandleIncomingPacket(data); // Update again
                break;
            case ChatMessageType.Say:
            case ChatMessageType.Yell:
                AddPage(ChatMessageType.Say, G.GetLocalizedString(R.string.channel_local));
                HandleIncomingPacket(data); // Update again
                break;
        }
    }

    public void AddPage(int messageType, String... args)
    {
        ChatWindowDescriptor descriptor = new ChatWindowDescriptor(messageType, args);
        int currentPageIndex = this.pager.getCurrentItem();
        if (currentPageIndex < getCount() - 1)
            insert(descriptor, currentPageIndex + 1);
        else
            add(descriptor);
    }

    public void RemovePage(int position)
    {
        if (getCount() > 1)
            remove(position);
    }

    public void RemoveCurrentPage()
    {
        if (getCurrentFragment().MessageType == ChatMessageType.Guild)
            return;

        G.WorldSocket().opcodeHandlers.SendLeaveChannel(0, getCurrentFragment().FrameName);
        remove(this.pager.getCurrentItem());
    }

    public void SetCurrent(int position) {
        if (position > 0 && position < getCount())
            this.pager.setCurrentItem(position);
    }

    // This is still a hack - I should figure out a nice retention strategy that
    // lets me reinstanciate fragments and repopulate their TextView.
    public static final RetentionStrategy NEVERDESTROY = new RetentionStrategy()
    {
        @Override
        public void attach(Fragment fragment, FragmentTransaction currTransaction) {
            currTransaction.attach(fragment);
        }

        @Override
        public void detach(Fragment fragment, FragmentTransaction fragmentTransaction) {
            // Do nothing
        }
    };
}
