package com.koenidv.gmbplanner;

import java.util.ArrayList;

import androidx.annotation.Nullable;

//  Created by koenidv on 04.03.2020.
public class Course {

    private String course;
    private String teacher;
    private ArrayList<Change> changes;

    Course(String course, String teacher) {
        this.course = course;
        this.teacher = teacher;
    }

    String getCourse() {
        return course;
    }

    public String getTeacher() {
        return teacher;
    }

    void addChange(Change change) {
        changes.add(change);
    }

    void clearChanges() {
        changes.clear();
    }

    Change[] getChanges() {
        return (Change[]) changes.toArray();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj != null) {
            return ((Course) obj).getCourse().equals(course);
        }
        return false;
    }
}
