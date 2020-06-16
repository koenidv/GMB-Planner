package com.koenidv.gmbplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;

//  Created by koenidv on 16.02.2020.
public class GradesSheet extends BottomSheetDialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppTheme_Sheet_Transparent);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sheet_grades, container, false);
        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        final Resolver resolver = new Resolver();
        final Gson gson = new Gson();

        Course thisCourse = resolver.getCourse(getTag(), getContext());

        if (thisCourse.getGrades().isEmpty()) {
            EditGradesSheet addSheet = new EditGradesSheet(thisCourse.getCourse(), -1);
            addSheet.show(getActivity().getSupportFragmentManager(), "new");
            dismiss();
        }

        ((ImageView) view.findViewById(R.id.iconImageView)).setColorFilter(ColorUtils.setAlphaComponent(resolver.resolveCourseColor(thisCourse.getCourse(), getContext()), 200));
        view.findViewById(R.id.addGradeFab).setBackgroundColor(ColorUtils.setAlphaComponent(resolver.resolveCourseColor(thisCourse.getCourse(), getContext()), 200));
        ((TextView) view.findViewById(R.id.courseTextView)).setText(resolver.resolveCourse(thisCourse.getCourse(), getContext()));
        if (thisCourse.getGradeAverage() == null)
            ((TextView) view.findViewById(R.id.averageGradeTextView)).setText("-");
        else {
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(2);
            ((TextView) view.findViewById(R.id.averageGradeTextView)).setText(nf.format(thisCourse.getGradeAverage()));
        }

        view.findViewById(R.id.headLayout).setOnClickListener(v -> {
            BottomSheetDialog averageSheet = new BottomSheetDialog(getContext());
            averageSheet.setContentView(R.layout.sheet_grade_average_edit);

            ((ImageView) averageSheet.findViewById(R.id.iconImageView)).setColorFilter(ColorUtils.setAlphaComponent(resolver.resolveCourseColor(thisCourse.getCourse(), getContext()), 200));
            ((TextView) averageSheet.findViewById(R.id.courseTextView)).setText(resolver.resolveCourse(thisCourse.getCourse(), getContext()));
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(2);
            ((TextView) averageSheet.findViewById(R.id.averageGradeTextView)).setText(nf.format(thisCourse.getGradeAverage()));

            SeekBar gradeSeekbar = averageSheet.findViewById(R.id.gradeSeekbar);
            gradeSeekbar.setProgress(Math.round(thisCourse.getGradeAverage()));
            gradeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    thisCourse.setGradeAverage((float) progress);
                    ((TextView) averageSheet.findViewById(R.id.averageGradeTextView)).setText(String.valueOf(progress));
                }

                //@formatter:off
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                //@formatter:on
            });
            averageSheet.findViewById(R.id.cancelButton).setOnClickListener(v1 -> averageSheet.dismiss());
            averageSheet.findViewById(R.id.saveButton).setOnClickListener(v2 -> {
                Map<String, Course> courses = new Gson().fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);
                courses.put(thisCourse.getCourse(), thisCourse);
                prefs.edit().putString("courses", new Gson().toJson(courses)).apply();
                averageSheet.dismiss();
                Intent doneIntent = new Intent("gradesRefreshed");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(doneIntent);
            });

            dismiss();
            averageSheet.show();
        });

        view.findViewById(R.id.addGradeFab).setOnClickListener(v -> {
            EditGradesSheet addSheet = new EditGradesSheet(thisCourse.getCourse(), -1);
            addSheet.show(getActivity().getSupportFragmentManager(), "new");
            dismiss();
        });

        RecyclerView gradesRecycler = view.findViewById(R.id.gradesRecycler);
        gradesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        gradesRecycler.setAdapter(new GradesAdapter(thisCourse.getGrades(), thisCourse.getCourse()));

        return view;
    }
}
