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
import com.koenidv.gmbplanner.Course;
import com.koenidv.gmbplanner.Grade;
import com.koenidv.gmbplanner.GradesAdapter;
import com.koenidv.gmbplanner.ListType;
import com.koenidv.gmbplanner.R;
import com.koenidv.gmbplanner.Resolver;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.futured.donut.DonutDataset;
import app.futured.donut.DonutProgressView;

//  Created by koenidv on 15.02.2020.
public class GradesFragment extends Fragment {

    private BroadcastReceiver mCourseChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("coursesChanged", false) && getView() != null)
                setup();
        }
    };
    private BroadcastReceiver mGradeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getView() != null)
                setup();
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setup();
    }

    private void setup() {
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        Resolver resolver = new Resolver();

        List<DonutDataset> donutSet = new ArrayList<>();

        // Get all courses
        Map<String, Course> courses = new Gson().fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);
        List<Grade> myGrades = new ArrayList<>();
        Float overallAverage = 0f;
        int overallCount = 0;
        Float totalValue = 0f;

        if (courses != null) {
            for (Map.Entry<String, Course> map : courses.entrySet()) {
                // For all favorite courses
                if (resolver.isFavorite(map.getKey(), getContext())) {
                    if (map.getValue().getGradeAverage() != null) {
                        donutSet.add(new DonutDataset(resolver.resolveCourse(map.getKey(), getContext()),
                                ColorUtils.setAlphaComponent(resolver.resolveCourseColor(map.getKey(), getContext()), 200),
                                map.getValue().getGradeAverage()));

                        if (map.getKey().contains("LK")) {
                            overallAverage += 2 * map.getValue().getGradeAverage();
                            overallCount += 2;
                        } else {
                            overallAverage += map.getValue().getGradeAverage();
                            overallCount++;
                        }
                        totalValue += map.getValue().getGradeAverage();
                    }
                    myGrades.add(new Grade(map.getKey(), map.getValue().getGradeAverage()));
                }
            }
        }

        Collections.sort(donutSet, (o1, o2) -> Float.compare(o2.getAmount(), o1.getAmount()));
        Collections.sort(myGrades, (o1, o2) -> {
            if (o1.getGrade() == null)
                return 1;
            else if (o2.getGrade() == null)
                return -1;
            else return Float.compare(o2.getGrade(), o1.getGrade());
        });

        ((DonutProgressView) getView().findViewById(R.id.donut)).setCap(totalValue > donutSet.size() * 14 ? totalValue : donutSet.size() * 14);
        ((DonutProgressView) getView().findViewById(R.id.donut)).submitData(donutSet);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(1);
        if (overallCount > 0)
            ((TextView) getView().findViewById(R.id.averageTextView)).setText(nf.format((17 - overallAverage / overallCount) / 3));
        else ((TextView) getView().findViewById(R.id.averageTextView)).setText("");

        RecyclerView recycler = getView().findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new GradesAdapter(myGrades));
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.registerReceiver(mCourseChangeReceiver, new IntentFilter("changesRefreshed"));
        broadcastManager.registerReceiver(mGradeChangeReceiver, new IntentFilter("gradesRefreshed"));
        return inflater.inflate(R.layout.fragment_grades, container, false);
    }
}
