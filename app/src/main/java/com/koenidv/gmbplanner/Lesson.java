package com.koenidv.gmbplanner;

import java.util.ArrayList;

//  Created by koenidv on 04.03.2020.
public class Lesson {

    private String course;
    private String room;
    private ArrayList<Change> changes;

    Lesson(String course, String room) {
        this.course = course;
        this.room = room;
    }

    String getCourse() {
        return course;
    }
    public String getRoom() {
        return room;
    }

    void addChange(Change change) {
        changes.add(change);
    }

    void clearChanges() {
        changes = new ArrayList<>();
    }

    Change[] getChanges() {
        return (Change[]) changes.toArray();
    }
}
