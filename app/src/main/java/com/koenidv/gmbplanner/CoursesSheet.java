package com.koenidv.gmbplanner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;
import static com.koenidv.gmbplanner.MainActivity.myCourses;

//  Created by koenidv on 16.02.2020.
public class CoursesSheet extends BottomSheetDialogFragment {

    CoursesAdapter adapter;
    private Gson gson = new Gson();
    CoursesTimetableSheet selectorSheet;
    private Lesson[][][] timetable = new Lesson[5][][];
    private LessonsAdapter[] timetableAdapters = {
            new LessonsAdapter(timetable[0], true, 0),
            new LessonsAdapter(timetable[1], true, 1),
            new LessonsAdapter(timetable[2], true, 2),
            new LessonsAdapter(timetable[3], true, 3),
            new LessonsAdapter(timetable[4], true, 4)
    };
    private RecyclerView[] dayRecyclers;


    public CoursesSheet() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppTheme_Sheet_Transparent);
    }


    private static boolean containsAll(String check, String... keywords) {
        for (String k : keywords)
            if (!check.toLowerCase().contains(k.toLowerCase())) return false;
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_courses, container, false);
        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);
        try {
            myCourses = new ArrayList<>(Arrays.asList(gson.fromJson(prefs.getString("myCourses", ""), String[].class)));
        } catch (NullPointerException ignored) {
            // No courses added yet
        }

        // Add courses from timetable

        final LinearLayout recyclerLayout = view.findViewById(R.id.recyclerLayout);
        final TextView titleTextView = view.findViewById(R.id.titleTextView);
        final ImageButton expandButton = view.findViewById(R.id.expandButton);

        ((LinearLayout) view.findViewById(R.id.compactLayout)).setLayoutTransition(null);

        titleTextView.setText(getString(R.string.courses_edit_timetable));
        titleTextView.setVisibility(View.VISIBLE);
        view.findViewById(R.id.todayRecycler).setVisibility(View.GONE);
        expandButton.setImageResource(R.drawable.ic_less);
        recyclerLayout.setVisibility(View.VISIBLE);

        // Set up 5 recyclerviews, one for each day
        dayRecyclers = new RecyclerView[]{
                view.findViewById(R.id.mondayRecycler),
                view.findViewById(R.id.tuesdayRecycler),
                view.findViewById(R.id.wednesdayRecycler),
                view.findViewById(R.id.thursdayRecycler),
                view.findViewById(R.id.fridayRecycler)
        };
        for (int i = 0; i < dayRecyclers.length; i++) {
            RecyclerView recycler = dayRecyclers[i];
            recycler.setLayoutManager(new LinearLayoutManager(getContext()));
            recycler.setAdapter(timetableAdapters[i]);
            timetableAdapters[i].setDataset(timetable[i]);
        }

        // Mark today
        int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 16) weekDay++;
        if (weekDay < 0 || weekDay > 4) weekDay = 0;
        PaintDrawable todayBackground = new PaintDrawable(getResources().getColor(R.color.background));
        todayBackground.setCornerRadius((new Resolver()).dpToPx(8, getActivity()));
        dayRecyclers[weekDay].setBackground(todayBackground);

        // Expand button to show the entire timetable
        View.OnClickListener expandListener = v -> {
            if (recyclerLayout.getVisibility() == View.GONE) {
                recyclerLayout.setVisibility(View.VISIBLE);
                expandButton.setImageResource(R.drawable.ic_less);
            } else {
                recyclerLayout.setVisibility(View.GONE);
                expandButton.setImageResource(R.drawable.ic_more);
            }
        };
        expandButton.setOnClickListener(expandListener);
        view.findViewById(R.id.compactLayout).setOnClickListener(expandListener);


        // Course list & manually add courses

        // Set up list with all added courses

        final ImageButton recyclerExpandButton = view.findViewById(R.id.recyclerExpandButton);
        final RecyclerView coursesRecycler = view.findViewById(R.id.coursesRecycler);

        view.findViewById(R.id.recyclerExpandLayout).setOnClickListener(v -> {
            if (coursesRecycler.getVisibility() == View.GONE) {
                coursesRecycler.setVisibility(View.VISIBLE);
                recyclerExpandButton.setImageResource(R.drawable.ic_less);
            } else {
                coursesRecycler.setVisibility(View.GONE);
                recyclerExpandButton.setImageResource(R.drawable.ic_more);
            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        coursesRecycler.setLayoutManager(layoutManager);
        adapter = new CoursesAdapter(myCourses);
        coursesRecycler.setAdapter(adapter);

        // Set up suggestion chips
        Map<String, Course> courses = gson.fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);
        final ChipGroup chipgroup = view.findViewById(R.id.chipgroup);
        final TextInputEditText editText = view.findViewById(R.id.addCourseEditText);

        View.OnClickListener chipListener = v -> {
            String text = (String) v.getTag();
            myCourses.add(text);
            adapter.notifyDataSetChanged();
            refreshTimetable();
            v.setVisibility(View.GONE);
            v.setEnabled(false);
            editText.setText("");
            // Vibrate
            (new Resolver()).vibrate(getActivity());
        };
        View.OnLongClickListener chipLongListener = v -> {
            v.setVisibility(View.GONE);
            return true;
        };

        if (courses != null) {
            Resolver resolver = new Resolver();
            for (Map.Entry<String, Course> thisMap : courses.entrySet()) {
                // Only show courses that are not yet added
                if (!myCourses.toString().contains(thisMap.getValue().getCourse())) {
                    final Chip chip = new Chip(chipgroup.getContext());
                    chip.setClickable(true);
                    chip.setOnClickListener(chipListener);
                    chip.setOnLongClickListener(chipLongListener);
                    chip.setText(resolver.resolveCourse(thisMap.getValue().getCourse(), getContext(), true));
                    chip.setTag(thisMap.getValue().getCourse());
                    chip.setMinHeight(32);
                    chipgroup.addView(chip);
                }
            }
        }

        // Custom courses
        final TextInputLayout inputLayout = view.findViewById(R.id.addCourseInputLayout);
        inputLayout.setEndIconVisible(false);
        inputLayout.setEndIconOnClickListener(v -> {
            if (!Objects.requireNonNull(editText.getText()).toString().isEmpty()) {
                myCourses.add(Objects.requireNonNull(editText.getText()).toString());
                editText.setText("");
                adapter.notifyDataSetChanged();
                refreshTimetable();
                // Vibrate
                (new Resolver()).vibrate(getActivity());
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String[] filter = s.toString().split(" ");
                if (s.toString().isEmpty())
                    inputLayout.setEndIconVisible(false);
                else
                    inputLayout.setEndIconVisible(true);
                for (int i = 0; i < chipgroup.getChildCount(); i++) {
                    // Search/Filter chips
                    if (chipgroup.getChildAt(i).isEnabled()) {
                        if (s.toString().isEmpty()) {
                            chipgroup.getChildAt(i).setVisibility(View.VISIBLE);
                        } else if (containsAll(((Chip) chipgroup.getChildAt(i)).getText().toString(), filter)) {
                            chipgroup.getChildAt(i).setVisibility(View.VISIBLE);
                        } else {
                            chipgroup.getChildAt(i).setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Set up dismiss button
        view.findViewById(R.id.doneButton).setOnClickListener(v -> {
            if (!Objects.requireNonNull(editText.getText()).toString().isEmpty()) {
                myCourses.add(Objects.requireNonNull(editText.getText()).toString().toUpperCase());
                editText.setText("");
            }
            CoursesSheet.this.dismiss();
        });

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // Save changes
        final SharedPreferences.Editor prefsEdit = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE).edit();
        prefsEdit.putString("myCourses", gson.toJson(myCourses)).apply();
        // Broadcast to refresh UI
        Intent intent = new Intent("changesRefreshed");
        intent.putExtra("coursesChanged", true);
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).sendBroadcast(intent);
        super.onDismiss(dialog);
    }

    void refreshTimetable() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        Gson gson = new Gson();

        Lesson[][][] allTable = gson.fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
        ArrayList<Lesson[]> dayTable = new ArrayList<>();
        ArrayList<Lesson> periodTable = new ArrayList<>();

        if (allTable == null) {
            // Probably q34 - no data yet
            prefs.edit().putString("timetableMine", "").apply();
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("changesRefreshed"));
            return;
        }

        for (int day = 0; day <= 4; day++) {
            for (int period = 0; period < allTable[day].length; period++) {
                for (Lesson lesson : allTable[day][period]) {
                    if (myCourses.contains(lesson.getCourse())) {
                        periodTable.add(lesson);
                    }
                }
                dayTable.add(periodTable.toArray(new Lesson[0]));
                periodTable.clear();
            }
            timetable[day] = dayTable.toArray(new Lesson[0][]);
            dayTable.clear();
        }

        // Re-set adapters.. Won't work otherwise
        for (int i = 0; i < timetableAdapters.length; i++) {
            dayRecyclers[i].setAdapter(timetableAdapters[i]);
            timetableAdapters[i].setDataset(timetable[i]);
        }

        adapter.notifyDataSetChanged();
    }
}
