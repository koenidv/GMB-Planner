package com.koenidv.gmbplanner;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

//  Created by koenidv on 04.03.2020.
public class Course {

    @Keep
    @SerializedName("course")
    private String course;
    @Keep
    @SerializedName("teacher")
    private String teacher;
    private String type;
    private ArrayList<Change> changes = new ArrayList<>();
    private ArrayList<Grade> grades = new ArrayList<>();
    private Float gradeAverage;

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

    private void doCalculateGradesAverage() {
        Float examsAverage = 0f, participationAverage = 0f, participationPartialAverage = 0f, othersAverage = 0f;
        Float examsCounter = 0f, participationCounter = 0f, participationPartialCounter = 0f, othersCounter = 0f;
        Date recent_participation = null;

        for (Grade thisGrade : grades) {
            if (thisGrade.getType() == Grade.TYPE_EXAM) {
                examsAverage += thisGrade.getGrade() * thisGrade.getWeight();
                examsCounter += thisGrade.getWeight();
            } else if (thisGrade.getType() == Grade.TYPE_PARTICIPATION) {
                participationAverage += thisGrade.getGrade() * thisGrade.getWeight();
                participationCounter += thisGrade.getWeight();
            } else if (thisGrade.getType() == Grade.TYPE_PARTICIPATION_PARTIAL) {
                if (recent_participation == null || thisGrade.getDate().compareTo(recent_participation) > 0) {
                    participationPartialAverage += thisGrade.getGrade() * thisGrade.getWeight();
                    participationPartialCounter += thisGrade.getWeight();
                    recent_participation = thisGrade.getDate();
                }
            } else {
                othersAverage += thisGrade.getGrade() * thisGrade.getWeight();
                othersCounter += thisGrade.getWeight();
            }
        }

        if (participationPartialCounter != 0) {
            participationPartialAverage /= participationPartialCounter;
            participationAverage += participationPartialAverage;
            participationCounter++;
        }

        examsAverage /= examsCounter;
        participationAverage /= participationCounter;
        othersAverage /= othersCounter;

        Float overallAverage = 0f;
        int overallCounter = 0;
        overallAverage += examsCounter == 0 ? 0 : 50 * examsAverage;
        overallCounter += examsCounter == 0 ? 0 : 50;
        overallAverage += participationCounter == 0 ? 0 : 50 * participationAverage;
        overallCounter += participationCounter == 0 ? 0 : 50;
        overallAverage += othersCounter == 0 ? 0 : 50 * othersAverage;
        overallCounter += othersCounter == 0 ? 0 : 20;

        overallAverage /= overallCounter;
        gradeAverage = overallAverage;
    }

    public void addGrade(Grade grade) {
        grades.add(grade);
        Collections.sort(grades, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));
        doCalculateGradesAverage();
    }

    public void removeGrade(int index) {
        grades.remove(index);
        doCalculateGradesAverage();
    }

    public void setGrade(int index, Grade grade) {
        grades.set(index, grade);
        Collections.sort(grades, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));
        doCalculateGradesAverage();
    }

    public Float getGradeAverage() {
        return gradeAverage;
    }
}
