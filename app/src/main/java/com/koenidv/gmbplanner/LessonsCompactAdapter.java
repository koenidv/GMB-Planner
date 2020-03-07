package com.koenidv.gmbplanner;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
// Lesson adapter for one day
public class LessonsCompactAdapter extends RecyclerView.Adapter<LessonsCompactAdapter.ViewHolder> {
    private Lesson[][] mDataset;
    private final int mDay;

    public LessonsCompactAdapter(Lesson[][] dataset, int day) {
        mDataset = dataset;
        mDay = day;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson_compact, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NotNull final ViewHolder holder, int position) {
        Context context = holder.courseTextView.getContext();
        if (mDataset[position].length > 0) {
            Lesson thisLesson = mDataset[position][0];
            Resolver resolver = new Resolver();

            holder.courseTextView.setText(resolver.resolveCourseVeryShort(thisLesson.getCourse(), context));
            holder.courseHiddenTextView.setText(thisLesson.getCourse());

            // ColorDrawable doesn't support corner radii
            int[] gradientColors = {resolver.resolveCourseColor(thisLesson.getCourse(), context), resolver.resolveCourseColor(thisLesson.getCourse(), context)};
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors);
            try {
                if (position > 0 && mDataset[position][0].getCourse().equals(mDataset[position - 1][0].getCourse())) {
                    holder.rootView.setVisibility(View.GONE);
                    holder.cardView.setVisibility(View.GONE);
                    holder.spacer.setVisibility(View.GONE);
                }
            } catch (ArrayIndexOutOfBoundsException ignored) {
                // Next or last period is empty
            }
            gradient.setCornerRadius(100f);
            holder.cardView.setBackground(gradient);

            // Show changes
            Course course = resolver.getCourse(thisLesson.getCourse(), context);
            if (course != null) {
                for (Change change : course.getChanges()) {
                    int[] period = resolver.resolvePeriod(change.getDate(), change.getTime());
                    if (period[0] == mDay && period[1] <= position && period[2] >= position) {
                        int[] changeGradientColors = {
                                resolver.resolveCourseColor(thisLesson.getCourse(), context),
                                resolver.resolveTypeColor(change.getType(), context),
                                resolver.resolveTypeColor(change.getType(), context)
                        };

                        if (change.getType().equals("EVA") && !change.getRoomNew().equals("Sek"))
                            changeGradientColors[1] = context.getColor(R.color.background);

                        GradientDrawable changeGradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, changeGradientColors);
                        changeGradient.setCornerRadius(100f);
                        holder.cardView.setBackground(changeGradient);
                    }
                    // If only one of multiple lessons is changed
                    if (position > 0 && mDataset[position - 1].length > 0
                            && mDataset[position][0].getCourse().equals(mDataset[position - 1][0].getCourse())
                            && period[0] == mDay && (period[1] > position - 1 || period[2] < position)) {
                        // Same course as last one, but different change
                        holder.rootView.setVisibility(View.VISIBLE);
                        holder.cardView.setVisibility(View.VISIBLE);
                        holder.spacer.setVisibility(View.VISIBLE);
                    }
                }
            }

        } else {
            holder.rootView.setVisibility(View.GONE);
            holder.cardView.setVisibility(View.GONE);
            holder.spacer.setVisibility(View.GONE);
        }
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
}
