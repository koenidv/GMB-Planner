package com.koenidv.gmbplanner;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class GradesAdapter extends RecyclerView.Adapter<GradesAdapter.ViewHolder> {
    private List<Pair<String, Float>> mDataset;

    public GradesAdapter(List<Pair<String, Float>> dataset) {
        mDataset = dataset;
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

        holder.nameTextView.setTag(mDataset.get(position).first);
        holder.nameTextView.setText(resolver.resolveCourse(mDataset.get(position).first, context));
        if (mDataset.get(position).second == null)
            holder.gradeTextView.setText("-");
        else {
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(1);
            holder.gradeTextView.setText(nf.format(mDataset.get(position).second));
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, gradeTextView;

        ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.nameTextView);
            gradeTextView = view.findViewById(R.id.gradeTextView);
        }
    }
}
