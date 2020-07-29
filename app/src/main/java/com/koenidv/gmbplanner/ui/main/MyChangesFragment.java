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
import com.koenidv.gmbplanner.SuggestionsProvider;

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

    private RecyclerView[] dayRecyclers = new RecyclerView[5];
    private LessonsCompactAdapter todayAdapter;
    private Lesson[][][] timetable;
    private int weekDay;
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
    public void onResume() {

        // Get today or tomorrow after 5pm
        weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 16) weekDay++;
        if (weekDay < 0 || weekDay > 4) weekDay = 0;

        // Update today overview recycler
        if (timetable != null)
            todayAdapter.setDataset(timetable[weekDay]);

        // Reset all day recycler backgrounds
        for (RecyclerView recycler : dayRecyclers) {
            recycler.setBackground(null);
        }
        // Mark todays day recycler
        PaintDrawable todayBackground = new PaintDrawable(getResources().getColor(R.color.highlight));
        todayBackground.setCornerRadius((new Resolver()).dpToPx(8, Objects.requireNonNull(getActivity())));
        dayRecyclers[weekDay].setBackground(todayBackground);

        super.onResume();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        /*
         * Set up timetable
         */

        dayRecyclers = new RecyclerView[]{
                view.findViewById(R.id.mondayRecycler),
                view.findViewById(R.id.tuesdayRecycler),
                view.findViewById(R.id.wednesdayRecycler),
                view.findViewById(R.id.thursdayRecycler),
                view.findViewById(R.id.fridayRecycler)
        };

        // Get filtered timetable
        timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);
        if (isTimetableEmpty(timetable))
            timetable = new Lesson[5][0][0];
        else
            view.findViewById(R.id.card_timetable).setVisibility(View.VISIBLE);

        // Set up 5 recyclerviews, one for each day
        for (int i = 0; i < dayRecyclers.length; i++) {
            RecyclerView recycler = dayRecyclers[i];
            recycler.setLayoutManager(new LinearLayoutManager(getContext()));
            recycler.setAdapter(new LessonsAdapter(timetable[i], i));
        }

        final LinearLayout recyclerLayout = view.findViewById(R.id.recyclerLayout);
        final TextView titleTextView = view.findViewById(R.id.titleTextView);
        final ImageButton expandButton = view.findViewById(R.id.expandButton);
        final RecyclerView todayRecycler = view.findViewById(R.id.todayRecycler);

        // Get today or tomorrow after 5pm
        weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 16) weekDay++;
        if (weekDay < 0 || weekDay > 4) weekDay = 0;

        // Set up today overview
        todayRecycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        todayAdapter = new LessonsCompactAdapter(timetable[weekDay], weekDay);
        todayRecycler.setAdapter(todayAdapter);

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
                if (todayRecycler.getAdapter() != null && todayRecycler.getAdapter().getItemCount() != 0)
                    titleTextView.setVisibility(View.GONE);
                todayRecycler.setVisibility(View.VISIBLE);
                expandButton.setImageResource(R.drawable.ic_more);
            }
            new Handler().postDelayed(() -> expandButton.setVisibility(View.VISIBLE), getResources().getInteger(android.R.integer.config_shortAnimTime));
        };
        view.findViewById(R.id.compactLayout).setOnClickListener(expandListener);
        expandButton.setOnClickListener(expandListener);

        // Enable transition for expanding the timetable
        ((ViewGroup) todayRecycler.getParent()).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        // Edit courses on long click
        View.OnLongClickListener editClickListener = v -> {
            coursesSheet = new CoursesSheet();
            coursesSheet.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "coursesSheet");
            return true;
        };
        view.findViewById(R.id.compactLayout).setOnLongClickListener(editClickListener);
        expandButton.setOnLongClickListener(editClickListener);

        // Refresh courses list
        refreshList();

        // Show suggestions
        new Handler().postDelayed(() -> {
            String suggestion = new SuggestionsProvider().provideSuggestion(getContext());
            if (suggestion != null) {
                ((TextView) view.findViewById(R.id.suggestionText)).setText(suggestion);
                view.findViewById(R.id.suggestionCard).setVisibility(View.VISIBLE);
            }
        }, 0);
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


        ChangesAdapter mAdapter = new ChangesAdapter(myChangeList, true, prefs.getBoolean("compactModeFavorite", false));
        myChangesRecycler.setAdapter(mAdapter);

        if (myChangeList.isEmpty()) {
            // Show no changes info
            TextView emptyTextView = mView.findViewById(R.id.noContentTextView);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText(R.string.nocontent_mine);
            if (prefs.getBoolean("sveaEE", false))
                emptyTextView.append(" :(");

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

            // Show timetable
            mView.findViewById(R.id.expandButton).setVisibility(View.GONE);
            mView.findViewById(R.id.todayRecycler).setVisibility(View.GONE);
            new Handler().postDelayed(() -> {
                mView.findViewById(R.id.titleTextView).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.recyclerLayout).setVisibility(View.VISIBLE);
            }, 0);

        } else {
            mView.findViewById(R.id.noContentTextView).setVisibility(View.GONE);
            mView.findViewById(R.id.addCoursesButton).setVisibility(View.GONE);
        }
    }

    private void refreshTimetable() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        try {
            // Get filtered timetable
            timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);

            // Hide card if timetable is empty
            if (isTimetableEmpty(timetable))
                mView.findViewById(R.id.card_timetable).setVisibility(View.GONE);
            else
                mView.findViewById(R.id.card_timetable).setVisibility(View.VISIBLE);

            // Update today recycler
            todayAdapter.setDataset(timetable[weekDay]);

            // Update every day recycler
            for (int i = 0; i < dayRecyclers.length; i++) {
                ((LessonsAdapter) Objects.requireNonNull(dayRecyclers[i].getAdapter())).setDataset(timetable[i]);
            }

        } catch (NullPointerException npe) {
            // Somethings wrong with the timetable, maybe there's just no data or no favorite courses
            mView.findViewById(R.id.card_timetable).setVisibility(View.GONE);
        }
    }

    private boolean isTimetableEmpty(Lesson[][][] mTimetable) {
        if (mTimetable != null)
            for (Lesson[][] day : mTimetable) {
                if (day != null)
                    for (Lesson[] period : day) {
                        if (period != null)
                            if (period.length > 0) {
                                return false;
                            }
                    }
            }
        return true;
    }
}
