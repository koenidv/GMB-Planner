package com.koenidv.gmbplanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class TasksExternalAdapter extends RecyclerView.Adapter<TasksExternalAdapter.ViewHolder> {
    private List<TaskExternal> mDataset;

    public TasksExternalAdapter(List<TaskExternal> dataset) {
        mDataset = dataset;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_external, parent, false);
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TaskExternal thisTask = mDataset.get(position);
        Resolver resolver = new Resolver();
        Context context = holder.titleTextView.getContext();

        holder.titleTextView.setText(thisTask.getTitle());
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

    public void setDataset(List<TaskExternal> mDataset) {
        this.mDataset = mDataset;
        this.notifyDataSetChanged();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        ViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.titleTextView);
        }
    }
}
