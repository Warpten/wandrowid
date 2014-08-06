package org.warpten.wandrowid.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.warpten.wandrowid.OnAuthFragmentListener;
import org.warpten.wandrowid.R;
import org.warpten.wandrowid.network.AuthPacket;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link org.warpten.wandrowid.OnAuthFragmentListener} interface
 * to handle interaction events.
 * Use the {@link RealmlistFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class RealmlistFragment extends Fragment {
    private RealmlistStruct realmInfoList;
    private Activity mActivity;

    private ListView realmListView;
    private RealmlistAdapter adapter;

    private OnAuthFragmentListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param realmInfos An instance of AuthPacket.
     * @return A new instance of fragment RealmlistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RealmlistFragment newInstance(AuthPacket realmInfos) {
        RealmlistFragment fragment = new RealmlistFragment();

        Bundle args = new Bundle();
        args.putParcelable("realmData", new RealmlistStruct(realmInfos));
        fragment.setArguments(args);

        return fragment;
    }
    public RealmlistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            realmInfoList = getArguments().getParcelable("realmData");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_realm_selector, container, false);

        realmListView = (ListView)view.findViewById(R.id.realmlistListView);
        realmListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String realmName = realmInfoList.realmNames.get(i);
                String realmAddress = realmInfoList.realmAddresses.get(i);
                int realmPort = realmInfoList.realmPorts.get(i);
                mListener.OnRealmSelected(realmName, realmAddress, realmPort);
            }
        });

        // Populate data
        adapter = new RealmlistAdapter(mActivity, realmInfoList);
        realmListView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAuthFragmentListener)activity;
            mActivity = activity;
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

    /**
     * Updates the realm data and informs the ListView that its data set has changed.
     * @param packet The Realmlist packet.
     */
    public void UpdateData(AuthPacket packet)
    {
        adapter.UpdateData(packet);
        ((BaseAdapter)realmListView.getAdapter()).notifyDataSetChanged();
    }
}
