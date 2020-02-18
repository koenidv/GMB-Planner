package com.koenidv.gmbplanner.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.koenidv.gmbplanner.Change;
import com.koenidv.gmbplanner.ChangesAdapter;
import com.koenidv.gmbplanner.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class AllChangesFragment extends Fragment {

    private View mView;
    // Refresh the list of changes whenever the broadcast "changesRefreshed" is received
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshList();
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        refreshList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register to receive messages.
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).registerReceiver(mMessageReceiver,
                new IntentFilter("changesRefreshed"));
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getActivity())).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private void refreshList() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        List<Change> mChangeList;
        Type listType = new TypeToken<ArrayList<Change>>() {
        }.getType();
        mChangeList = gson.fromJson(prefs.getString("changes", ""), listType);

        RecyclerView myChangesRecycler = mView.findViewById(R.id.myChangesRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        myChangesRecycler.setLayoutManager(layoutManager);

        ChangesAdapter mAdapter = new ChangesAdapter(mChangeList, false);
        myChangesRecycler.setAdapter(mAdapter);

        if (mChangeList == null || mChangeList.isEmpty()) {
            TextView emptyTextView = mView.findViewById(R.id.noContentTextView);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText(R.string.nocontent_all);
        } else {
            mView.findViewById(R.id.noContentTextView).setVisibility(View.GONE);
        }
    }
}
