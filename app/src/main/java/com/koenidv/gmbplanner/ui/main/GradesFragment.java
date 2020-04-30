package com.koenidv.gmbplanner.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.koenidv.gmbplanner.Course;
import com.koenidv.gmbplanner.ListType;
import com.koenidv.gmbplanner.R;
import com.koenidv.gmbplanner.Resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.futured.donut.DonutDataset;
import app.futured.donut.DonutProgressView;

//  Created by koenidv on 15.02.2020.
public class GradesFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        Resolver resolver = new Resolver();

        List<DonutDataset> donutSet = new ArrayList<>();

        // Get all courses
        Map<String, Course> courses = new Gson().fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);

        if (courses != null) {
            for (Map.Entry<String, Course> map : courses.entrySet()) {
                // For all favorite courses
                if (resolver.isFavorite(map.getKey(), getContext())) {
                    if (map.getValue().getGradeAverage() != null) {
                        donutSet.add(new DonutDataset(resolver.resolveCourse(map.getKey(), getContext()),
                                resolver.resolveCourseColor(map.getKey(), getContext()),
                                map.getValue().getGradeAverage()));
                    }
                }
            }
        }

        Collections.sort(donutSet, (o1, o2) -> Float.compare(o2.getAmount(), o1.getAmount()));


        ((DonutProgressView) view.findViewById(R.id.donut)).setCap(donutSet.size() * 15);
        ((DonutProgressView) view.findViewById(R.id.donut)).submitData(donutSet);

    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grades, container, false);
    }
}
