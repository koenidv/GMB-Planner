package com.koenidv.gmbplanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

//  Created by koenidv on 20.02.2020.
public class Resolver {

    /**
     * Resolves absolute date Strings to relative date Strings
     *
     * @param dateString Date as imported, like "Di 25.02."
     * @param context    Application context to get resources
     * @return Relative date as string, eg "Today", "Tomorrow", etc.
     */
    public String resolveDate(String dateString, Context context) {
        Calendar date = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd.MM.", Locale.GERMAN);
        try {
            date = Calendar.getInstance();
            date.setTime(Objects.requireNonNull(dateFormat.parse(dateString)));
            date.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        } catch (ParseException mE) {
            mE.printStackTrace();
        }

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long difference = date.getTimeInMillis() - today.getTimeInMillis();

        if (difference < 86400000) {
            return context.getString(R.string.today);
        } else if (difference < 2 * 86400000) {
            return context.getString(R.string.tomorrow);
        }/* else if (difference < 3 * 86400000) {
            return mContext.getString(R.string.aftertomorrow);
        }*/ else {
            SimpleDateFormat returnFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            return returnFormat.format(date.getTimeInMillis());
        }

    }

    /**
     * @param dateString The change's date: EE dd.MM.
     * @param timeString The change's period(s): p1( - p2)
     * @return [day: 0-6, mo-sun][startperiod][endperiod]
     */
    int[] resolvePeriod(String dateString, String timeString) {
        int[] result = new int[3];
        // Parse date
        Calendar date = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd.MM.", Locale.GERMAN);
        try {
            date = Calendar.getInstance();
            date.setTime(Objects.requireNonNull(dateFormat.parse(dateString)));
            date.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        } catch (ParseException mE) {
            mE.printStackTrace();
        }

        // Weekday
        int weekDay = date.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekDay < 0) weekDay += 7;
        result[0] = weekDay;

        //Periods
        if (!timeString.contains("-")) {
            result[1] = Integer.parseInt(timeString) - 1;
            result[2] = Integer.parseInt(timeString) - 1;
        } else {
            result[1] = Integer.parseInt(timeString.substring(0, timeString.indexOf(" - "))) - 1;
            result[2] = Integer.parseInt(timeString.substring(timeString.indexOf(" - ") + 3)) - 1;
        }

