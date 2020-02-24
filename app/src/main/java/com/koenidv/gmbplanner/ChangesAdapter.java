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
    private String lastDate = "";
    private boolean isFavorite, isCompact;

    public ChangesAdapter(List<Change> dataset, boolean isFavorite, boolean isCompact) {
        mDataset = dataset;
        this.isFavorite = isFavorite;
        this.isCompact = isCompact;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (isCompact) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change_compact, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change, parent, false);
        }
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Change thisChange = mDataset.get(position);
        Context context = holder.centerTextView.getContext();
        Resolver resolver = new Resolver();
        StringBuilder topString = new StringBuilder(thisChange.getTime() + context.getString(R.string.change_hours));
        StringBuilder centerString = new StringBuilder();
        StringBuilder bottomString = new StringBuilder();

        if (isFavorite)
            centerString.append(resolver.resolveCourse(thisChange.getCourse(), context));
        else
            centerString.append(resolver.resolveCourse(thisChange.getCourse(), context)).append(" (").append(resolver.resolveTeacher(thisChange.getTeacher())).append(")");

        if (thisChange.getType().equals("EVA") || thisChange.getType().equals("Entfall") || thisChange.getType().equals("Freistellung")) {
            if (thisChange.getRoomNew().equals("Sek"))
                topString.append(context.getString(R.string.change_workorders));
            centerString.append(" ").append(thisChange.getType());
            bottomString.append(thisChange.getRoom()).append(" • ");
            if (isFavorite)
                bottomString.append(resolver.resolveTeacher(thisChange.getTeacher()));
            else
                bottomString.append(thisChange.getCourse());
        } else {
            topString.append(" • ").append(thisChange.getType());
            if (thisChange.isCourseChanged()) {
                centerString.delete(0, centerString.length()).append("<strike>").append(thisChange.getCourse()).append("</strike> ").append(thisChange.getCourseNew());
            }
            if (thisChange.isRoomChanged()) {
                centerString.append(context.getString(R.string.change_connect_room)).append(thisChange.getRoomNew());
                bottomString.append("<strike>").append(thisChange.getRoom()).append("</strike>").append(" • ");
            } else {
                bottomString.append(thisChange.getRoom()).append(" • ");
            }
            if (thisChange.isTeacherChanged()) {
                centerString.append(context.getString(R.string.change_connect_teacher)).append(resolver.resolveTeacher(thisChange.getTeacherNew()));
                bottomString.append("<strike>").append(resolver.resolveTeacher(thisChange.getTeacher())).append("</strike>");
            } else if (isFavorite) {
                bottomString.append(resolver.resolveTeacher(thisChange.getTeacher()));
            }
            if (thisChange.isCourseChanged()) {
                bottomString.append("<strike>").append(thisChange.getCourse()).append("</strike>");
            } else if (!isFavorite) {
                bottomString.append(thisChange.getCourse());
            }
            if (!thisChange.isRoomChanged() && !thisChange.isTeacherChanged() && !thisChange.isCourseChanged()) {
                // Probably an exam
                centerString.append(" • ").append(thisChange.getType());
            }
        }

        holder.topTextView.setText(Html.fromHtml(topString.toString(), Html.FROM_HTML_MODE_COMPACT));
        holder.centerTextView.setText(Html.fromHtml(centerString.toString(), Html.FROM_HTML_MODE_COMPACT));
        holder.bottomTextView.setText(Html.fromHtml(bottomString.toString(), Html.FROM_HTML_MODE_COMPACT));
        holder.courseHiddenTextView.setText(thisChange.getCourse());
        holder.teacherHiddenTextView.setText(thisChange.getTeacher());
        holder.typeHiddenTextView.setText(thisChange.getType());

        holder.dateTextView.setText(thisChange.getDate());
        if (!thisChange.getDate().equals(lastDate)) {
            holder.dateTextView.setVisibility(View.VISIBLE);
            lastDate = thisChange.getDate();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            holder.card.setOutlineAmbientShadowColor(resolver.resolveCourseColor(thisChange.getCourse(), context));
            holder.card.setOutlineSpotShadowColor(resolver.resolveCourseColor(thisChange.getCourse(), context));
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
        TextView topTextView, centerTextView, bottomTextView, dateTextView, courseHiddenTextView, teacherHiddenTextView, typeHiddenTextView;
        View card;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.changeCard);
            topTextView = view.findViewById(R.id.topTextView);
            centerTextView = view.findViewById(R.id.centerTextView);
            bottomTextView = view.findViewById(R.id.bottomTextView);
            dateTextView = view.findViewById(R.id.dateTextView);
            courseHiddenTextView = view.findViewById(R.id.courseHiddenTextView);
            teacherHiddenTextView = view.findViewById(R.id.teacherHiddenTextView);
            typeHiddenTextView = view.findViewById(R.id.typeHiddenTextView);
        }
    }
}
