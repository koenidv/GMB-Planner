package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;

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

        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();
        final Resolver resolver = new Resolver();
        final Gson gson = new Gson();

        // Get course
        Course courseInherited = resolver.getCourse(((TextView) mPreview.findViewById(R.id.courseHiddenTextView)).getText().toString(), getContext());
        if (courseInherited == null) courseInherited = new Course();
        final Course course = courseInherited;

        // Only when started from change
        final String type = ((TextView) mPreview.findViewById(R.id.typeHiddenTextView)).getText().toString(),
                date = ((TextView) ((ViewGroup) mPreview.getParent()).findViewById(R.id.dateTextView)).getText().toString();

        final boolean isChange = mPreview.getId() == R.id.outerCard;
        if (isChange) {
            // Copy values
            ((TextView) view.findViewById(R.id.topTextView)).setText(((TextView) mPreview.findViewById(R.id.topTextView)).getText());
            ((TextView) view.findViewById(R.id.centerTextView)).setText(((TextView) mPreview.findViewById(R.id.centerTextView)).getText());
            ((TextView) view.findViewById(R.id.bottomTextView)).setText(((TextView) mPreview.findViewById(R.id.bottomTextView)).getText());
            view.findViewById(R.id.changeCard).setOnClickListener(null);
            view.findViewById(R.id.changeCard).setBackground(mPreview.findViewById(R.id.changeCard).getBackground());
        } else {
            view.findViewById(R.id.shareButton).setVisibility(View.GONE);
            view.findViewById(R.id.include_change).setVisibility(View.GONE);
        }

        // Timetable
        //Lesson[][][] timetable = filterTimetable(course.getCourse());
        Lesson[][][] timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);

        final LinearLayout recyclerLayout = view.findViewById(R.id.recyclerLayout);
        final TextView titleTextView = view.findViewById(R.id.titleTextView);
        final ImageButton expandButton = view.findViewById(R.id.expandButton);

        if (timetable != null && !course.getCourse().equals("")) {
            if (!isChange)
                recyclerLayout.setVisibility(View.VISIBLE);
            titleTextView.setText(resolver.resolveCourse(course.getCourse(), getContext()));
            titleTextView.setVisibility(View.VISIBLE);
            view.findViewById(R.id.todayRecycler).setVisibility(View.GONE);
            // Set up 5 recyclerviews, one for each day
            RecyclerView mondayRecycler = view.findViewById(R.id.mondayRecycler),
                    tuesdayRecycler = view.findViewById(R.id.tuesdayRecycler),
                    wednesdayRecycler = view.findViewById(R.id.wednesdayRecycler),
                    thursdayRecycler = view.findViewById(R.id.thursdayRecycler),
                    fridayRecycler = view.findViewById(R.id.fridayRecycler);
            mondayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            tuesdayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            wednesdayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            thursdayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            fridayRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            LessonsAdapter mondayAdapter = new LessonsAdapter(timetable[0], course.getCourse(), 0);
            LessonsAdapter tuesdayAdapter = new LessonsAdapter(timetable[1], course.getCourse(), 1);
            LessonsAdapter wednesdayAdapter = new LessonsAdapter(timetable[2], course.getCourse(), 2);
            LessonsAdapter thursdayAdapter = new LessonsAdapter(timetable[3], course.getCourse(), 3);
            LessonsAdapter fridayAdapter = new LessonsAdapter(timetable[4], course.getCourse(), 4);
            mondayRecycler.setAdapter(mondayAdapter);
            tuesdayRecycler.setAdapter(tuesdayAdapter);
            wednesdayRecycler.setAdapter(wednesdayAdapter);
            thursdayRecycler.setAdapter(thursdayAdapter);
            fridayRecycler.setAdapter(fridayAdapter);
        } else {
            view.findViewById(R.id.card_timetable).setVisibility(View.GONE);
        }

        // Expand button to show the entire timetable
        expandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandButton.setVisibility(View.GONE);
                if (recyclerLayout.getVisibility() == View.GONE) {
                    recyclerLayout.setVisibility(View.VISIBLE);
                    expandButton.setImageResource(R.drawable.ic_less);
                } else {
                    recyclerLayout.setVisibility(View.GONE);
                    expandButton.setImageResource(R.drawable.ic_more);
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        expandButton.setVisibility(View.VISIBLE);
                    }
                }, getResources().getInteger(android.R.integer.config_shortAnimTime));
            }
        });

        Button emailButton = view.findViewById(R.id.emailButton);
        if (resolver.resolveTeacherInitial(course.getTeacher()).equals("unknown")) {
            emailButton.setVisibility(View.GONE);
        }
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subject;
                if (isChange) {
                    if (date.equals(getString(R.string.today)) || date.equals(getString(R.string.tomorrow))) {
                        subject = type + " " + date.toLowerCase();
                    } else {
                        subject = type + getString(R.string.change_connect_date) + date;
                    }
                } else {
                    subject = resolver.resolveCourse(course.getCourse(), getContext());
                }
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{
                                resolver.resolveTeacherInitial(course.getTeacher()) + "." + resolver.resolveTeacher(course.getTeacher()).toLowerCase() + "@mosbacher-berg.de"})
                        .putExtra(Intent.EXTRA_SUBJECT, subject);
                // Only open if email client is installed
                if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null)
                    startActivity(emailIntent);
                dismiss();
            }
        });

        MaterialButton favButton = view.findViewById(R.id.favoritesButton);
        if (resolver.isFavorite(course.getCourse(), getContext())) {
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

                if (resolver.isFavorite(course.getCourse(), getContext())) {
                    // Is already favorite -> remove
                    // Check every entry if it contains this course and remove it if it does
                    String courseToRemove = "";
                    for (String thisCourse : myCourses) {
                        if (thisCourse.toUpperCase().contains(course.getCourse().toUpperCase())) {
                            courseToRemove = thisCourse;
                        }
                    }
                    myCourses.remove(courseToRemove);
                } else {
                    // Not already favorite, add to myCourses
                    myCourses.add(course.getCourse() + " (" + course.getTeacher() + ")");
                }

                // Save changes
                prefsEdit.putString("myCourses", gson.toJson(myCourses)).commit();
                // Broadcast to refresh UI
                Intent intent = new Intent("changesRefreshed");
                intent.putExtra("coursesChanged", true);
                LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).sendBroadcast(intent);
                dismiss();
            }
        });

        view.findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dateBody;
                if (date.equals(getString(R.string.today)) || date.equals(getString(R.string.tomorrow))) {
                    dateBody = " " + date.toLowerCase();
                } else {
                    dateBody = getString(R.string.change_connect_date) + date;
                }
                String shareBody =
                        ((TextView) mPreview.findViewById(R.id.centerTextView)).getText().toString() + dateBody;
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
