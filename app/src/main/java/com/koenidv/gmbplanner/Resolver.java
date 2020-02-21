package com.koenidv.gmbplanner;

import android.content.Context;

import java.util.Date;

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
        } else if (courseName.contains("ETH-")) {
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
                return "Krombacher";
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
}
