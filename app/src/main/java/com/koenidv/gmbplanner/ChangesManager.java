package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//  Created by koenidv on 16.02.2020.
// This class handles all the network requests
public class ChangesManager extends AsyncTask<String, String, String> {

    @SuppressLint("StaticFieldLeak")
    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEdit;
    private OkHttpClient client = new OkHttpClient.Builder().cookieJar(new MyCookieJar()).build();
    private Gson gson = new Gson();
    private ArrayList<Change> mChangeList = new ArrayList<>();
    private boolean isBackground = false;

    /**
     * Sets the context and executes the changes request
     *
     * @param mContext Application context
     */
    @SuppressLint("CommitPrefEdits")
    public void refreshChanges(Context mContext) {
        context = mContext;
        prefs = mContext.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        prefsEdit = prefs.edit();
        execute("https://mosbacher-berg.de/user/login", prefs.getString("name", ""), prefs.getString("pass", ""));
    }

    void refreshChanges(Context mContext, boolean mIsBackground) {
        isBackground = mIsBackground;
        refreshChanges(mContext);
    }

    /**
     * Downloads the entire user web page from mosbacher-berg.de
     *
     * @param input URL of the requested page (mosbacher-berg.de), username and password
     * @return Source code of the user page
     */
    @Override
    protected String doInBackground(String... input) {

        Intent refreshing = new Intent("refreshing");
        LocalBroadcastManager.getInstance(context).sendBroadcast(refreshing);

        RequestBody formBody = new FormBody.Builder()
                .add("name", input[1])
                .add("pass", input[2])
                .add("form_id", "user_login")
                .add("op", "Anmelden")
                .build();
        Request request = new Request.Builder()
                .url(input[0])
                .post(formBody)
                .header("User-Agent", "GMB Planner")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException ioe) {
            if (!isBackground) {
                // Display offline error
                Intent intent = new Intent("refreshFailed");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                // Update when device goes online
                Constraints workConstraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RefreshWorker.class)
                        .setConstraints(workConstraints)
                        .addTag("changesRefreshWhenOnline")
                        .build();
                WorkManager.getInstance(context).enqueueUniqueWork("changesRefreshWhenOnline", ExistingWorkPolicy.KEEP, workRequest);
            }