        return result;
    }

    /**
     * Resolves course descriptions to subject names
     *
     * @param courseName The description of a course, eg "PH-LK-1"
     * @param context    Context to get resources
     * @return The entire subject name as String, eg "Physics"
     */
    public String resolveCourse(String courseName, Context context) {
        StringBuilder name = new StringBuilder();
        if (courseName == null || courseName.length() < 3) {
            return "";
        } else if (courseName.startsWith("D-")) {
            name.append(context.getString(R.string.course_german));
        } else if (courseName.startsWith("E-")) {
            name.append(context.getString(R.string.course_english));
        } else if (courseName.startsWith("L-")) {
            name.append(context.getString(R.string.course_latin));
        } else if (courseName.startsWith("SPO-")) {
            name.append(context.getString(R.string.course_sports));
        } else if (courseName.startsWith("F-")) {
            name.append(context.getString(R.string.course_french));
        } else if (courseName.startsWith("G-")) {
            name.append(context.getString(R.string.course_history));
        } else if (courseName.startsWith("INFO-")) {
            name.append(context.getString(R.string.course_it));
        } else if (courseName.startsWith("KU-")) {
            name.append(context.getString(R.string.course_arts));
        } else if (courseName.startsWith("MU-")) {
            name.append(context.getString(R.string.course_music));
        } else if (courseName.startsWith("M-")) {
            name.append(context.getString(R.string.course_maths));
        } else if (courseName.startsWith("PH-")) {
            name.append(context.getString(R.string.course_physics));
        } else if (courseName.startsWith("BIO-")) {
            name.append(context.getString(R.string.course_biology));
        } else if (courseName.startsWith("CH-")) {
            name.append(context.getString(R.string.course_chemistry));
        } else if (courseName.startsWith("EK-")) {
            name.append(context.getString(R.string.course_geography));
        } else if (courseName.startsWith("POWI-")) {
            name.append(context.getString(R.string.course_politics));
        } else if (courseName.startsWith("RKA-") || courseName.startsWith("REV-")) {
            name.append(context.getString(R.string.course_religion));
        } else if (courseName.startsWith("ETHI-")) {
            name.append(context.getString(R.string.course_ethics));
        } else if (courseName.startsWith("GBi-")) {
            name.append(context.getString(R.string.course_history_bili));
        } else if (courseName.startsWith("AG-ConcB")) {
            name.append(context.getString(R.string.course_concb));
        } else {
            name.append(courseName.substring(0, courseName.indexOf('-')));
        }

        if (courseName.contains("-LK")) {
            name.append(context.getString(R.string.course_intensified));
        } else if (courseName.contains("-PF")) {
            name.append(context.getString(R.string.course_examination));
        }
        return name.toString();
    }

    String resolveCourse(String courseName, Context context, boolean courseNumber) {
        String course = resolveCourse(courseName, context);
        if (courseNumber)
            try {
                course += " " + courseName.substring(courseName.lastIndexOf('-') + 1);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
        return course;
    }

    /**
     * Resolves course descriptions to short subject names
     *
     * @param courseName The description of a course, eg "PH-LK-1"
     * @param context    Context to get resources
     * @return The short subject name as String, eg "Phy"
     */
    String resolveCourseShort(String courseName, Context context) {
        StringBuilder name = new StringBuilder();
        if (courseName == null || courseName.length() < 3) {
            return "";
        } else if (courseName.startsWith("D-")) {
            name.append(context.getString(R.string.course_german_short));
        } else if (courseName.startsWith("E-")) {
            name.append(context.getString(R.string.course_english_short));
        } else if (courseName.startsWith("L-")) {
            name.append(context.getString(R.string.course_latin_short));
        } else if (courseName.startsWith("SPO-")) {
            name.append(context.getString(R.string.course_sports_short));
        } else if (courseName.startsWith("F-")) {
            name.append(context.getString(R.string.course_french_short));
        } else if (courseName.startsWith("G-")) {
            name.append(context.getString(R.string.course_history_short));
        } else if (courseName.startsWith("INFO-")) {
            name.append(context.getString(R.string.course_it_short));
        } else if (courseName.startsWith("KU-")) {
            name.append(context.getString(R.string.course_arts_short));
        } else if (courseName.startsWith("MU-")) {
            name.append(context.getString(R.string.course_music_short));
        } else if (courseName.startsWith("M-")) {
            name.append(context.getString(R.string.course_maths_short));
        } else if (courseName.startsWith("PH-")) {
            name.append(context.getString(R.string.course_physics_short));
        } else if (courseName.startsWith("BIO-")) {
            name.append(context.getString(R.string.course_biology_short));
        } else if (courseName.startsWith("CH-")) {
            name.append(context.getString(R.string.course_chemistry_short));
        } else if (courseName.startsWith("EK-")) {
            name.append(context.getString(R.string.course_geography_short));
        } else if (courseName.startsWith("POWI-")) {
            name.append(context.getString(R.string.course_politics_short));
        } else if (courseName.startsWith("RKA-") || courseName.startsWith("REV-")) {
            name.append(context.getString(R.string.course_religion_short));
        } else if (courseName.startsWith("ETHI-")) {
            name.append(context.getString(R.string.course_ethics_short));
        } else if (courseName.startsWith("GBi-")) {
            name.append(context.getString(R.string.course_history_bili_short));
        } else if (courseName.startsWith("AG-ConcB")) {
            name.append(context.getString(R.string.course_concb_short));
        } else {
            name.append(courseName.substring(0, courseName.indexOf('-')));
        }

        if (courseName.contains("-LK")) {
            name.append(context.getString(R.string.course_intensified_short));
        } else if (courseName.contains("-PF")) {
            name.append(context.getString(R.string.course_examination_short));
        }
        return name.toString();
    }

    String resolveCourseVeryShort(String courseName, Context context) {
        if (courseName == null) return "";
        if (courseName.contains("-LK")
                || courseName.contains("M-")
                || courseName.contains("D-")
                || courseName.contains("E-"))
            if (courseName.contains("POWI"))
                return "W";
            else
                return resolveCourse(courseName, context).substring(0, 1);
        else if (courseName.contains("BIO"))
            return "Bio";
        if (courseName.length() >= 2)
            return resolveCourse(courseName, context).substring(0, 2);
        else
            return courseName;
    }

    /**
     * Resolves course descriptions to their according colors
     *
     * @param courseName The description of a course
     * @param context    Context to get resources
     * @return The according color
     */
    int resolveCourseColor(String courseName, Context context) {
        if (courseName == null) {
            return context.getResources().getColor(R.color.spotShadowColor);
        } else if (courseName.startsWith("D-")) {
            return context.getResources().getColor(R.color.course_german);
        } else if (courseName.startsWith("E-")) {
            return context.getResources().getColor(R.color.course_english);
        } else if (courseName.startsWith("SPO-")) {
            return context.getResources().getColor(R.color.course_sports);
        } else if (courseName.startsWith("L-")) {
            return context.getResources().getColor(R.color.course_latin);
        } else if (courseName.startsWith("F-")) {
            return context.getResources().getColor(R.color.course_french);
        } else if (courseName.startsWith("G-") || courseName.startsWith("GBi-")) {
            return context.getResources().getColor(R.color.course_history);
        } else if (courseName.startsWith("INFO-")) {
            return context.getResources().getColor(R.color.course_it);
        } else if (courseName.startsWith("KU-")) {
            return context.getResources().getColor(R.color.course_arts);
        } else if (courseName.startsWith("MU-")) {
            return context.getResources().getColor(R.color.course_music);
        } else if (courseName.startsWith("M-")) {
            return context.getResources().getColor(R.color.course_maths);
        } else if (courseName.startsWith("PH-")) {
            return context.getResources().getColor(R.color.course_physics);
        } else if (courseName.startsWith("BIO-")) {
            return context.getResources().getColor(R.color.course_biology);
        } else if (courseName.startsWith("CH-")) {
            return context.getResources().getColor(R.color.course_chemistry);
        } else if (courseName.startsWith("EK-")) {
            return context.getResources().getColor(R.color.course_geography);
        } else if (courseName.startsWith("POWI-")) {
            return context.getResources().getColor(R.color.course_politics);
        } else if (courseName.startsWith("RKA-") || courseName.startsWith("REV-") || courseName.startsWith("ETHI-")) {
            return context.getResources().getColor(R.color.course_religion);
        } else {
            return context.getResources().getColor(R.color.spotShadowColor);
        }
    }

    /**
     * Resolves change types to their according colors
     *
     * @param type    The change type
     * @param context Context to get resources
     * @return The according color
     */
    int resolveTypeColor(String type, Context context) {
        switch (type.toLowerCase()) {
            case "eva":
                return context.getResources().getColor(R.color.type_eva);
            case "freisetzung":
                return context.getResources().getColor(R.color.type_freed);
            case "entfall":
                return context.getResources().getColor(R.color.type_cancelled);
            case "vertretung":
                return context.getResources().getColor(R.color.type_standin);
            case "statt-vertretung":
                return context.getResources().getColor(R.color.type_standin_instead);
            case "verlegung":
                return context.getResources().getColor(R.color.type_transfer);
            case "betreuung":
                return context.getResources().getColor(R.color.type_care);
            case "raum":
                return context.getResources().getColor(R.color.type_room);
            case "klausur":
                return context.getResources().getColor(R.color.type_exam);
            default:
                return context.getResources().getColor(R.color.type_other);
        }
    }

    /**
     * Resolves teacher shorthands to entire last names
     *
     * @param shorthand A teachers shorthand like "Fgr"
     * @return Their last name, if known, eg Fachinger
     */
    public String resolveTeacher(String shorthand) {
        if (shorthand == null)
            return "";
        switch (shorthand) {
            case "Adb":
                return "Adelsberger";
            case "Afh":
                return "Afghanyar";
            case "Asc":
                return "Ascheidt";
            case "Bal":
                return "Balser";
            case "Bar":
                return "Barth";
            case "Bec":
                return "Becker";
            case "Ber":
                return "Berger";
            case "Brö":
                return "Brömser";
            case "Cas":
                return "Cassier";
            case "Con":
                return "Del Conte";
            case "Det":
                return "Dettweiler";
            case "Els":
                return "Elsen";
            case "Esg":
                return "Essig";
            case "Ess":
                return "Esslinger";
            case "Fgr":
                return "Fachinger";
            case "F-W":
                return "Fechtig-Weinert";
            case "Gil":
                return "Gilsdorf";
            case "Glo":
                return "Glocker";
            case "Goe":
                return "Goetzmann";
            case "Gom":
                return "Gomes";
            case "Got":
                return "Gotschlich";
            case "Grn":
                return "Grün";
            case "Gro":
                return "Großmann";
            case "Ham":
                return "Hammes";
            case "Han":
                return "Hanßmann";
            case "Hbt":
                return "Herbst";
            case "Hei":
                return "Heinze";
            case "Hie":
                return "Hiebsch";
            case "Hin":
                return "Hinkel";
            case "Hlr":
                return "Heilhecker";
            case "Hns":
                return "Hansmann";
            case "Jaz":
                return "Jarzina";
            case "Ker":
                return "Kleer";
            case "Kli":
                return "Klie";
            case "Kmb":
                return "Krombach";
            case "Kow":
                return "Kowalewsky";
            case "K-S":
                return "Kury-Smythe";
            case "Kss":
                return "Kiss";
            case "Lip":
                return "Lipowsky";
            case "Lüt":
                return "Lüttmann";
            case "Mak":
                return "Makridis";
            case "Man":
                return "Manig";
            case "Mes":
                return "Menges";
            case "Mie":
                return "Miesen";
            case "Müc":
                return "Mückenberger";
            case "NiG":
                return "Nicolay";
            case "Osc":
                return "Oschwald";
            case "Pau":
                return "Pausch";
            case "Pei":
                return "Peich";
            case "Pin":
                return "Piniek";
            case "Pod":
                return "Podehl";
            case "Rei":
                return "Reich";
            case "Rüf":
                return "Rüffel";
            case "Sbr":
                return "Schreiber";
            case "Sdr":
                return "Schneider";
            case "Sha":
                return "Shahvari";
            case "Sht":
                return "Scheinert";
            case "Sol":
                return "Scholzen";
            case "Sta":
                return "Stahl";
            case "S-T":
                return "Siercke";
            case "Tre":
                return "Treu";
            case "Vac":
                return "Vachek";
            case "Wag":
                return "Wagner";
            case "Wan":
                return "Wangen";
            case "Wdr":
                return "Weidauer";
            case "Wdt":
                return "Weidt";
            case "Web":
                return "Weber";
            case "Wei":
                return "Weimer";
            case "Wen":
                return "Wendel";
            case "Wer":
                return "Werner";
            case "Zir":
                return "Zirkel";
            default:
                return shorthand;
        }
    }

    /**
     * Resolves teacher shorthands to the first char of their first name
     *
     * @param shorthand A teachers shorthand like "Fgr"
     * @return Their first name initial, if known, or "unknown" if not
     */
    String resolveTeacherInitial(String shorthand) {
        switch (shorthand) {
            case "Adb":
            case "Wdt":
            case "Sol":
            case "Lip":
            case "Hin":
            case "Det":
                return "c";
            case "Asc":
            case "Sbr":
            case "Osc":
            case "K-S":
            case "Hie":
                return "k";
            case "Bar":
            case "Wen":
            case "Vac":
            case "Pau":
            case "Kss":
            case "Kow":
            case "Hlr":
            case "Hbt":
            case "Han":
            case "Ham":
            case "Gro":
            case "Gom":
            case "Goe":
            case "Ess":
                return "m";
            case "Bec":
            case "Gil":
            case "Esg":
            case "Els":
            case "Ber":
            case "Wag":
            case "Tre":
            case "Sdr":
            case "Ker":
            case "Man":
                return "a";
            case "Brö":
            case "Mie":
            case "Cas":
                return "p";
            case "Con":
            case "Glo":
            case "Sht":
            case "Rei":
            case "Pod":
            case "Lüt":
            case "Hns":
                return "s";
            case "Fgr":
            case "S-T":
            case "NiG":
            case "Kli":
                return "g";
            case "F-W":
            case "Mes":
            case "Grn":
            case "Wdr":
                return "j";
            case "Got":
            case "Zir":
                return "b";
            case "Hei":
            case "Sta":
            case "Pin":
                return "t";
            case "Jaz":
                return "r";
            case "Kmb":
                return "w";
            case "Mak":
            case "Wan":
                return "d";
            case "Müc":
            case "Sha":
            case "Pei":
                return "h";
            case "Rüf":
            case "Wer":
                return "f";
            case "Web":
                return "l";
            case "Wei":
                return "u";
            case "Afh":
            case "Bal":
                return "n";
            default:
                return "unknown";
        }
    }

    Course getCourse(String courseName, Context context) {
        // Get all changes from sharedPrefs
        Map<String, Course> courses = (new Gson()).fromJson(context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE).getString("courses", ""), ListType.COURSEMAP);
        if (courses == null) return null;
        return courses.get(courseName);
    }

    /**
     * Checks whether a course is added to the favorites list
     *
     * @param course  The course to check
     * @param context Application context to get SharedPrefs
     * @return If course is added to my plan
     */
    public boolean isFavorite(String course, Context context) {
        List<String> myCourses = new ArrayList<>();
        SharedPreferences prefs = Objects.requireNonNull(context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE));
        try {
            myCourses = Arrays.asList((new Gson()).fromJson(prefs.getString("myCourses", ""), String[].class));
        } catch (NullPointerException ignored) {
        }
        // Convert to string so that the list can contain a note about the teacher (eg course (teacher))
        return course != null && myCourses.toString().toUpperCase().contains(course.toUpperCase());
    }

    float dpToPx(float dp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
