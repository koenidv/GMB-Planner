package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;
import static com.koenidv.gmbplanner.MainActivity.coursesSheet;

//  Created by koenidv on 16.02.2020.
public class ActionsSheet extends BottomSheetDialogFragment {

    private View mPreview;
    private String mCourseTag;

    ActionsSheet(View preview) {
        mPreview = preview;
    }

    ActionsSheet(View preview, String course) {
        mPreview = preview;
        mCourseTag = course;
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
        Course courseInherited;
        if (mCourseTag == null)
            courseInherited = resolver.getCourse(((TextView) mPreview.findViewById(R.id.courseHiddenTextView)).getText().toString(), getContext());
        else {
            courseInherited = resolver.getCourse(mCourseTag, getContext());
            view.findViewById(R.id.favoritesButton).setVisibility(View.GONE);
            view.findViewById(R.id.recyclerLayout).setVisibility(View.VISIBLE);
        }
        if (courseInherited == null) courseInherited = new Course();
        final Course course = courseInherited;

        final boolean isChange = mPreview.getId() == R.id.outerCard;

        // Timetable
        Lesson[][][] timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);

        if (isChange) {
            // Copy values
            ((TextView) view.findViewById(R.id.topTextView)).setText(((TextView) mPreview.findViewById(R.id.topTextView)).getText());
            ((TextView) view.findViewById(R.id.centerTextView)).setText(((TextView) mPreview.findViewById(R.id.centerTextView)).getText());
            ((TextView) view.findViewById(R.id.bottomTextView)).setText(((TextView) mPreview.findViewById(R.id.bottomTextView)).getText());
            view.findViewById(R.id.changeCard).setBackground(mPreview.findViewById(R.id.changeCard).getBackground());
            // Disable onClick as it would just open this sheet again
            view.findViewById(R.id.changeCard).setOnClickListener(null);
        } else {
            // Hide unneccessary buttons
            view.findViewById(R.id.shareButton).setVisibility(View.GONE);
            view.findViewById(R.id.include_change).setVisibility(View.GONE);
            // Display the lesson's room
            if (mPreview.getTag(R.id.room) != null) {
                view.findViewById(R.id.roomCard).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.roomTextView)).setText(getString(R.string.lesson_room).replace("%room", (String) mPreview.getTag(R.id.room)));
            }
        }

        // Add to timetable if not a favorite course and thus not already added
        if (!resolver.isFavorite(course.getCourse(), getContext())) {
            Lesson[][][] allTable = (new Gson()).fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
            for (int day = 0; day <= 4; day++) {
                for (int period = 0; period < allTable[day].length; period++) {
                    for (Lesson lesson : allTable[day][period]) {
                        if (lesson.getCourse().equals(course.getCourse())) {
                            List<Lesson> thisPeriod = new ArrayList<>(Arrays.asList(timetable[day][period]));
                            thisPeriod.add(lesson);
                            timetable[day][period] = thisPeriod.toArray(new Lesson[0]);
                        }
                    }
                }
            }
        }

        final LinearLayout recyclerLayout = view.findViewById(R.id.recyclerLayout);
        final TextView titleTextView = view.findViewById(R.id.titleTextView);
        final ImageButton expandButton = view.findViewById(R.id.expandButton);

        ((LinearLayout) view.findViewById(R.id.compactLayout)).setLayoutTransition(null);

        if (timetable != null && !course.getCourse().equals("")) {
            titleTextView.setText(resolver.resolveCourse(course.getCourse(), getContext(), true));
            titleTextView.setVisibility(View.VISIBLE);
            view.findViewById(R.id.todayRecycler).setVisibility(View.GONE);

            // Set up 5 recyclerviews, one for each day
            RecyclerView[] dayRecyclers = {
                    view.findViewById(R.id.mondayRecycler),
                    view.findViewById(R.id.tuesdayRecycler),
                    view.findViewById(R.id.wednesdayRecycler),
                    view.findViewById(R.id.thursdayRecycler),
                    view.findViewById(R.id.fridayRecycler)
            };
            for (int i = 0; i < dayRecyclers.length; i++) {
                RecyclerView recycler = dayRecyclers[i];
                recycler.setLayoutManager(new LinearLayoutManager(getContext()));
                recycler.setAdapter(new LessonsAdapter(timetable[i], course.getCourse(), i));
            }

            // Mark today
            int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
            if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 16) weekDay++;
            if (weekDay < 0 || weekDay > 4) weekDay = 0;
            PaintDrawable todayBackground = new PaintDrawable(getResources().getColor(R.color.background));
            todayBackground.setCornerRadius((new Resolver()).dpToPx(8, getActivity()));
            dayRecyclers[weekDay].setBackground(todayBackground);
        } else {
            view.findViewById(R.id.card_timetable).setVisibility(View.GONE);
        }

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


        List<Change> changeList = Arrays.asList(course.getChanges());
        if (!isChange && !changeList.isEmpty()) {
            view.findViewById(R.id.recyclerCard).setVisibility(View.VISIBLE);
            final ImageButton recyclerExpandButton = view.findViewById(R.id.recyclerExpandButton);
            final RecyclerView changesRecycler = view.findViewById(R.id.changesRecycler);

            changesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            changesRecycler.setAdapter(new ChangesAdapter(changeList, resolver.isFavorite(course.getCourse(), getContext()), true));

            view.findViewById(R.id.recyclerExpandLayout).setOnClickListener(v -> {
                if (changesRecycler.getVisibility() == View.GONE) {
                    changesRecycler.setVisibility(View.VISIBLE);
                    recyclerExpandButton.setImageResource(R.drawable.ic_less);
                } else {
                    changesRecycler.setVisibility(View.GONE);
                    recyclerExpandButton.setImageResource(R.drawable.ic_more);
                }
            });
        }

        Button emailButton = view.findViewById(R.id.emailButton);
        if (resolver.resolveTeacherInitial(course.getTeacher()).equals("unknown")) {
            emailButton.setVisibility(View.GONE);
        }
        emailButton.setOnClickListener(v -> {
            String subject;
            if (isChange) {
                String type = ((TextView) mPreview.findViewById(R.id.typeHiddenTextView)).getText().toString(),
                        date = ((TextView) ((ViewGroup) mPreview.getParent()).findViewById(R.id.dateTextView)).getText().toString();
                if (date.equals(getString(R.string.today)) || date.equals(getString(R.string.tomorrow))) {
                    subject = type + " " + date.toLowerCase();
                } else {
                    subject = type + getString(R.string.change_connect_date) + date;
                }
            } else {
                subject = resolver.resolveCourse(course.getCourse(), getContext());
            }
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:"))
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{
                            resolver.resolveTeacherInitial(course.getTeacher()) + "." + resolver.resolveTeacher(course.getTeacher()).toLowerCase() + "@mosbacher-berg.de"})
                    .putExtra(Intent.EXTRA_SUBJECT, subject);
            // Only open if email client is installed
            if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null)
                startActivity(emailIntent);
            dismiss();
        });

        MaterialButton favButton = view.findViewById(R.id.favoritesButton);
        if (resolver.isFavorite(course.getCourse(), getContext())) {
            // Already added to favorites
            favButton.setText(R.string.action_favorites_remove);
            favButton.setIcon(getResources().getDrawable(R.drawable.ic_star));
        }
        favButton.setOnClickListener(v -> {
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
            prefsEdit.putString("myCourses", gson.toJson(myCourses)).apply();
            // Broadcast to refresh UI
            Intent intent = new Intent("changesRefreshed");
            intent.putExtra("coursesChanged", true);
            LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).sendBroadcast(intent);
            // Vibrate
            resolver.vibrate(getContext());
            dismiss();
        });
        favButton.setOnLongClickListener(v -> {
            coursesSheet = new CoursesSheet();
            coursesSheet.show(getActivity().getSupportFragmentManager(), "coursesSheet");
            return true;
        });

        if (isChange) {
            view.findViewById(R.id.shareButton).setOnClickListener(v -> {
                String dateBody;
                String date = ((TextView) ((ViewGroup) mPreview.getParent()).findViewById(R.id.dateTextView)).getText().toString();
                if (date.equals(getString(R.string.today)) || date.equals(getString(R.string.tomorrow))) {
                    dateBody = " " + date.toLowerCase();
                } else {
                    dateBody = getString(R.string.change_connect_date) + date;
                }
                String shareBody =
                        ((TextView) mPreview.findViewById(R.id.centerTextView)).getText().toString() + dateBody;
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.action_share)));
                dismiss();
                // leTodo: Improve shared content
            });
        }

        view.findViewById(R.id.doneButton).setOnClickListener(v -> dismiss());


        return view;
    }
}
