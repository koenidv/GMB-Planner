package com.koenidv.gmbplanner;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

//  Created by koenidv on 04.03.2020.
public class Lesson {

    @Keep
    @SerializedName("course")
    private String course;
    @Keep
    @SerializedName("room")
    private String room;

    Lesson(String course, String room) {
        this.course = course;
        this.room = room;
    }

    String getCourse() {
        return course == null ? "" : course;
    }
    public String getRoom() {
        return room;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        try {
            Lesson testFor = (Lesson) obj;
            return testFor != null && testFor.course.equals(course);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            return false;
        }
    }
}
