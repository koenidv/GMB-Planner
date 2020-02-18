package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEdit;
    private OkHttpClient client = new OkHttpClient.Builder().cookieJar(new MyCookieJar()).build();
    private Gson gson = new Gson();
    private ArrayList<Change> mChangeList = new ArrayList<>();

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

    /**
     * Downloads the entire user web page from mosbacher-berg.de
     *
     * @param input URL of the requested page (mosbacher-berg.de), username and password
     * @return Source code of the user page
     */
    @Override
    protected String doInBackground(String... input) {

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
            ioe.printStackTrace();
            return "";
        }
    }

    /**
     * Stores all changes in SharedPrefs
     *
     * @param result The entire user web page from mosbacher-berg.de
     */
    @Override
    protected void onPostExecute(String result) {
        try {
            if (result.contains("Anmelden")) {
                // Login failed
                prefsEdit.putString("pass", "").commit();
                // Broadcast to show the credentials sheet
                Intent intent = new Intent("invalidateCredentials");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {
                // Login succeeded
                result = result.substring(result.indexOf("<div class=\"view-content\">"));
                result = result.substring(result.indexOf("<tbody>") + 7, result.indexOf("</tbody>"));

                while (result.contains("</tr>")) { // Can't check for <tr> as tag might include classes
                    mChangeList.add(new Change(result.substring(result.indexOf(">") + 1, result.indexOf("</tr>"))));
                    result = result.substring(result.indexOf("</tr>") + 5);
                }
                prefsEdit.putString("changes", gson.toJson(mChangeList));

                // Add all courses that have not yet been seen
                List<String> allCourses = new ArrayList<>();
                try {
                    allCourses = new ArrayList<>(Arrays.asList(gson.fromJson(prefs.getString("allCourses", ""), String[].class)));
                } catch (NullPointerException ignored) {
                }
                for (Change change : mChangeList)
                    if (!allCourses.contains(change.getCourse()))
                        allCourses.add(change.getCourse());

                prefsEdit.putString("allCourses", gson.toJson(allCourses));
                prefsEdit.commit();
            }
        } catch (IndexOutOfBoundsException ignored) {
            // Do nothing if network request failed
        }

        // Broadcast to refresh UI
        Intent intent = new Intent("changesRefreshed");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Store cookies from the login page. Website will deny access otherwise.
     */
    public class MyCookieJar implements CookieJar {

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
