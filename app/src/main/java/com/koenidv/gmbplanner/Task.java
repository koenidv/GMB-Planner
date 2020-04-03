package com.koenidv.gmbplanner;

import java.util.Date;

//  Created by koenidv on 03.04.2020.
public class Task {

    private String title;
    private String description;
    private String course;
    private Date dueOn;
    private Date doOn;
    private int type;

    Task() {
    }

    public Task(String mTitle, String mDescription, String mAddedBy, String mCourse, Date mDueOn, Date mDoOn) {
        title = mTitle;
        description = mDescription;
        course = mCourse;
        dueOn = mDueOn;
        doOn = mDoOn;
    }

    Task(Date mDoOn) {
        doOn = mDoOn;
        type = TasksAdapter.TYPE_HEADER;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCourse() {
        return course;
    }

    public Date getDueOn() {
        return dueOn;
    }

    public Date getDoOn() {
        return doOn;
    }

    public int getType() {
        return type;
    }
}
