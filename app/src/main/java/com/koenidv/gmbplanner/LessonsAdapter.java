package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
// Lesson adapter for one day
public class LessonsAdapter extends RecyclerView.Adapter<LessonsAdapter.ViewHolder> {
    private Lesson[][] mDataset;
    private String mFilter;
    private final int mDay;
    private boolean mEditMode;

    public LessonsAdapter(Lesson[][] dataset, int day) {
        mDataset = dataset;
        mDay = day;
    }

    LessonsAdapter(Lesson[][] dataset, String filter, int day) {
        mDataset = dataset;
        mFilter = filter;
        mDay = day;
    }

    LessonsAdapter(Lesson[][] dataset, boolean editMode, int day) {
        mDataset = dataset;
        mDay = day;
        mEditMode = editMode;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NotNull final ViewHolder holder, int position) {
        Context context = holder.courseTextView.getContext();
        Resolver resolver = new Resolver();
        List<Integer> gradientColors = new ArrayList<>();
        boolean matchesFilter = mFilter == null;

        // Reset to avoid recycling issues
        holder.courseTextView.setText("");
        holder.rootView.setVisibility(View.VISIBLE);
        holder.cardView.setVisibility(View.VISIBLE);
        holder.spacer.setVisibility(View.VISIBLE);
        holder.cardView.setClickable(true);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
        params.height = (int) resolver.dpToPx(32, context);
        holder.cardView.setLayoutParams(params);
        holder.cardView.setBackground(null);

        if (mDataset[position].length > 0) {
            holder.courseHiddenTextView.setText(mDataset[position][0].getCourse());
            for (int i = 0; i < mDataset[position].length; i++) {
                Lesson thisLesson = mDataset[position][i];
                StringBuilder stringToAdd = new StringBuilder();

                if (mDataset[position].length > 1)
                    stringToAdd.append(resolver.resolveCourseVeryShort(thisLesson.getCourse(), context)).append(".");
                else
                    stringToAdd.append(resolver.resolveCourseShort(thisLesson.getCourse(), context));

                if (position < mDataset.length - 1 && mDataset[position + 1].length > 0
                        && Arrays.equals(mDataset[position], mDataset[position + 1])) {
                    // Enlarge if same as next lesson
                    params = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
                    params.height = (int) resolver.dpToPx(68, context);
                    holder.cardView.setLayoutParams(params);
                    // Remove spacer from second-last (eg last shown) element
                    if (position + 2 == mDataset.length) {
                        holder.spacer.setVisibility(View.GONE);
                    }
                } else if (position > 0 && mDataset[position - 1].length > 0
                        && Arrays.equals(mDataset[position], mDataset[position - 1])) {
                    // Hide if same as last lesson
                    holder.rootView.setVisibility(View.GONE);
                    holder.cardView.setVisibility(View.GONE);
                    holder.spacer.setVisibility(View.GONE);
                }

                // Add course color to the gradient, twice
                gradientColors.add(resolver.resolveCourseColor(thisLesson.getCourse(), context));
                gradientColors.add(resolver.resolveCourseColor(thisLesson.getCourse(), context));

                // Show changes
                Course course = resolver.getCourse(thisLesson.getCourse(), context);
                if (course != null) {
                    for (Change change : course.getChanges()) {
                        int[] period = resolver.resolvePeriod(change.getDate(), change.getTime());
                        if (period[0] == mDay && period[1] <= position && period[2] >= position) {

                            gradientColors.set(2 * (i + 1) - 1, resolver.resolveTypeColor(change.getType(), context));

                            if (change.getType().equals("EVA") && !change.getRoomNew().equals("Sek"))
                                gradientColors.set(2 * (i + 1) - 1, Color.TRANSPARENT);

                            stringToAdd.setLength(0);
                            stringToAdd.append(resolver.resolveCourseVeryShort(course.getCourse(), context)).append(".");
                            if (change.isRoomChanged())
                                stringToAdd.append(context.getString(R.string.change_connect_room)).append(change.getRoomNew());
                            if (change.isTeacherChanged()) {
                                stringToAdd.append(context.getString(R.string.change_connect_teacher));
                                if (mDataset[position].length > 1)
                                    stringToAdd.append(change.getTeacherNew());
                                else
                                    stringToAdd.append(resolver.resolveTeacher(change.getTeacherNew()));
                            }
                            if (!change.isRoomChanged() && !change.isTeacherChanged())
                                stringToAdd.append(" ").append(change.getType());
                        }
                        // If only one of multiple lessons is changed
                        if (position < mDataset.length - 1 && mDataset[position + 1].length > 0
                                && Arrays.equals(mDataset[position], mDataset[position + 1])
                                && period[0] == mDay && (period[2] < position + 1 || period[1] > position)) {
                            // Same course as next one, but different change
                            params = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
                            params.height = (int) resolver.dpToPx(32, context);
                            holder.cardView.setLayoutParams(params);
                            // Re-show spacer as this is not the last element anymore
                            if (position + 2 == mDataset.length) {
                                holder.spacer.setVisibility(View.VISIBLE);
                            }
                        } else if (position > 0 && mDataset[position - 1].length > 0
                                && Arrays.equals(mDataset[position], mDataset[position - 1])
                                && period[0] == mDay && (period[1] > position - 1 || period[2] < position)) {
                            // Same course as last one, but different change
                            holder.rootView.setVisibility(View.VISIBLE);
                            holder.cardView.setVisibility(View.VISIBLE);
                            holder.spacer.setVisibility(View.VISIBLE);
                        }
                    }
                }

                if (i < mDataset[position].length - 1)
                    stringToAdd.append(", ");
                holder.courseTextView.append(stringToAdd);

                // Make cards opaque that don't match the filter
                if (!matchesFilter && mFilter != null && mFilter.equals(thisLesson.getCourse())) {
                    matchesFilter = true;
                }
            }

            int[] colorArray = gradientColors.stream().mapToInt(i -> i).toArray();
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colorArray);
            gradient.setCornerRadius(resolver.dpToPx(8, context));
            holder.cardView.setBackground(gradient);

            if (!matchesFilter) {
                holder.cardView.getBackground().setAlpha(65);
                holder.courseTextView.setAlpha(0.5f);
            }

            if (mFilter != null) {
                holder.cardView.setClickable(false);
            }

            holder.cardView.setTag(R.id.room, mDataset[position][0].getRoom());
            holder.cardView.setTag(R.id.course, mDataset[position][0].getCourse());

        } else {
            if (mEditMode) {
                PaintDrawable emptyBackground = new PaintDrawable(context.getColor(R.color.background));
                emptyBackground.setCornerRadius(resolver.dpToPx(8, context));
                holder.cardView.setBackground(emptyBackground);
            } else {
                holder.rootView.setVisibility(View.INVISIBLE);
                holder.cardView.setClickable(false);
            }
        }

        // Remove spacer from last element
        if (position + 1 == mDataset.length) {
            holder.spacer.setVisibility(View.GONE);
        }

        holder.cardView.setTag(R.id.day, mDay);
        holder.cardView.setTag(R.id.period, position);
        if (mEditMode) holder.cardView.setTag("edit");
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        try {
            return mDataset.length;
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseTextView, courseHiddenTextView;
        View cardView, rootView, spacer;

        ViewHolder(View view) {
            super(view);
            courseTextView = view.findViewById(R.id.courseTextView);
            courseHiddenTextView = view.findViewById(R.id.courseHiddenTextView);
            cardView = view.findViewById(R.id.cardView);
            rootView = view.findViewById(R.id.rootView);
            spacer = view.findViewById(R.id.spacer);
        }
    }

    public void setDataset(Lesson[][] mDataset) {
        this.mDataset = mDataset;
        notifyDataSetChanged();
    }
}
