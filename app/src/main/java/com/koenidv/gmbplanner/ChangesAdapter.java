package com.koenidv.gmbplanner;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class ChangesAdapter extends RecyclerView.Adapter<ChangesAdapter.ViewHolder> {
    private List<Change> mDataset;
    String lastDate = "";
    private boolean isFavorite;

    public ChangesAdapter(List<Change> dataset, boolean isFavorite) {
        mDataset = dataset;
        this.isFavorite = isFavorite;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Change thisChange = mDataset.get(position);
        Context context = holder.centerTextView.getContext();
        StringBuilder topString = new StringBuilder(thisChange.getTime() + context.getString(R.string.change_hours));
        StringBuilder centerString = new StringBuilder();
        StringBuilder bottomString = new StringBuilder();

        if (isFavorite)
            centerString.append(resolveCourseName(thisChange.getCourse(), context));
        else
            centerString.append(thisChange.getCourse());

        if (thisChange.getType().equals("EVA") || thisChange.getType().equals("Entfall") || thisChange.getType().equals("Freistellung")) {
            if (thisChange.getRoomNew().equals("Sek"))
                topString.append(context.getString(R.string.change_workorders));
            centerString.append(" ").append(thisChange.getType());
            bottomString.append(thisChange.getRoom()).append(" • ").append(thisChange.getTeacher());
        } else {
            topString.append(" • ").append(thisChange.getType());
            if (thisChange.isCourseChanged()) {
                centerString.delete(0, centerString.length()).append("<strike>").append(thisChange.getCourse()).append("</strike> ").append(thisChange.getCourseNew());
                bottomString.append("<strike>").append(thisChange.getCourse()).append("</strike> ").append(thisChange.getCourseNew()).append(" • ");
            }
            if (thisChange.isTeacherChanged()) {
                centerString.append(context.getString(R.string.change_connect_teacher)).append(thisChange.getTeacherNew());
                bottomString.append("<strike>").append(thisChange.getTeacher()).append("</strike> ").append(thisChange.getTeacherNew()).append(" • ");
            } else {
                bottomString.append(thisChange.getTeacher()).append(" • ");
            }
            if (thisChange.isRoomChanged()) {
                centerString.append(context.getString(R.string.change_connect_room)).append(thisChange.getRoomNew());
                bottomString.append("<strike>").append(thisChange.getRoom()).append("</strike> ").append(thisChange.getRoomNew());
            } else {
                bottomString.append(thisChange.getRoom());
            }
        }
        //Todo: Klausuren

        holder.topTextView.setText(Html.fromHtml(topString.toString(), Html.FROM_HTML_MODE_COMPACT));
        holder.centerTextView.setText(Html.fromHtml(centerString.toString(), Html.FROM_HTML_MODE_COMPACT));
        holder.bottomTextView.setText(Html.fromHtml(bottomString.toString(), Html.FROM_HTML_MODE_COMPACT));

        if (!thisChange.getDate().equals(lastDate)) {
            holder.dateTextView.setText(thisChange.getDate());
            holder.dateTextView.setVisibility(View.VISIBLE);
            lastDate = thisChange.getDate();
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

    private String resolveCourseName(String courseName, Context context) {
        StringBuilder name = new StringBuilder();
        if (courseName.contains("D-")) {
            name.append(context.getString(R.string.course_german));
        } else if (courseName.contains("E-")) {
            name.append(context.getString(R.string.course_english));
        } else if (courseName.contains("L-")) {
            name.append(context.getString(R.string.course_latin));
        } else if (courseName.contains("F-")) {
            name.append(context.getString(R.string.course_french));
        } else if (courseName.contains("G-")) {
            name.append(context.getString(R.string.course_history));
        } else if (courseName.contains("INFO-")) {
            name.append(context.getString(R.string.course_it));
        } else if (courseName.contains("KU-")) {
            name.append(context.getString(R.string.course_arts));
        } else if (courseName.contains("MU-")) {
            name.append(context.getString(R.string.course_music));
        } else if (courseName.contains("M-")) {
            name.append(context.getString(R.string.course_maths));
        } else if (courseName.contains("PH-")) {
            name.append(context.getString(R.string.course_physics));
        } else if (courseName.contains("BIO-")) {
            name.append(context.getString(R.string.course_biology));
        } else if (courseName.contains("CH-")) {
            name.append(context.getString(R.string.course_chemistry));
        } else if (courseName.contains("EK-")) {
            name.append(context.getString(R.string.course_geography));
        } else if (courseName.contains("POWI-")) {
            name.append(context.getString(R.string.course_politics));
        } else if (courseName.contains("SPO-")) {
            name.append(context.getString(R.string.course_sports));
        } else if (courseName.contains("RKA-") || courseName.contains("REV-")) {
            name.append(context.getString(R.string.course_german));
        } else if (courseName.contains("ETH-")) {
            name.append(context.getString(R.string.course_ethics));
        }
        if (courseName.contains("-LK")) {
            name.append(context.getString(R.string.course_intensified));
        }
        return name.toString();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView topTextView, centerTextView, bottomTextView, dateTextView;

        ViewHolder(View view) {
            super(view);

            topTextView = view.findViewById(R.id.topTextView);
            centerTextView = view.findViewById(R.id.centerTextView);
            bottomTextView = view.findViewById(R.id.bottomTextView);
            dateTextView = view.findViewById(R.id.dateTextView);
        }
    }
}
