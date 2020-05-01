package com.koenidv.gmbplanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class GradesAdapter extends RecyclerView.Adapter<GradesAdapter.ViewHolder> {
    String mCourse;
    private List<Grade> mDataset;

    public GradesAdapter(List<Grade> dataset) {
        mDataset = dataset;
    }

    public GradesAdapter(List<Grade> dataset, String course) {
        mDataset = dataset;
        mCourse = course;
    }



    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grade, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Resolver resolver = new Resolver();
        Context context = holder.nameTextView.getContext();
        Grade thisGrade = mDataset.get(position);

        if (thisGrade.getType() == Grade.TYPE_COURSE_AVERAGE) {
            holder.nameTextView.setText(resolver.resolveCourse(thisGrade.getName(), context));
            holder.iconImageView.setColorFilter(resolver.resolveCourseColor(thisGrade.getName(), context));
            holder.rootView.setTag(R.id.course, thisGrade.getName());
            if (thisGrade.getGrade() != null && thisGrade.getGrade() < 6) {
                holder.iconImageView.setImageResource(R.drawable.ic_error);
            } else if (thisGrade.getGrade() != null && thisGrade.getGrade() < 11) {
                holder.iconImageView.setImageResource(R.drawable.ic_warning);
            }
        } else {
            holder.nameTextView.setText(thisGrade.getName());
            holder.rootView.setTag(R.id.index, position);
            holder.rootView.setTag(R.id.course, mCourse);
            switch (thisGrade.getType()) {
                case Grade.TYPE_EXAM:
                    holder.iconImageView.setImageResource(R.drawable.ic_exam);
                    break;
                case Grade.TYPE_PARTICIPATION:
                    holder.iconImageView.setImageResource(R.drawable.ic_participation);
                    break;
                case Grade.TYPE_PARTICIPATION_PARTIAL:
                    holder.iconImageView.setImageResource(R.drawable.ic_participation_partial);
                    break;
                default:
                    holder.iconImageView.setImageResource(R.drawable.ic_other);
            }
        }

        holder.rootView.setTag(thisGrade.getName());
        holder.rootView.setTag(R.id.type, thisGrade.getType());


        if (thisGrade.getGrade() == null)
            holder.gradeTextView.setText("-");
        else {
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(1);
            holder.gradeTextView.setText(nf.format(thisGrade.getGrade()));
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, gradeTextView;
        ImageView iconImageView;
        View rootView;

        ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.nameTextView);
            gradeTextView = view.findViewById(R.id.gradeTextView);
            iconImageView = view.findViewById(R.id.iconImageView);
            rootView = view.findViewById(R.id.rootView);
        }
    }
}
