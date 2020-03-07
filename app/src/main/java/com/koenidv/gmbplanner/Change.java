package com.koenidv.gmbplanner;

import androidx.annotation.Nullable;

//  Created by koenidv on 15.02.2020.
public class Change {

    private String type;
    private String date;
    private String time;
    private String schoolclass;
    private String course;
    private String courseNew;
    private boolean courseChanged;
    private String room;
    private String roomNew;
    private boolean roomChanged;
    private String teacher;
    private String teacherNew;
    private boolean teacherChanged;
    private String information;

    /**
     * Parses the imported html table row to a change object
     */
    Change(String rawInput) {
        try {
            date = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            schoolclass = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            time = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            teacher = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            room = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            course = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            information = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
            rawInput = rawInput.substring(rawInput.indexOf("</td>") + 5);
            type = rawInput.substring(rawInput.indexOf(">") + 1, rawInput.indexOf("</td>")).trim();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        courseNew = course.substring(course.indexOf(" &rArr; ") + 8);
        course = course.substring(0, course.indexOf(" &rArr; "));
        roomNew = room.substring(room.indexOf(" &rArr; ") + 8);
        room = room.substring(0, room.indexOf(" &rArr; "));
        teacherNew = teacher.substring(teacher.indexOf(" &rArr; ") + 8);
        teacher = teacher.substring(0, teacher.indexOf(" &rArr; "));

        if (type.equals("Klausur")) {
            course = courseNew;
            room = roomNew;
            teacher = teacherNew;
        }

        courseChanged = !courseNew.equals(course);
        roomChanged = !roomNew.equals("Sek") && !roomNew.equals(room);
        teacherChanged = !teacherNew.equals("+") && !teacherNew.equals(teacher);
    }

    Change(String type, String date, String time, String course, String room, String teacher) {
        this.type = type;
        this.date = date;
        this.time = time;
        this.course = course;
        this.room = room;
        this.teacher = teacher;

        this.courseChanged = false;
        this.roomChanged = false;
        this.teacherChanged = false;

        this.courseNew = course;
        this.roomNew = "Sek";
        this.teacherNew = teacher;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        try {
            Change toTest = (Change) obj;
            return toTest != null
                    && toTest.getCourseString().equals(course)
                    && toTest.getDate().equals(date)
                    && toTest.getTime().equals(time)
                    && toTest.getType().equals(type);
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    String getSchoolclass() {
        return schoolclass;
    }

    public String getCourseString() {
        return course;
    }

    public String getCourseStringNew() {
        return courseNew;
    }

    public Course getCourse() {
        return new Course(getCourseString(), getTeacher());
    }

    public String getRoom() {
        return room;
    }

    public String getRoomNew() {
        return roomNew;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getTeacherNew() {
        return teacherNew;
    }

    String getInformation() {
        return information;
    }

    public boolean isCourseChanged() {
        return courseChanged;
    }

    public boolean isRoomChanged() {
        return roomChanged;
    }

    public boolean isTeacherChanged() {
        return teacherChanged;
    }
}
