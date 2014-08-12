package org.warpten.wandrowid.fragments;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.warpten.wandrowid.R;
import org.warpten.wandrowid.network.GruntPacket;

/**
 * Custom class that is used to display realms on the realm selection screen.
 */
public class RealmlistAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final RealmlistStruct realmData;

    public RealmlistAdapter(Activity context, RealmlistStruct realminfos)
    {
        super(context, R.layout.realmlist_view_item_single, realminfos.realmNames);
        this.context = context;
        this.realmData = realminfos;
    }

    public void UpdateData(GruntPacket packet)
    {
        realmData.UpdateData(packet);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.realmlist_view_item_single, null, true);

        ((TextView)rowView.findViewById(R.id.realmSelectorName)).
            setText(this.realmData.realmNames.get(position));
        ((TextView)rowView.findViewById(R.id.realmBuildSelector)).
            setText(realmData.GetVersionString(position));

        return rowView;
    }
}
