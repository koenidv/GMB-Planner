package com.koenidv.gmbplanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
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

import static android.content.Context.MODE_PRIVATE;

//  Created by koenidv on 15.02.2020.
public class ChangesAdapter extends RecyclerView.Adapter<ChangesAdapter.ViewHolder> {
    private List<Change> mDataset;
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

        if (thisChange.getType() != null) {
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
                if (!thisChange.getRoom().equals(" "))
                    bottomString.append(thisChange.getRoom()).append(" • ");
            if (isFavorite)
                bottomString.append(resolver.resolveTeacher(thisChange.getTeacher()));
            else
                bottomString.append(thisChange.getCourse());
            } else {
                if (thisChange.isCourseChanged()) {
                    centerString.delete(0, centerString.length()).append("<strike>").append(thisChange.getCourse()).append("</strike> ").append(thisChange.getCourseNew());
                }
                if (thisChange.isRoomChanged()) {
                    centerString.append(context.getString(R.string.change_connect_room)).append(thisChange.getRoomNew());
                    bottomString.append("<strike>").append(thisChange.getRoom()).append("</strike>").append(" • ");
                } else {
                    if (!thisChange.getRoom().equals(" "))
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
                } else {
                    topString.append(" • ").append(thisChange.getType());
                }
            }

            holder.topTextView.setText(Html.fromHtml(topString.toString()));
            holder.centerTextView.setText(Html.fromHtml(centerString.toString()));
            holder.bottomTextView.setText(Html.fromHtml(bottomString.toString()));
            holder.courseHiddenTextView.setText(thisChange.getCourse());
            holder.teacherHiddenTextView.setText(thisChange.getTeacher());
            holder.typeHiddenTextView.setText(thisChange.getType());

            holder.dateTextView.setText(resolver.resolveDate(thisChange.getDate(), context));
            if (position == 0 || !thisChange.getDate().equals(mDataset.get(position - 1).getDate())) {
                holder.dateTextView.setVisibility(View.VISIBLE);
                holder.setIsRecyclable(false);
                String lastDate = thisChange.getDate();
            } else {
                holder.dateTextView.setVisibility(View.GONE);
            }

            int[] gradientColors = {resolver.resolveCourseColor(thisChange.getCourse(), context), resolver.resolveTypeColor(thisChange.getType(), context)};
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors);
            gradient.setCornerRadius(resolver.dpToPx(10, context));
            holder.card.setBackground(gradient);

            SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            if (prefs.getBoolean("colorless", false)) {
                holder.innerCard.setBackground(context.getDrawable(R.drawable.card_background));
            }
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
        View card, innerCard;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.changeCard);
            innerCard = view.findViewById(R.id.innerCard);
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
