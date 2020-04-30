package com.koenidv.gmbplanner;

import java.util.Calendar;
import java.util.Date;

//  Created by koenidv on 30.04.2020.
public class Grade {

    final static int TYPE_EXAM = 0, TYPE_PARTICIPATION = 1, TYPE_PARTICIPATION_PARTIAL = 2, TYPE_OTHER = 3;

    private String name;
    private Date date = Calendar.getInstance().getTime();
    private Float grade, weight = 1f;
    private int type;

    Grade() {
    }

    Grade(String name, Float grade, int type) {
        this.name = name;
        this.grade = grade;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String mName) {
        name = mName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date mDate) {
        date = mDate;
    }

    public Float getGrade() {
        return grade;
    }

    public void setGrade(Float mGrade) {
        grade = mGrade;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float mWeight) {
        weight = mWeight;
    }

    public int getType() {
        return type;
    }

    public void setType(int mType) {
        type = mType;
    }
}
