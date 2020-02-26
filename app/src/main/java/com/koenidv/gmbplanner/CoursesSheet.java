package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 16.02.2020.
public class CoursesSheet extends BottomSheetDialogFragment {

    List<String> myCourses = new ArrayList<>();
    CoursesAdapter adapter;
    private Gson gson = new Gson();

    public CoursesSheet() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_courses, container, false);

        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        // Set up list with all added courses
        try {
            myCourses = new ArrayList<>(Arrays.asList(gson.fromJson(prefs.getString("myCourses", ""), String[].class)));
        } catch (NullPointerException ignored) {
            // No courses added yet
        }

        RecyclerView recyclerView = view.findViewById(R.id.coursesRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new CoursesAdapter(myCourses);
        recyclerView.setAdapter(adapter);

        // Set up suggestion chips
        String[] allCourses = gson.fromJson(prefs.getString("allCourses", ""), String[].class);
        final ChipGroup chipgroup = view.findViewById(R.id.chipgroup);
        final TextInputEditText editText = view.findViewById(R.id.addCourseEditText);


        View.OnClickListener chipListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = ((Chip) v).getText().toString();
                myCourses.add(text);
                adapter.notifyDataSetChanged();
                v.setVisibility(View.GONE);
                v.setEnabled(false);
                editText.setText("");
            }
        };
        View.OnLongClickListener chipLongListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.setVisibility(View.GONE);
                return true;
            }
        };

        if (allCourses != null) {
            for (String thisCourse : allCourses) {
                // Only show courses that are not yet added
                if (!myCourses.contains(thisCourse)) {
                    final Chip chip = new Chip(chipgroup.getContext());
                    chip.setClickable(true);
                    chip.setOnClickListener(chipListener);
                    chip.setOnLongClickListener(chipLongListener);
                    chip.setText(thisCourse);
                    chip.setMinHeight(32);
                    chipgroup.addView(chip);
                }
            }
        }

        // Custom courses
        final TextInputLayout inputLayout = view.findViewById(R.id.addCourseInputLayout);
        inputLayout.setEndIconVisible(false);
        inputLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Objects.requireNonNull(editText.getText()).toString().isEmpty()) {
                    myCourses.add(Objects.requireNonNull(editText.getText()).toString());
                    editText.setText("");
                    adapter.notifyDataSetChanged();
                }
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty())
                    inputLayout.setEndIconVisible(false);
                else
                    inputLayout.setEndIconVisible(true);
                for (int i = 0; i < chipgroup.getChildCount(); i++) {
                    // Search chips
                    if (chipgroup.getChildAt(i).isEnabled()) {
                        if (s.toString().isEmpty()) {
                            chipgroup.getChildAt(i).setVisibility(View.VISIBLE);
                        } else if (((Chip) chipgroup.getChildAt(i)).getText().toString().toUpperCase().contains(s.toString().toUpperCase())) {
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
        view.findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Objects.requireNonNull(editText.getText()).toString().isEmpty()) {
                    myCourses.add(Objects.requireNonNull(editText.getText()).toString().toUpperCase());
                    editText.setText("");
                }
                CoursesSheet.this.dismiss();
            }
        });

        return view;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // Save changes
        final SharedPreferences.Editor prefsEdit = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE).edit();
        prefsEdit.putString("myCourses", gson.toJson(myCourses)).commit();
        // Broadcast to refresh UI
        Intent intent = new Intent("changesRefreshed");
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).sendBroadcast(intent);
        super.onDismiss(dialog);
    }
}
