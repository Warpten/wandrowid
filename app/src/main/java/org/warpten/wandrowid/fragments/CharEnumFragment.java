package org.warpten.wandrowid.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.warpten.wandrowid.G;
import org.warpten.wandrowid.OnCharEnumFragmentListener;
import org.warpten.wandrowid.R;

public class CharEnumFragment extends Fragment {
    private OnCharEnumFragmentListener mListener;
    private Activity mActivity;

    private ListView charEnumView;
    private CharEnumAdapter adapter;

    public CharEnumFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View thisView  = inflater.inflate(R.layout.fragment_char_enum, container, false);

        charEnumView = (ListView)thisView.findViewById(R.id.charEnumListView);
        charEnumView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String charName = G.CharacterData.CharNames[i];
                byte[] charGuid = G.CharacterData.CharGuids[i];
                G.CurrentCharacterIndex = i;
                mListener.OnCharacterSelected(charName, charGuid);
            }
        });

        adapter = new CharEnumAdapter(mActivity);
        charEnumView.setAdapter(adapter);

        return thisView;
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
        try {
            mListener = (OnCharEnumFragmentListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAuthFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
