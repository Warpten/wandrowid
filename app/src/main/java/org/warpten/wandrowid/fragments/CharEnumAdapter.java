package org.warpten.wandrowid.fragments;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.R;

public class CharEnumAdapter extends ArrayAdapter<String> {
    private final Activity context;

    public CharEnumAdapter(Activity context)
    {
        super(context, R.layout.char_enum_single_fragment, G.CharacterData.CharNames);
        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.char_enum_single_fragment, null, true);

        CharEnumStruct charData = G.CharacterData;

        ((TextView)rowView.findViewById(R.id.nameText)).
                setText(charData.CharNames[position]);
        ((TextView)rowView.findViewById(R.id.levelText)).
                setText(charData.GetDescription(position));
        ((ImageView)rowView.findViewById(R.id.factionImage)).
                setImageResource(charData.GetFactionResourceID(position));
        ((ImageView)rowView.findViewById(R.id.classImage)).
                setImageResource(charData.GetClassResourceID(position));
        ((ImageView)rowView.findViewById(R.id.raceImage)).
                setImageResource(charData.GetRaceResourceID(position));

        // TODO Complete

        return rowView;
    }
}
