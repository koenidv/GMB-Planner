package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
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
    void refreshChanges(Context mContext) {
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

        Type listType = new TypeToken<ArrayList<Change>>() {
        }.getType();
        List<Change> previousChanges = gson.fromJson(prefs.getString("changes", ""), listType);
        Resolver resolver = new Resolver();

        try {
            if (result.contains("Anmelden")) {
                // Login failed
                prefsEdit.putString("pass", "").commit();
                // Broadcast to show the credentials sheet
                Intent intent = new Intent("invalidateCredentials");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {
                // Login succeeded
                String lastChange = result.substring(result.indexOf("Importierte Daten wurden hochgeladen: ") + 38);
                lastChange = lastChange.substring(0, lastChange.indexOf("<"));

                String grade = prefs.getString("grade", "");
                if (grade.isEmpty()) {
                    grade = result.substring(result.indexOf("Stufe ") + 6);
                    grade = grade.substring(0, grade.indexOf("<"));
                }

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

                prefsEdit.putString("changes", gson.toJson(mChangeList));

                // Add all courses that have not yet been seen
                List<String> allCourses = new ArrayList<>();
                try {
                    allCourses = new ArrayList<>(Arrays.asList(gson.fromJson(prefs.getString("allCourses", ""), String[].class)));
                } catch (NullPointerException ignored) {
                }
                // Get course presets for the first time or after 2 weeks
                if (Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastCourseRefresh", 0) > 1209600 * 1000) {
                    // Get course presets from koenidv.de
                    final String finalGrade = grade;
                    new AsyncTask<String, String, String>() {
                        @Override
                        protected String doInBackground(String... mStrings) {
                            // Ignore first refresh broadcast, as this request will still be still running
                            Intent ignoreIntent = new Intent("refreshing");
                            ignoreIntent.putExtra("ignoreFirst", true);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(ignoreIntent);

                            // Network request needs to be async
                            Request request = new Request.Builder()
                                    .url(context.getString(R.string.url_presets).replace("%grade", finalGrade.toLowerCase()))
                                    .header("User-Agent", "GMB Planner")
                                    .build();
                            try (Response response = client.newCall(request).execute()) {
                                String responseString = Objects.requireNonNull(response.body()).string();
                                // Remove newlines so that gson can parse the data
                                responseString = responseString.replace("\n", "");
                                String[] presets = gson.fromJson(responseString, String[].class);
                                // Add all loaded courses if they aren't already in the list
                                List<String> allCoursesAsync = new ArrayList<>(Arrays.asList(gson.fromJson(prefs.getString("allCourses", ""), String[].class)));
                                for (String preset : presets)
                                    if (!allCoursesAsync.contains(preset))
                                        allCoursesAsync.add(preset);
                                prefsEdit.putString("allCourses", gson.toJson(allCoursesAsync))
                                        .putLong("lastCourseRefresh", Calendar.getInstance().getTimeInMillis())
                                        .commit();

                                // Broadcast to refresh UI
                                Intent doneIntent = new Intent("changesRefreshed");
                                doneIntent.putExtra("dontIgnore", true);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(doneIntent);
                            } catch (IOException | RuntimeException mE) {
                                mE.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                }
                for (Change change : mChangeList)
                    if (!allCourses.toString().toUpperCase().contains(change.getCourse().toUpperCase()))
                        allCourses.add(change.getCourse() + " (" + change.getTeacher() + ")");

                prefsEdit.putString("allCourses", gson.toJson(allCourses))
                        .putString("lastChange", lastChange)
                        .putLong("lastRefresh", Calendar.getInstance().getTimeInMillis());
                prefsEdit.commit();
            }
        } catch (IndexOutOfBoundsException ignored) {
        }

        // Broadcast to refresh UI
        Intent intent = new Intent("changesRefreshed");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Send a notification for new changes within myCourses
        if (isBackground) {
            List<Change> newChanges = new ArrayList<>();
            StringBuilder notificationString = new StringBuilder();

            // Add all new favorite changes
            for (Change thisChange : mChangeList) {
                if (previousChanges.contains(thisChange)) {
                    if (resolver.isFavorite(thisChange.getCourse(), context)) {
                        newChanges.add(thisChange);
                    }
                }
            }

            // Create notification text with new changes
            for (Change thisChange : newChanges) {
                notificationString.append(new Resolver().resolveCourse(thisChange.getCourse(), context));
                if (thisChange.isTeacherChanged()) {
                    notificationString.append(context.getString(R.string.change_connect_teacher)).append(new Resolver().resolveTeacher(thisChange.getTeacherNew()));
                }
                if (thisChange.isRoomChanged()) {
                    notificationString.append(context.getString(R.string.change_connect_room)).append(thisChange.getRoomNew());
                }
                if (!thisChange.getType().equals("Raum") && !thisChange.getType().equals("Vertretung")) {
                    notificationString.append(" ").append(thisChange.getType());
                }
                notificationString.append(" (").append(thisChange.getDate().substring(0, 2)).append("),\n");
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