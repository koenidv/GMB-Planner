package com.koenidv.gmbplanner.ui.main;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.koenidv.gmbplanner.Change;
import com.koenidv.gmbplanner.ChangesAdapter;
import com.koenidv.gmbplanner.CoursesSheet;
import com.koenidv.gmbplanner.Lesson;
import com.koenidv.gmbplanner.LessonsAdapter;
import com.koenidv.gmbplanner.LessonsCompactAdapter;
import com.koenidv.gmbplanner.R;
import com.koenidv.gmbplanner.Resolver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.koenidv.gmbplanner.MainActivity.coursesSheet;

//  Created by koenidv on 15.02.2020.
public class MyChangesFragment extends Fragment {

    private View mView;
    // Refresh the list of changes whenever the broadcast "changesRefreshed" is received
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshList();
        }
    };
    private BroadcastReceiver mFailedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(Objects.requireNonNull(getView()).findViewById(R.id.constraintLayout), R.string.error_offline, Snackbar.LENGTH_LONG).show();
        }
    };

    private LessonsCompactAdapter todayAdapter;
    private LessonsAdapter mondayAdapter, tuesdayAdapter, wednesdayAdapter, thursdayAdapter, fridayAdapter;
    private Lesson[][][] timetable;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        // Timetable
        timetable = (new Gson()).fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
        view.findViewById(R.id.include).setVisibility(View.VISIBLE);

        final RecyclerView todayRecycler = view.findViewById(R.id.todayRecycler);
        LinearLayoutManager todayLayoutManager = new LinearLayoutManager(getContext());
        todayLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        todayRecycler.setLayoutManager(todayLayoutManager);
        int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
        if (weekDay < 0 || weekDay > 4) weekDay = 0;
        todayAdapter = new LessonsCompactAdapter(timetable[weekDay]);
        todayRecycler.setAdapter(todayAdapter);

        final LinearLayout recyclerLayout = view.findViewById(R.id.recyclerLayout), todayLayout = view.findViewById(R.id.compactLayout);
        final TextView titleTextView = view.findViewById(R.id.titleTextView);
        final ImageButton expandButton = view.findViewById(R.id.expandButton);

        expandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandButton.setVisibility(View.GONE);
                if (recyclerLayout.getVisibility() == View.GONE) {
                    todayRecycler.setVisibility(View.GONE);
                    titleTextView.setVisibility(View.VISIBLE);
                    recyclerLayout.setVisibility(View.VISIBLE);
                    expandButton.setImageResource(R.drawable.ic_less);
                } else {
                    recyclerLayout.setVisibility(View.GONE);
                    titleTextView.setVisibility(View.GONE);
                    todayRecycler.setVisibility(View.VISIBLE);
                    expandButton.setImageResource(R.drawable.ic_more);
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        expandButton.setVisibility(View.VISIBLE);
                    }
                }, getResources().getInteger(android.R.integer.config_shortAnimTime));
            }
        });

        RecyclerView mondayRecycler = view.findViewById(R.id.mondayRecycler),
                tuesdayRecycler = view.findViewById(R.id.tuesdayRecycler),
                wednesdayRecycler = view.findViewById(R.id.wednesdayRecycler),
                thursdayRecycler = view.findViewById(R.id.thursdayRecycler),
                fridayRecycler = view.findViewById(R.id.fridayRecycler);
        mondayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        tuesdayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        wednesdayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        thursdayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        fridayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mondayAdapter = new LessonsAdapter(timetable[0]);
        tuesdayAdapter = new LessonsAdapter(timetable[1]);
        wednesdayAdapter = new LessonsAdapter(timetable[2]);
        thursdayAdapter = new LessonsAdapter(timetable[3]);
        fridayAdapter = new LessonsAdapter(timetable[4]);
        mondayRecycler.setAdapter(mondayAdapter);
        tuesdayRecycler.setAdapter(tuesdayAdapter);
        wednesdayRecycler.setAdapter(wednesdayAdapter);
        thursdayRecycler.setAdapter(thursdayAdapter);
        fridayRecycler.setAdapter(fridayAdapter);


        refreshList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register to receive messages.
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).registerReceiver(mMessageReceiver,
                new IntentFilter("changesRefreshed"));
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).registerReceiver(mFailedReceiver,
                new IntentFilter("refreshFailed"));
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
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getActivity())).unregisterReceiver(mFailedReceiver);
        super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    private void refreshList() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        List<Change> everyChangeList;
        ArrayList<Change> myChangeList = new ArrayList<>();

        Type listType = new TypeToken<ArrayList<Change>>() {
        }.getType();
        everyChangeList = gson.fromJson(prefs.getString("changes", ""), listType);

        if (everyChangeList != null) {
            for (Change change : everyChangeList) {
                // Convert to string so that the list can contain a note about the teacher (eg course (teacher))
                if ((new Resolver()).isFavorite(change.getCourse(), getActivity()))
                    myChangeList.add(change);
            }
        }

        RecyclerView myChangesRecycler = mView.findViewById(R.id.myChangesRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        myChangesRecycler.setLayoutManager(layoutManager);

        ((ViewGroup) myChangesRecycler.getParent()).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        ChangesAdapter mAdapter = new ChangesAdapter(myChangeList, true, prefs.getBoolean("compactModeFavorite", false));
        myChangesRecycler.setAdapter(mAdapter);

        if (myChangeList.isEmpty()) {
            TextView emptyTextView = mView.findViewById(R.id.noContentTextView);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText(R.string.nocontent_mine);
            if (prefs.getBoolean("sveaEE", false))
                emptyTextView.append(" :(");

            if (prefs.getString("myCourses", "").length() <= 2) {
                emptyTextView.setText(R.string.nocontent_mine_nocourses);
                mView.findViewById(R.id.addCoursesButton).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.addCoursesButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        coursesSheet = new CoursesSheet();
                        coursesSheet.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "coursesSheet");
                    }
                });
            } else {
                mView.findViewById(R.id.addCoursesButton).setVisibility(View.GONE);
            }
        } else {
            mView.findViewById(R.id.noContentTextView).setVisibility(View.GONE);
            mView.findViewById(R.id.addCoursesButton).setVisibility(View.GONE);
        }

        timetable = (new Gson()).fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
        todayAdapter.notifyDataSetChanged();
        mondayAdapter.notifyDataSetChanged();
        tuesdayAdapter.notifyDataSetChanged();
        wednesdayAdapter.notifyDataSetChanged();
        thursdayAdapter.notifyDataSetChanged();
        fridayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        timetable = (new Gson()).fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
        todayAdapter.notifyDataSetChanged();
        super.onResume();
    }
}