            return "";
        }
    }

    /**
     * Stores all changes in SharedPrefs
     *
     * @param result The entire user web page from mosbacher-berg.de
     */

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onPostExecute(String result) {

        List<Change> previousChanges = gson.fromJson(prefs.getString("changes", ""), ListType.CHANGES);
        Resolver resolver = new Resolver();

        try {
            if (result.contains("Anmelden")) {
                // Login failed
                prefsEdit.putString("pass", "").commit();
                // Broadcast to show the credentials sheet
                Intent intent = new Intent("invalidateCredentials");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (result.contains("Vertretungsplan")) {
                // Login succeeded
                String lastChange = result.substring(result.indexOf("Importierte Daten wurden hochgeladen: ") + 38);
                lastChange = lastChange.substring(0, lastChange.indexOf("<"));

                // Get name and grade
                String personname = "";
                String grade = "";
                try {
                    personname = result.substring(result.indexOf("page-title\">") + 12);
                    personname = personname.substring(0, personname.indexOf("<"));
                    grade = result.substring(result.indexOf("Stufe ") + 6);
                    grade = grade.substring(0, grade.indexOf("<"));
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }

                // Update courses and timetable if grade has changed
                if (!grade.equals(prefs.getString("grade", ""))) {
                    prefsEdit.putLong("lastCourseRefresh", 0)
                            .putLong("lastTimetableRefresh", 0)
                            .apply();
                }

                // Parse website to changes
                try {
                    result = result.substring(result.indexOf("<div class=\"view-content\">"));
                    result = result.substring(result.indexOf("<tbody>") + 7, result.indexOf("</tbody>"));

                    while (result.contains("</tr>")) { // Can't check for <tr> as tag might include classes
                        mChangeList.add(new Change(result.substring(result.indexOf(">") + 1, result.indexOf("</tr>"))));
                        result = result.substring(result.indexOf("</tr>") + 5);
                    }
                } catch (IndexOutOfBoundsException ignored) {
                    // Currently no changes
                }

                //todo debugging
                mChangeList.add(new Change("EVA", "Fr 13.3.", "1 - 2", "D-GK-5", "A13", "Kow"));
                mChangeList.add(new Change("Raum", "Mo 9.3.", "5 - 6", "E-GK-3", "M120", "Asc"));
                mChangeList.add(new Change("Klausur", "DO 12.3.", "3 - 4", "PH-LK-1", "E14", "Fgr"));

                // Add all courses that have not yet been seen and add changes to courses
                // Get all courses
                Map<String, Course> courses = gson.fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);

                if (courses != null) {
                    // Clear changes from all courses
                    for (Map.Entry<String, Course> map : courses.entrySet()) {
                        map.getValue().clearChanges();
                    }
                    // Add each change to the according course
                    for (Change change : mChangeList) {
                        // Add course if it's not already added
                        if (courses.get(change.getCourseString()) == null)
                            courses.put(change.getCourseString(), change.getCourse());

                        Objects.requireNonNull(courses.get(change.getCourseString())).addChange(change);
                    }

                    //todo debugging
                    courses.get("D-GK-5").addGrade(new Grade("Testklausur", 7f, Grade.TYPE_EXAM));
                    courses.get("M-GK-3").addGrade(new Grade("Testklausur", 13f, Grade.TYPE_EXAM));
                    courses.get("POWI-LK-1").addGrade(new Grade("EPO", 11f, Grade.TYPE_PARTICIPATION_PARTIAL));
                    courses.get("INFO-GK-1").addGrade(new Grade("Mitarbeit", 15f, Grade.TYPE_PARTICIPATION));

                }

                prefsEdit
                        .putString("changes", gson.toJson(mChangeList))
                        .putString("courses", gson.toJson(courses))
                        .putString("grade", grade)
                        .putString("realname", personname)
                        .putString("lastChange", lastChange)
                        .putLong("lastRefresh", Calendar.getInstance().getTimeInMillis());
                prefsEdit.apply();

                // Get course presets for the first time or after 2 weeks
                if (prefs.getString("courses", "").length() == 0
                        || Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastCourseRefresh", 0) > 1209600 * 1000) {
                    // Get course presets from koenidv.de
                    final String finalGrade = grade;
                    // Ignore first refresh broadcast, as this request will still be still running
                    Intent ignoreIntent = new Intent("refreshing");
                    ignoreIntent.putExtra("ignoreFirst", true);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(ignoreIntent);
                    new AsyncTask<String, String, String>() {
                        @Override
                        protected String doInBackground(String... mStrings) {
                            // Network request needs to be async
                            Request request = new Request.Builder()
                                    .url(context.getString(R.string.url_presets).replace("%grade", finalGrade.toLowerCase()))
                                    .header("User-Agent", "GMB Planner")
                                    .build();
                            try (Response response = client.newCall(request).execute()) {
                                String responseString = Objects.requireNonNull(response.body()).string();
                                // Remove newlines so that gson can parse the data
                                responseString = responseString.replace("\n", "");
                                // Parse
                                ArrayList<Course> presets = new ArrayList<>();
                                try {
                                    presets = gson.fromJson(responseString, ListType.COURSES);
                                } catch (NullPointerException npe) {
                                    // Not well formatted json or network error
                                    npe.printStackTrace();
                                }
                                // Add all loaded courses if they aren't already in the list
                                Map<String, Course> allCourseObjectsAsync = new HashMap<>();

                                Map<String, Course> allCourseObjectsAsyncPrefs = gson.fromJson(prefs.getString("courses", ""), ListType.COURSEMAP);
                                if (allCourseObjectsAsyncPrefs != null)
                                    allCourseObjectsAsync = allCourseObjectsAsyncPrefs;

                                for (Course preset : presets)
                                    if (allCourseObjectsAsync.get(preset.getCourse()) == null)
                                        allCourseObjectsAsync.put(preset.getCourse(), preset);
                                prefsEdit.putString("courses", gson.toJson(allCourseObjectsAsync))
                                        .putLong("lastCourseRefresh", Calendar.getInstance().getTimeInMillis())
                                        .commit();

                                // Broadcast to refresh UI
                                Intent doneIntent = new Intent("changesRefreshed");
                                doneIntent.putExtra("dontIgnore", true)
                                        .putExtra("setup_courses", true);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(doneIntent);
                            } catch (IOException | RuntimeException mE) {
                                Intent doneIntent = new Intent("changesRefreshed");
                                doneIntent.putExtra("dontIgnore", true)
                                        .putExtra("failed", true)
                                        .putExtra("setup_courses", true);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(doneIntent);
                                mE.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                }

                // Get timetable for the first time or after 8 weeks
                if (Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastTimetableRefresh", 0) > 4838400L * 1000) {
                    // Get timetable from koenidv.de
                    final String finalGrade = grade;
                    // Ignore first refresh broadcast, as this request will still be still running
                    Intent ignoreIntent = new Intent("refreshing");
                    ignoreIntent.putExtra("ignoreFirst", true);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(ignoreIntent);
                    new AsyncTask<String, String, String>() {
                        @Override
                        protected String doInBackground(String... mStrings) {
                            // Network request needs to be async
                            Request request = new Request.Builder()
                                    .url(context.getString(R.string.url_timetable).replace("%grade", finalGrade.toLowerCase()))
                                    .header("User-Agent", "GMB Planner")
                                    .build();
                            try (Response response = client.newCall(request).execute()) {
                                String responseString = Objects.requireNonNull(response.body()).string();
                                // Remove newlines so that gson can parse the data
                                responseString = responseString.replace("\n", "");
                                Lesson[][][] lessons = gson.fromJson(responseString, Lesson[][][].class);

                                prefsEdit.putString("timetableAll", gson.toJson(lessons))
                                        .putLong("lastTimetableRefresh", Calendar.getInstance().getTimeInMillis())
                                        .commit();

                                // Broadcast to refresh UI
                                Intent doneIntent = new Intent("changesRefreshed");
                                doneIntent.putExtra("dontIgnore", true)
                                        .putExtra("coursesChanged", true)
                                        .putExtra("setup_timetable", true);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(doneIntent);
                            } catch (IOException | RuntimeException mE) {
                                Intent doneIntent = new Intent("changesRefreshed");
                                doneIntent.putExtra("dontIgnore", true)
                                        .putExtra("failed", true)
                                        .putExtra("setup_timetable", true);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(doneIntent);
                                mE.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                }

            } else if (result.contains("Berechtigungscode")) {
                // Prompt to redeem access code first
                Intent redeemintent = new Intent(Intent.ACTION_VIEW);
                redeemintent.setData(Uri.parse("https://mosbacher-berg.de/school_coderole/redeem"));
                redeemintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(redeemintent);
                Toast.makeText(context, context.getString(R.string.credentials_redeem_code_prompt), Toast.LENGTH_LONG).show();
            }
        } catch (IndexOutOfBoundsException ignored) {
        }

        // Broadcast to refresh UI
        Intent intent = new Intent("changesRefreshed");
        intent.putExtra("setup_changes", true);
        if (!prefs.getBoolean("completed_first", false)) {
            prefsEdit.putBoolean("completed_first", true).apply();
            intent.putExtra("first_update", true);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Send a notification for new changes within myCourses
        if (isBackground) {
            List<Change> newChanges = new ArrayList<>();
            StringBuilder notificationString = new StringBuilder();

            // Add all new favorite changes
            for (Change thisChange : mChangeList) {
                if (!previousChanges.contains(thisChange)) {
                    if (resolver.isFavorite(thisChange.getCourseString(), context)) {
                        newChanges.add(thisChange);
                    }
                }
            }

            // Create notification text with new changes
            String lastDate = "";
            for (Change thisChange : newChanges) {
                if (!lastDate.equals(thisChange.getDate())) {
                    lastDate = thisChange.getDate();
                    notificationString.append(resolver.resolveDate(thisChange.getDate(), context)).append(":\n");
                }
                notificationString.append(new Resolver().resolveCourse(thisChange.getCourseString(), context));
                if (thisChange.isTeacherChanged()) {
                    notificationString.append(context.getString(R.string.change_connect_teacher)).append(new Resolver().resolveTeacher(thisChange.getTeacherNew()));
                }
                if (thisChange.isRoomChanged()) {
                    notificationString.append(context.getString(R.string.change_connect_room)).append(thisChange.getRoomNew());
                }
                if (!thisChange.getType().equals("Raum") && !thisChange.getType().equals("Vertretung")) {
                    notificationString.append(" ").append(thisChange.getType());
                }
                notificationString.append(",\n");
            }

            if (!newChanges.isEmpty()) {
                notificationString.delete(notificationString.lastIndexOf(","), notificationString.length());

                // Create an explicit intent for an Activity in your app
                Intent notificationIntent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

                Notification.Builder notification = new Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notification.setChannelId("changes");
                }
                notification
                        .setSmallIcon(R.drawable.ic_notification)
                        .setSubText(context.getResources().getQuantityString(R.plurals.notification_title, newChanges.size()))
                        .setContentText(notificationString.toString())
                        .setStyle(new Notification.BigTextStyle()
                                .bigText(notificationString.toString()))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(0, notification.build());
            }
        }
    }

    /**
     * Store cookies from the login page. Website will deny access otherwise.
     */
    public static class MyCookieJar implements CookieJar {

        private List<Cookie> cookies;

        @Override
        public void saveFromResponse(@NotNull HttpUrl url, @NotNull List<Cookie> cookies) {
            this.cookies = cookies;
        }

        @NotNull
        @Override
        public List<Cookie> loadForRequest(@NotNull HttpUrl url) {
            if (cookies != null)
                return cookies;
            return new ArrayList<>();

        }
    }
}