package com.koenidv.gmbplanner;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.Calendar;

//  Created by koenidv on 16.06.2020.
public class SuggestionsProvider {

    public String provideSuggestion(Context context) {

        if (context == null) return null;

        // Simply returns the worst graded subject on that day if it's before 13h on a weekday at this point in time

        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;

        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 13 && weekDay >= 0 && weekDay <= 4) {

            // Get filtered timetable for today
            Lesson[][][] timetable = (new Gson()).fromJson(prefs.getString("timetableMine", ""), Lesson[][][].class);
            Lesson[][] today = timetable[weekDay];

            Float worstGrade = null;
            String worstCourse = null;
            Resolver resolver = new Resolver();

            for (Lesson[] lesson : today) {
                for (Lesson thisLesson : lesson) {
                    if ((worstGrade == null && resolver.getCourse(thisLesson.getCourse(), context).getGradeAverage() != null)
                            || (worstGrade != null && resolver.getCourse(thisLesson.getCourse(), context).getGradeAverage() < worstGrade)) {
                        worstGrade = resolver.getCourse(thisLesson.getCourse(), context).getGradeAverage();
                        worstCourse = thisLesson.getCourse();
                    }
                }
            }

            if (worstGrade != null && worstGrade < 13 && worstCourse != null) {
                return context.getString(R.string.suggestion_bad_course, resolver.resolveCourse(worstCourse, context));
            } else {
                return null;
            }
        } else {
            return null;
        }

    }
}
