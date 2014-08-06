package org.warpten.wandrowid.fragments;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.commonsware.cwac.pager.PageDescriptor;

/**
 * Created by perquet on 27/07/14.
 */
public class ChatWindowDescriptor implements PageDescriptor {
    private Bundle data = new Bundle();

    public ChatWindowDescriptor(int messageType, String... args)
    {
        data.putInt("messageType", messageType);
        for (int i = 0; i < args.length; ++i)
            data.putString("arg" + i, args[i]);
    }

    public int GetType() { return data.getInt("messageType"); }

    private ChatWindowDescriptor(Bundle data)
    {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getTitle() {
        return data.getString("arg0"); // Window title is always param 0
    }

    @Override
    public String getFragmentTag() {
        return data.toString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBundle(data);
    }

    public static final Parcelable.Creator<ChatWindowDescriptor> CREATOR = new Creator<ChatWindowDescriptor>() {
        @Override
        public ChatWindowDescriptor createFromParcel(Parcel parcel) {
            return new ChatWindowDescriptor(parcel.readBundle());
        }

        @Override
        public ChatWindowDescriptor[] newArray(int i) {
            return new ChatWindowDescriptor[i];
        }
    };
}
