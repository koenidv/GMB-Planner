package com.koenidv.gmbplanner;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
// Lesson adapter for one day
public class LessonsAdapter extends RecyclerView.Adapter<LessonsAdapter.ViewHolder> {
    private Lesson[][] mDataset;

    public LessonsAdapter(Lesson[][] dataset) {
        mDataset = dataset;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson, parent, false);
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

            holder.courseTextView.setText(resolver.resolveCourseShort(thisLesson.getCourse(), context));

            // ColorDrawable doesn't support corner radii
            int[] gradientColors = {resolver.resolveCourseColor(thisLesson.getCourse(), context), resolver.resolveCourseColor(thisLesson.getCourse(), context)};
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors);

            float[] cornerRadii = {
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context),
                    resolver.dpToPx(8, context)
            };

            if (position < mDataset.length - 1 && mDataset[position + 1].length > 0
                    && mDataset[position][0].getCourse().equals(mDataset[position + 1][0].getCourse())) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
                    params.height = (int) resolver.dpToPx(68, context);
                    holder.cardView.setLayoutParams(params);
            } else if (position > 0 && mDataset[position - 1].length > 0
                    && mDataset[position][0].getCourse().equals(mDataset[position - 1][0].getCourse())) {
                    holder.rootView.setVisibility(View.GONE);
                    holder.cardView.setVisibility(View.GONE);
                    holder.spacer.setVisibility(View.GONE);
                }

            gradient.setCornerRadii(cornerRadii);
            holder.cardView.setBackground(gradient);
        } else {
            holder.rootView.setVisibility(View.INVISIBLE);
            holder.cardView.setVisibility(View.INVISIBLE);
            holder.spacer.setVisibility(View.INVISIBLE);
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
        TextView courseTextView;
        View cardView, rootView, spacer;

        ViewHolder(View view) {
            super(view);
            courseTextView = view.findViewById(R.id.courseTextView);
            cardView = view.findViewById(R.id.cardView);
            rootView = view.findViewById(R.id.rootView);
            spacer = view.findViewById(R.id.spacer);
        }
    }
}
