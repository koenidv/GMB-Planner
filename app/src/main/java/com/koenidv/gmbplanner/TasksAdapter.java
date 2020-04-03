package com.koenidv.gmbplanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 15.02.2020.
public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final int TYPE_HEADER = 1;
    private static final int TYPE_TASK = 0;
    private List<Task> mDataset;

    public TasksAdapter(List<Task> dataset) {
        mDataset = dataset;
        for (int i = 0; i < mDataset.size() - 1; i++) {
            if (i == 0 || mDataset.get(i).getDoOn() != mDataset.get(i + 1).getDoOn()) {
                mDataset.add(i + 1, new Task(mDataset.get(i).getDoOn()));
            }
            mDataset.add(0, new Task(mDataset.get(i).getDoOn()));
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case (TYPE_HEADER):
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.sectionheader_tasks, parent, false);
                return new HeaderViewHolder(view);
            default:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_task, parent, false);
                return new TaskViewHolder(view);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        if (itemViewType == TYPE_TASK) {
            TaskViewHolder viewHolder = (TaskViewHolder) holder;
            viewHolder.titleTextView.setText(mDataset.get(position).getTitle());
        } else {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.dateTextView.setText(mDataset.get(position).getType());
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

    @Override
    public int getItemViewType(int position) {
        if (mDataset.get(position).getType() == TYPE_HEADER) {
            return TYPE_HEADER;
        } else {
            return TYPE_TASK;
        }
    }


    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        TaskViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.titleTextView);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;

        HeaderViewHolder(View view) {
            super(view);
            dateTextView = view.findViewById(R.id.dateTextView);
        }
    }


}
