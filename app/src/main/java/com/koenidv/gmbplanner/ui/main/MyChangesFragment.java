package com.koenidv.gmbplanner.ui.main;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.PaintDrawable;
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
import java.util.Arrays;
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
            refreshTimetable();
        }
    };
    private BroadcastReceiver mFailedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(Objects.requireNonNull(getView()).findViewById(R.id.constraintLayout), R.string.error_offline, Snackbar.LENGTH_LONG).show();
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        refreshList();
        refreshTimetable();
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
        // Unregister since the activity is about to be closed
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

        // Get all changes from sharedPrefs
        Type listType = new TypeToken<ArrayList<Change>>() {
        }.getType();
        everyChangeList = gson.fromJson(prefs.getString("changes", ""), listType);

        // Filter favorite courses
        if (everyChangeList != null) {
            for (Change change : everyChangeList) {
                // Convert to string so that the list can contain a note about the teacher (eg course (teacher))
                if ((new Resolver()).isFavorite(change.getCourseString(), getActivity()))
                    myChangeList.add(change);
            }
        }

        // Set up changes recycler
        RecyclerView myChangesRecycler = mView.findViewById(R.id.recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        myChangesRecycler.setLayoutManager(layoutManager);

        // Enable transition for expanding the timetable
        ((ViewGroup) myChangesRecycler.getParent()).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        ChangesAdapter mAdapter = new ChangesAdapter(myChangeList, true, prefs.getBoolean("compactModeFavorite", false));
        myChangesRecycler.setAdapter(mAdapter);

        if (myChangeList.isEmpty()) {
            // Show no changes info
            TextView emptyTextView = mView.findViewById(R.id.noContentTextView);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText(R.string.nocontent_mine);
            if (prefs.getBoolean("sveaEE", false))
                emptyTextView.append(" :(");

            // Show timetable
            mView.findViewById(R.id.expandButton).setVisibility(View.GONE);
            mView.findViewById(R.id.todayRecycler).setVisibility(View.GONE);
            mView.findViewById(R.id.titleTextView).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.recyclerLayout).setVisibility(View.VISIBLE);

            // Show option to add courses if none are added yet
            if (prefs.getString("myCourses", "").length() <= 2) {
                emptyTextView.setText(R.string.nocontent_mine_nocourses);
                mView.findViewById(R.id.addCoursesButton).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.addCoursesButton).setOnClickListener(v -> {
                    coursesSheet = new CoursesSheet();
                    coursesSheet.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "coursesSheet");
                });
            } else {
                mView.findViewById(R.id.addCoursesButton).setVisibility(View.GONE);
            }
        } else {
            mView.findViewById(R.id.noContentTextView).setVisibility(View.GONE);
            mView.findViewById(R.id.addCoursesButton).setVisibility(View.GONE);
        }
    }

    private void refreshTimetable() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        try {
            // Get filtered timetable
            Lesson[][][] timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);
            mView.findViewById(R.id.include).setVisibility(View.VISIBLE);

            // Hide card if timetable is empty
            // Manual check as timetable might include empty entries
            if (Arrays.deepToString(timetable)
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "")
                    .replace(" ", "")
                    .length() == 0)
                mView.findViewById(R.id.include).setVisibility(View.GONE);

            // Set up today overview
            // Show today's classes, tomorrows if after 5pm or monday's on weekends
            final RecyclerView todayRecycler = mView.findViewById(R.id.todayRecycler);
            LinearLayoutManager todayLayoutManager = new LinearLayoutManager(getContext());
            todayLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
            todayRecycler.setLayoutManager(todayLayoutManager);
            // Get today or tomorrow after 5pm
            int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
            if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 16) weekDay++;
            if (weekDay < 0 || weekDay > 4) weekDay = 0;
            LessonsCompactAdapter todayAdapter = new LessonsCompactAdapter(timetable[weekDay], weekDay);
            todayRecycler.setAdapter(todayAdapter);

            final LinearLayout recyclerLayout = mView.findViewById(R.id.recyclerLayout);
            final TextView titleTextView = mView.findViewById(R.id.titleTextView);
            final ImageButton expandButton = mView.findViewById(R.id.expandButton);

            // Show title if today overview is empty
            final boolean todayEmpty = Arrays.deepToString(timetable[weekDay])
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "")
                    .replace(" ", "")
                    .length() == 0;
            if (todayEmpty)
                titleTextView.setVisibility(View.VISIBLE);

            // Expand button to show the entire timetable
            View.OnClickListener expandListener = v -> {
                expandButton.setVisibility(View.GONE);
                if (recyclerLayout.getVisibility() == View.GONE) {
                    todayRecycler.setVisibility(View.GONE);
                    titleTextView.setVisibility(View.VISIBLE);
                    recyclerLayout.setVisibility(View.VISIBLE);
                    expandButton.setImageResource(R.drawable.ic_less);
                } else {
                    recyclerLayout.setVisibility(View.GONE);
                    if (!todayEmpty)
                        titleTextView.setVisibility(View.GONE);
                    todayRecycler.setVisibility(View.VISIBLE);
                    expandButton.setImageResource(R.drawable.ic_more);
                }
                new Handler().postDelayed(() -> expandButton.setVisibility(View.VISIBLE), getResources().getInteger(android.R.integer.config_shortAnimTime));
            };
            mView.findViewById(R.id.compactLayout).setOnClickListener(expandListener);
            expandButton.setOnClickListener(expandListener);

            // Set up 5 recyclerviews, one for each day
            RecyclerView[] dayRecyclers = {
                    mView.findViewById(R.id.mondayRecycler),
                    mView.findViewById(R.id.tuesdayRecycler),
                    mView.findViewById(R.id.wednesdayRecycler),
                    mView.findViewById(R.id.thursdayRecycler),
                    mView.findViewById(R.id.fridayRecycler)
            };
            for (int i = 0; i < dayRecyclers.length; i++) {
                RecyclerView recycler = dayRecyclers[i];
                recycler.setLayoutManager(new LinearLayoutManager(getContext()));
                recycler.setAdapter(new LessonsAdapter(timetable[i], i));
                recycler.setBackground(null);
            }

            // Mark today
            PaintDrawable todayBackground = new PaintDrawable(getResources().getColor(R.color.highlight));
            todayBackground.setCornerRadius((new Resolver()).dpToPx(8, getActivity()));
            dayRecyclers[weekDay].setBackground(todayBackground);
        } catch (NullPointerException npe) {
            // Somethings wrong with the timetable, maybe there's just no data or no favorite courses
            mView.findViewById(R.id.include).setVisibility(View.GONE);
        }
    }
}
