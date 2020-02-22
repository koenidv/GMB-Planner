package com.koenidv.gmbplanner;

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
        courseChanged = !courseNew.equals(course);
        roomNew = room.substring(room.indexOf(" &rArr; ") + 8);
        room = room.substring(0, room.indexOf(" &rArr; "));
        roomChanged = !roomNew.equals("Sek") && !roomNew.equals(room);
        teacherNew = teacher.substring(teacher.indexOf(" &rArr; ") + 8);
        teacher = teacher.substring(0, teacher.indexOf(" &rArr; "));
        teacherChanged = !teacherNew.equals("+") && !teacherNew.equals(teacher);
    }

    Change(String mCourse, String mType, String mTeacher, boolean mTeacherChanged, String mDate) {
        course = mCourse;
        type = mType;
        teacher = mTeacher;
        teacherChanged = mTeacherChanged;
        teacherNew = teacher;
        date = mDate;
    }


    String getType() {
        return type;
    }

    String getDate() {
        return date;
    }

    String getTime() {
        return time;
    }

    String getSchoolclass() {
        return schoolclass;
    }

    public String getCourse() {
        return course;
    }

    String getCourseNew() {
        return courseNew;
    }

    String getRoom() {
        return room;
    }

    String getRoomNew() {
        return roomNew;
    }

    String getTeacher() {
        return teacher;
    }

    String getTeacherNew() {
        return teacherNew;
    }

    String getInformation() {
        return information;
    }

    boolean isCourseChanged() {
        return courseChanged;
    }

    boolean isRoomChanged() {
        return roomChanged;
    }

    boolean isTeacherChanged() {
        return teacherChanged;
    }
}
