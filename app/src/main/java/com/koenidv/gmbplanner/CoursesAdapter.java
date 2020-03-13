package com.koenidv.gmbplanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class CoursesAdapter extends RecyclerView.Adapter<CoursesAdapter.ViewHolder> {
    private List<String> mDataset;
    private boolean mEditMode;

    public CoursesAdapter(List<String> dataset) {
        mDataset = dataset;
    }

    CoursesAdapter(List<String> dataset, boolean editMode) {
        mDataset = dataset;
        mEditMode = editMode;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (mEditMode)
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_timetable, parent, false);
        else
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String thisCourse = mDataset.get(position);
        Resolver resolver = new Resolver();
        Context context = holder.courseTextView.getContext();

        holder.courseTextView.setTag(thisCourse);
        holder.courseTextView.setText(resolver.resolveCourse(thisCourse, context, true));

        if (mEditMode) {
            holder.starButton.setTag(thisCourse);
            holder.infoButton.setTag(thisCourse);
            if (MainActivity.myCourses.contains(thisCourse))
                holder.starButton.setImageResource(R.drawable.ic_star);
        } else {
            holder.deleteButton.setTag(thisCourse);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        try {
            return mDataset.size();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseTextView;
        ImageButton deleteButton, starButton, infoButton;

        ViewHolder(View view) {
            super(view);
            courseTextView = view.findViewById(R.id.courseTextView);
            deleteButton = view.findViewById(R.id.deleteButton);
            starButton = view.findViewById(R.id.favoritesButton);
            infoButton = view.findViewById(R.id.infoButton);
        }
    }
}
