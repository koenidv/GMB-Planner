package com.koenidv.gmbplanner;

//  Created by koenidv on 04.03.2020.
public class Lesson {

    private String course;
    private String room;

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
}
