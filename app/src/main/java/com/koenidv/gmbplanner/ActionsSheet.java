package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//  Created by koenidv on 16.02.2020.
public class ActionsSheet extends BottomSheetDialogFragment {

    private View mPreview;

    ActionsSheet(View preview) {
        mPreview = preview;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppTheme_Sheet_Transparent);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sheet_actions, container, false);


        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();
        final Resolver resolver = new Resolver();
        final Gson gson = new Gson();

        final String course = ((TextView) mPreview.findViewById(R.id.courseHiddenTextView)).getText().toString();
        final String teacher = ((TextView) mPreview.findViewById(R.id.teacherHiddenTextView)).getText().toString();
        final String type = ((TextView) mPreview.findViewById(R.id.typeHiddenTextView)).getText().toString();
        final String date = ((TextView) ((ViewGroup) mPreview.getParent()).findViewById(R.id.dateTextView)).getText().toString();

        // Copy values
        ((TextView) view.findViewById(R.id.topTextView)).setText(((TextView) mPreview.findViewById(R.id.topTextView)).getText());
        ((TextView) view.findViewById(R.id.centerTextView)).setText(((TextView) mPreview.findViewById(R.id.centerTextView)).getText());
        ((TextView) view.findViewById(R.id.bottomTextView)).setText(((TextView) mPreview.findViewById(R.id.bottomTextView)).getText());
        view.findViewById(R.id.changeCard).setOnClickListener(null);
        view.findViewById(R.id.changeCard).setBackground(mPreview.getBackground());

        Button emailButton = view.findViewById(R.id.emailButton);
        if (resolver.resolveTeacherInitial(teacher).equals("unknown")) {
            emailButton.setVisibility(View.GONE);
        }
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subject;
                if (date.equals(getString(R.string.today)) || date.equals(getString(R.string.tomorrow))) {
                    subject = type + " " + date.toLowerCase();
                } else {
                    subject = type + getString(R.string.change_connect_date) + date;
                }
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{
                                resolver.resolveTeacherInitial(teacher) + "." + resolver.resolveTeacher(teacher).toLowerCase() + "@mosbacher-berg.de"})
                        .putExtra(Intent.EXTRA_SUBJECT, subject);
                // Only open if email client is installed
                if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null)
                    startActivity(emailIntent);
                dismiss();
            }
        });

        MaterialButton favButton = view.findViewById(R.id.favoritesButton);
        if (resolver.isFavorite(course, getContext())) {
            // Already added to favorites
            favButton.setText(R.string.action_favorites_remove);
            favButton.setIcon(getResources().getDrawable(R.drawable.ic_star));
        }
        favButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View v) {
                // Toggle favorite

                // Set up list with all added courses
                List<String> myCourses = new ArrayList<>();
                try {
                    myCourses = new ArrayList<>(Arrays.asList(gson.fromJson(prefs.getString("myCourses", ""), String[].class)));
                } catch (NullPointerException ignored) {
                    // No courses added yet
                }

                if (resolver.isFavorite(course, getContext())) {
                    // Is already favorite -> remove
                    // Check every entry if it contains this course and remove it if it does
                    String courseToRemove = "";
                    for (String thisCourse : myCourses) {
                        if (thisCourse.toUpperCase().contains(course.toUpperCase())) {
                            courseToRemove = thisCourse;
                        }
                    }
                    myCourses.remove(courseToRemove);
                } else {
                    // Not already favorite, add to myCourses
                    myCourses.add(course + " (" + teacher + ")");
                }

                // Save changes
                prefsEdit.putString("myCourses", gson.toJson(myCourses)).commit();
                // Broadcast to refresh UI
                Intent intent = new Intent("changesRefreshed");
                LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).sendBroadcast(intent);
                dismiss();
            }
        });

        view.findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareBody =
                        ((TextView) mPreview.findViewById(R.id.centerTextView)).getText().toString()
                                + getString(R.string.change_connect_date)
                                + date;
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.action_share)));
                dismiss();
                // Todo: Improve shared content
            }
        });

        view.findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


        return view;
    }
}
