package com.washinson.yaradio2;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by User on 10.04.2018.
 */

public class TargetFragment extends Fragment {

    public TargetFragment(){}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int id;
        if(getArguments() == null) {
            return inflater.inflate(R.layout.emply, container, false);
        }
        else id = getArguments().getInt("StationID");

        View root = inflater.inflate(R.layout.expandedlist, container, false);

        String groupFrom[] = new String[] { "groupName" };
        int groupTo[] = new int[] { android.R.id.text1 };

        String childFrom[] = new String[] { "monthName" };
        int childTo[] = new int[] { android.R.id.text1 };

        final Station station = MainActivity.stations.get(id);

        Map<String, String> map;
        ArrayList<Map<String, String>> groupDataList = new ArrayList<>();
        ArrayList<ArrayList<Map<String, String>>> сhildDataList = new ArrayList<>();

        for (Station.Type group : station.types) {
            map = new HashMap<>();
            map.put("groupName", group.name);
            groupDataList.add(map);
            ArrayList<Map<String, String>> сhildDataItemList = new ArrayList<>();
            for (Station.Subtype type : group.subtypes) {
                map = new HashMap<>();
                map.put("monthName", type.name);
                сhildDataItemList.add(map);
            }
            сhildDataList.add(сhildDataItemList);
        }

        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                getActivity(), groupDataList,
                R.layout.expanded_parent, groupFrom,
                groupTo, сhildDataList, R.layout.expanded_text,
                childFrom, childTo);

        ExpandableListView expandableListView = root.findViewById(R.id.expanded_list);
        expandableListView.setAdapter(adapter);

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, final int i, final int i1, long l) {
                PlayerService.subtype = station.types.get(i).subtypes.get(i1);
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                startActivity(intent);
                return true;
            }
        });
        return root;
    }
}
