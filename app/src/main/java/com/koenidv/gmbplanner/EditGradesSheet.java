package com.koenidv.gmbplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.content.Context.MODE_PRIVATE;

//  Created by koenidv on 16.02.2020.
public class EditGradesSheet extends BottomSheetDialogFragment {

    String mCourse;
    int mIndex = -1;
    TextInputLayout gradeLayout;
    EditText nameText;
    EditText gradeText;
    Button saveButton;

    EditGradesSheet(String course, int index) {
        mCourse = course;
        mIndex = index;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppTheme_Sheet_Transparent);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sheet_grades_edit, container, false);
        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        if (mCourse == null)
            dismiss();

        final Resolver resolver = new Resolver();
        final Gson gson = new Gson();

        ImageView icon = view.findViewById(R.id.iconImageView);
        Spinner typeSpinner = view.findViewById(R.id.typeSpinner);
        nameText = view.findViewById(R.id.nameEditText);
        gradeText = view.findViewById(R.id.gradeEditText);
        gradeLayout = view.findViewById(R.id.gradeInputLayout);
        saveButton = view.findViewById(R.id.saveButton);

        Grade thisGrade;
        if (mIndex == -1)
            thisGrade = new Grade();
        else {
            thisGrade = resolver.getCourse(mCourse, getContext()).getGrades().get(mIndex);
            nameText.setText(thisGrade.getName());
            gradeText.setText(String.valueOf(thisGrade.getGrade()));
            switch (thisGrade.getType()) {
                case Grade.TYPE_EXAM:
                    typeSpinner.setSelection(0);
                    break;
                case Grade.TYPE_PARTICIPATION:
                    typeSpinner.setSelection(1);
                    break;
                case Grade.TYPE_PARTICIPATION_PARTIAL:
                    typeSpinner.setSelection(2);
                    break;
                default:
                    typeSpinner.setSelection(3);
            }
            checkInputValid();
        }

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        thisGrade.setType(Grade.TYPE_EXAM);
                        icon.setImageResource(R.drawable.ic_exam);
                        break;
                    case 1:
                        thisGrade.setType(Grade.TYPE_PARTICIPATION);
                        icon.setImageResource(R.drawable.ic_participation);
                        break;
                    case 2:
                        thisGrade.setType(Grade.TYPE_PARTICIPATION_PARTIAL);
                        icon.setImageResource(R.drawable.ic_participation_partial);
                        break;
                    default:
                        thisGrade.setType(Grade.TYPE_OTHER);
                        icon.setImageResource(R.drawable.ic_other);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        nameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                thisGrade.setName(s.toString());
                checkInputValid();
            }
        });

        gradeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty() && Float.parseFloat(s.toString()) <= 15) {
                    thisGrade.setGrade(Float.parseFloat(s.toString()));
                    gradeLayout.setErrorEnabled(false);
                } else if (!s.toString().isEmpty()) {
                    gradeLayout.setErrorEnabled(true);
                }
                checkInputValid();
            }
        });

        view.findViewById(R.id.moreButton).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), v);
            popup.setOnMenuItemClickListener(this::onMenuItemClick);
            popup.inflate(R.menu.delete);
            popup.show();
        });

        if (mIndex < 0)
            view.findViewById(R.id.moreButton).setVisibility(View.GONE);

        view.findViewById(R.id.cancelButton).setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> {
            Map<String, Course> courses = new Gson().fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);
            if (mIndex == -1) {
                courses.get(mCourse).addGrade(thisGrade);
            } else {
                courses.get(mCourse).setGrade(mIndex, thisGrade);
            }
            prefs.edit().putString("courses", gson.toJson(courses)).apply();
            dismiss();
            Intent doneIntent = new Intent("gradesRefreshed");
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(doneIntent);
        });

        return view;
    }

    private void checkInputValid() {
        saveButton.setEnabled(!gradeLayout.isErrorEnabled()
                && !nameText.getText().toString().isEmpty()
                && !gradeText.getText().toString().isEmpty());
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            //Delete this grade
            SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            Map<String, Course> courses = new Gson().fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);
            courses.get(mCourse).removeGrade(mIndex);
            prefs.edit().putString("courses", new Gson().toJson(courses)).apply();
            dismiss();
            Intent doneIntent = new Intent("gradesRefreshed");
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(doneIntent);
        }
        return true;
    }

}
