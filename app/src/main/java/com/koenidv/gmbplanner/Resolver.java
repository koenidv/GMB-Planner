package com.koenidv.gmbplanner;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import kotlin.NotImplementedError;

//  Created by koenidv on 20.02.2020.
class Resolver {

    Date resolveDate(String date) {
        // Todo: Resolve dates like Mo, 24.2.
        throw new NotImplementedError("Not yet implemented..");
    }

    /**
     * Resolves course descriptions to subject names
     *
     * @param courseName The description of a course, eg "PH-LK-1"
     * @param context    Context to get resources
     * @return The entire subject name as String, eg "Physics"
     */
    String resolveCourse(String courseName, Context context) {
        StringBuilder name = new StringBuilder();
        if (courseName.contains("D-")) {
            name.append(context.getString(R.string.course_german));
        } else if (courseName.contains("E-")) {
            name.append(context.getString(R.string.course_english));
        } else if (courseName.contains("L-")) {
            name.append(context.getString(R.string.course_latin));
        } else if (courseName.contains("F-")) {
            name.append(context.getString(R.string.course_french));
        } else if (courseName.contains("G-")) {
            name.append(context.getString(R.string.course_history));
        } else if (courseName.contains("INFO-")) {
            name.append(context.getString(R.string.course_it));
        } else if (courseName.contains("KU-")) {
            name.append(context.getString(R.string.course_arts));
        } else if (courseName.contains("MU-")) {
            name.append(context.getString(R.string.course_music));
        } else if (courseName.contains("M-")) {
            name.append(context.getString(R.string.course_maths));
        } else if (courseName.contains("PH-")) {
            name.append(context.getString(R.string.course_physics));
        } else if (courseName.contains("BIO-")) {
            name.append(context.getString(R.string.course_biology));
        } else if (courseName.contains("CH-")) {
            name.append(context.getString(R.string.course_chemistry));
        } else if (courseName.contains("EK-")) {
            name.append(context.getString(R.string.course_geography));
        } else if (courseName.contains("POWI-")) {
            name.append(context.getString(R.string.course_politics));
        } else if (courseName.contains("SPO-")) {
            name.append(context.getString(R.string.course_sports));
        } else if (courseName.contains("RKA-") || courseName.contains("REV-")) {
            name.append(context.getString(R.string.course_german));
        } else if (courseName.contains("ETHI-")) {
            name.append(context.getString(R.string.course_ethics));
        } else if (courseName.contains("BI-")) {
            name.append(context.getString(R.string.coursr_bili));
        } else {
            name.append(courseName.substring(0, courseName.indexOf('-')));
        }
        if (courseName.contains("-LK")) {
            name.append(context.getString(R.string.course_intensified));
        }
        return name.toString();
    }

    /**
     * Resolves course descriptions to their according colors
     *
     * @param courseName The description of a course, eg "PH-LK-1"
     * @param context    Context to get resources
     * @return The entire subject name as String, eg "Physics"
     */
    int resolveCourseColor(String courseName, Context context) {
        if (courseName.contains("D-")) {
            return context.getResources().getColor(R.color.course_german);
        } else if (courseName.contains("E-")) {
            return context.getResources().getColor(R.color.course_english);
        } else if (courseName.contains("L-")) {
            return context.getResources().getColor(R.color.course_latin);
        } else if (courseName.contains("F-")) {
            return context.getResources().getColor(R.color.course_french);
        } else if (courseName.contains("G-") || courseName.contains("BI-")) {
            return context.getResources().getColor(R.color.course_history);
        } else if (courseName.contains("INFO-")) {
            return context.getResources().getColor(R.color.course_it);
        } else if (courseName.contains("KU-")) {
            return context.getResources().getColor(R.color.course_arts);
        } else if (courseName.contains("MU-")) {
            return context.getResources().getColor(R.color.course_music);
        } else if (courseName.contains("M-")) {
            return context.getResources().getColor(R.color.course_maths);
        } else if (courseName.contains("PH-")) {
            return context.getResources().getColor(R.color.course_physics);
        } else if (courseName.contains("BIO-")) {
            return context.getResources().getColor(R.color.course_biology);
        } else if (courseName.contains("CH-")) {
            return context.getResources().getColor(R.color.course_chemistry);
        } else if (courseName.contains("EK-")) {
            return context.getResources().getColor(R.color.course_geography);
        } else if (courseName.contains("POWI-")) {
            return context.getResources().getColor(R.color.course_politics);
        } else if (courseName.contains("SPO-")) {
            return context.getResources().getColor(R.color.course_sports);
        } else if (courseName.contains("RKA-") || courseName.contains("REV-") || courseName.contains("ETHI-")) {
            return context.getResources().getColor(R.color.course_religion);
        } else {
            return context.getResources().getColor(R.color.spotShadowColor);
        }
    }

    /**
     * Resolves teacher shorthands to entire last names
     *
     * @param shorthand A teachers shorthand like "Fgr"
     * @return Their last name, if known, eg Fachinger
     */
    String resolveTeacher(String shorthand) {
        switch (shorthand) {
            case "Adb":
                return "Adelsberger";
            case "Asc":
                return "Ascheidt";
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
            default:
                return "unknown";
        }
    }

    /**
     * Checks whether a course is added to the favorites list
     *
     * @param course  The course to check
     * @param context Application context to get SharedPrefs
     * @return If course is added to my plan
     */
    boolean isFavorite(String course, Context context) {
        List<String> myCourses = new ArrayList<>();
        SharedPreferences prefs = Objects.requireNonNull(context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE));
        try {
            myCourses = Arrays.asList((new Gson()).fromJson(prefs.getString("myCourses", ""), String[].class));
        } catch (NullPointerException ignored) {
        }
        // Convert to string so that the list can contain a note about the teacher (eg course (teacher))
        return myCourses.toString().toUpperCase().contains(course.toUpperCase());
    }
}
