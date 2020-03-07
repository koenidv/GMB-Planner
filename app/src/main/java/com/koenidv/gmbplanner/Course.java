package com.koenidv.gmbplanner;

import java.util.ArrayList;

import androidx.annotation.Nullable;

//  Created by koenidv on 04.03.2020.
public class Course {

    private String course;
    private String teacher;
    private String type;
    private ArrayList<Change> changes = new ArrayList<>();

    Course(String course, String teacher) {
        this.course = course;
        this.teacher = teacher;
        try {
            this.type = course.substring(0, course.indexOf('-'));
        } catch (NullPointerException ignored) {
            // Malformed course name
        }
    }

    Course() {
        course = "";
        teacher = "";
    }

    String getCourse() {
        return course;
    }

    String getTeacher() {
        return teacher;
    }

    public String getType() {
        return type;
    }

    void addChange(Change change) {
        changes.add(change);
    }

    void clearChanges() {
        changes.clear();
    }

    Change[] getChanges() {
        return changes.toArray(new Change[0]);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        try {
            if (obj == null) return course == null;
            return ((Course) obj).getCourse().equals(course);
        } catch (NullPointerException wrongType) {
            return false;
        }
    }
}
