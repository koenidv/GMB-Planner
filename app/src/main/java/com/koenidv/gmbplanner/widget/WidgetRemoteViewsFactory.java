package com.koenidv.gmbplanner.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.koenidv.gmbplanner.Change;
import com.koenidv.gmbplanner.R;
import com.koenidv.gmbplanner.Resolver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//  Created by koenidv on 24.02.2020.
public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private List<Change> dataset = new ArrayList<>();
    private String lastDate;

    WidgetRemoteViewsFactory(Context applicationContext, Intent intent) {
        mContext = applicationContext;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        Gson gson = new Gson();
        SharedPreferences prefs = mContext.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        List<Change> everyChangeList;
        Type listType = new TypeToken<ArrayList<Change>>() {
        }.getType();

        everyChangeList = gson.fromJson(prefs.getString("changes", ""), listType);
        List<String> myCourses = new ArrayList<>();
        try {
            myCourses = Arrays.asList(gson.fromJson(prefs.getString("myCourses", ""), String[].class));
        } catch (NullPointerException ignored) {
        }

        if (everyChangeList != null) {
            for (Change change : everyChangeList) {
                // Convert to string so that the list can contain a note about the teacher (eg course (teacher))
                if (myCourses.toString().toUpperCase().contains(change.getCourseString().toUpperCase()))
                    dataset.add(change);
            }
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return dataset.size();
        //return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.item_change_widget);
        Change change = dataset.get(position);
        Resolver resolver = new Resolver();

        StringBuilder mainString = new StringBuilder(resolver.resolveCourse(change.getCourseString(), mContext));
        StringBuilder infoString = new StringBuilder(change.getTime() + " " + mContext.getString(R.string.change_hours) + " • ");

        if (change.getType().equals("EVA") || change.getType().equals("Entfall") || change.getType().equals("Freistellung")) {
            if (change.getRoomNew().equals("Sek"))
                infoString.append(mContext.getString(R.string.change_workorders)).append(" • ");
            mainString.append(" ").append(change.getType());
            infoString.append(change.getRoom()).append(" • ").append(resolver.resolveTeacher(change.getTeacher()));
        } else {
            infoString.append(change.getType()).append(" • ");
            if (change.isCourseChanged()) {
                mainString.delete(0, mainString.length()).append("<strike>").append(change.getCourseString()).append("</strike> ").append(change.getCourseStringNew());
            }
            if (change.isRoomChanged()) {
                mainString.append(mContext.getString(R.string.change_connect_room)).append(change.getRoomNew());
                infoString.append("<strike>").append(change.getRoom()).append("</strike>").append(" • ");
            } else {
                infoString.append(change.getRoom()).append(" • ");
            }
            if (change.isTeacherChanged()) {
                mainString.append(mContext.getString(R.string.change_connect_teacher)).append(change.getTeacherNew());
                infoString.append("<strike>").append(change.getTeacher()).append("</strike>");
            } else {
                infoString.append(change.getTeacher());
            }
            if (change.isCourseChanged()) {
                infoString.append(" • ").append("<strike>").append(change.getCourseString()).append("</strike>");
            }
            if (!change.isRoomChanged() && !change.isTeacherChanged() && !change.isCourseChanged()) {
                // Probably an exam
                mainString.append(" • ").append(change.getType());
            }
        }

        rv.setTextViewText(R.id.centerTextView, Html.fromHtml(mainString.toString()));
        rv.setTextViewText(R.id.infoTextView, Html.fromHtml(infoString.toString()));

        if (!change.getDate().equals(lastDate)) {
            rv.setTextViewText(R.id.dateTextView, resolver.resolveDate(change.getDate(), mContext));
            rv.setViewVisibility(R.id.dateTextView, View.VISIBLE);
            lastDate = change.getDate();
        }

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(mContext.getPackageName(), R.layout.item_change_widget);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        //return mCursor.moveToPosition(position) ? mCursor.getLong(0) : position;
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public boolean isEnabled(int position) {
        return false;
    }

}