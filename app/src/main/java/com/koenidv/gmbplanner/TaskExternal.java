package com.koenidv.gmbplanner;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

//  Created by koenidv on 03.04.2020.
public class TaskExternal {

    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("addedBy")
    private String addedBy;
    @SerializedName("course")
    private String course;
    @SerializedName("dueOn")
    private Date dueOn;

    TaskExternal() {
    }

    TaskExternal(String mTitle, String mDescription, String mAddedBy, String mCourse, Date mDueOn) {
        title = mTitle;
        description = mDescription;
        addedBy = mAddedBy;
        course = mCourse;
        dueOn = mDueOn;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public String getCourse() {
        return course;
    }

    public Date getDueOn() {
        return dueOn;
    }
}
