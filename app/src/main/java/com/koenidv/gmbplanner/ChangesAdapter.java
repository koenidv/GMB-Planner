package com.koenidv.gmbplanner;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class ChangesAdapter extends RecyclerView.Adapter<ChangesAdapter.ViewHolder> {
    private List<Change> mDataset;

    public ChangesAdapter(List<Change> dataset) {
        mDataset = dataset;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Change thisChange = mDataset.get(position);

        holder.dateTextView.setText(thisChange.getDate());
        holder.typeTextView.setText(thisChange.getTime() + " " + thisChange.getType());

        if (thisChange.isCourseChanged())
            holder.courseTextView.setText(Html.fromHtml(thisChange.getCourseNew() + " <strike>" + thisChange.getCourse() + "</strike>"));
        else holder.courseTextView.setText(thisChange.getCourse());

        if (thisChange.isTeacherChanged())
            holder.teacherTextView.setText(Html.fromHtml("<strike>" + thisChange.getTeacher() + "</strike> " + thisChange.getTeacherNew()));
        else holder.teacherTextView.setText(thisChange.getTeacher());

        if (thisChange.isRoomChanged())
            holder.roomTextView.setText(Html.fromHtml(thisChange.getRoomNew() + " <strike>" + thisChange.getRoom() + "</strike>"));
        else holder.roomTextView.setText(thisChange.getRoom());

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
        TextView dateTextView, courseTextView, typeTextView, teacherTextView, roomTextView;

        ViewHolder(View view) {
            super(view);

            dateTextView = view.findViewById(R.id.dateTextView);
            courseTextView = view.findViewById(R.id.courseTextView);
            typeTextView = view.findViewById(R.id.typeTextView);
            teacherTextView = view.findViewById(R.id.teacherTextView);
            roomTextView = view.findViewById(R.id.roomTextView);
        }
    }
}
