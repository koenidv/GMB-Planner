package com.koenidv.gmbplanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.koenidv.gmbplanner.MainActivity.myCourses;

//  Created by koenidv on 16.02.2020.
public class CoursesTimetableSheet extends BottomSheetDialogFragment {

    private Gson gson = new Gson();
    private int mDay, mPeriod;

    CoursesTimetableSheet(int day, int period) {
        mDay = day;
        mPeriod = period;
    }

    private static boolean containsAll(String check, String... keywords) {
        for (String k : keywords)
            if (!check.toLowerCase().contains(k.toLowerCase())) return false;
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_courses_timetable, container, false);

        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        // Show a recyclerview containing all courses for the selected time
        Lesson[][][] allTable = (new Gson()).fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
        List<String> newCourses = new ArrayList<>();

        for (Lesson lesson : allTable[mDay][mPeriod]) {
            if (myCourses.contains(lesson.getCourse()))
                newCourses.add(0, lesson.getCourse());
            else
                newCourses.add(lesson.getCourse());
        }

        RecyclerView recyclerView = view.findViewById(R.id.coursesRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        CoursesAdapter adapter = new CoursesAdapter(newCourses, true);
        recyclerView.setAdapter(adapter);

        // Set up dismiss button
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> dismiss());

        return view;
    }
}
