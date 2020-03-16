package com.koenidv.gmbplanner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.gson.Gson;
import com.koenidv.gmbplanner.ui.main.AllChangesFragment;
import com.koenidv.gmbplanner.ui.main.MyChangesFragment;
import com.koenidv.gmbplanner.widget.WidgetProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {

    public static CoursesSheet coursesSheet;
    static List<String> myCourses = new ArrayList<>();
    private SwipeRefreshLayout swiperefresh;
    CoursesTimetableSheet selectorSheet;
    boolean ignoreFirstRefreshed = false;
    private int UPDATE_REQUEST = 100;

    // Show that changes are refreshing when the broadcast "refreshing" is received
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            swiperefresh.setRefreshing(true);
            if (intent.getBooleanExtra("ignoreFirst", false))
                ignoreFirstRefreshed = true;
        }
    };
    // Show that refreshing is done when the broadcast "changesRefreshed" is received
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ignoreFirstRefreshed || intent.getBooleanExtra("dontIgnore", false))
                swiperefresh.setRefreshing(false);
            else
                ignoreFirstRefreshed = false;

            // Update timetable if courses have changed
            if (intent.getBooleanExtra("coursesChanged", false))
                refreshTimetable();

            // Show a setup snackbar if this was the first refresh
            if (intent.getBooleanExtra("first_update", false))
                Snackbar.make(findViewById(R.id.rootView), getString(R.string.setup_prompt), BaseTransientBottomBar.LENGTH_INDEFINITE)
                        .setAction(R.string.setup_action, v -> {
                            coursesSheet = new CoursesSheet();
                            coursesSheet.show(getSupportFragmentManager(), "coursesSheet");
                        })
                        .setGestureInsetBottomIgnored(false)
                        .show();

            // Update widget
            Intent widgetIntent = new Intent(MainActivity.this, WidgetProvider.class);
            widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(getApplication())
                    .getAppWidgetIds(new ComponentName(getApplication(), WidgetProvider.class));
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(widgetIntent);
        }
    };
    // Show the credentials sheet when ChangesManager encounters wrong credentials
    private BroadcastReceiver mInvalidateCredentialsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Show a bottom sheet to edit credentials
            CredentialsSheet bottomsheet = new CredentialsSheet();
            bottomsheet.show(getSupportFragmentManager(), "credentialsSheet");
        }
    };
    // Recreate Activity after design change
    private BroadcastReceiver mRecreateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            recreate();
        }
    };
    // Show offline snackbar on error
    private BroadcastReceiver mFailedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(findViewById(R.id.container), R.string.error_offline, Snackbar.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onResume() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        // Refresh if last refresh is more than 15 minutes ago and setup is complete
        if ((Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastRefresh", 0)) > 900 * 1000
                && !prefs.getString("pass", "").isEmpty()) {
            new ChangesManager().refreshChanges(getApplicationContext());
            swiperefresh.setRefreshing(true);
        }

        // In-App updates
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Request the update
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            UPDATE_REQUEST);

                    // Create a listener to track request state updates.
                    InstallStateUpdatedListener listener = state -> {
                        // Show module progress, log state, or install the update.

                        if (state.installStatus() == InstallStatus.DOWNLOADED) {
                            Snackbar completeSnackbar = Snackbar.make(findViewById(R.id.rootView), R.string.update_downloaded, BaseTransientBottomBar.LENGTH_INDEFINITE);
                            completeSnackbar.setAction(R.string.update_complete, view -> appUpdateManager.completeUpdate())
                                    .show();
                        }
                    };
                    appUpdateManager.registerListener(listener);
                } catch (IntentSender.SendIntentException mE) {
                    mE.printStackTrace();
                }
            }
        });

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        // Force dark option below Android 10
        // Needs to be set before activity is created
        if (Build.VERSION.SDK_INT < 29) {
            if (prefs.getBoolean("forceDark", true))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Post-process update
        if (prefs.getInt("lastVersion", 123) < 128) {
            prefs.edit().putInt("lastVersion", 128)
                    .putString("changes", "")
                    .putString("courses", "")
                    .putLong("lastCourseRefresh", 0)
                    .putLong("lastTimetableRefresh", 0).apply();
            if (!prefs.getString("name", "").isEmpty())
                new ChangesManager().refreshChanges(getApplicationContext());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Set up tabs
        /*SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        // Disable SwipeRefreshLayout while swiping
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float v, int i1) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (swiperefresh != null)
                    swiperefresh.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });*/


        FragmentManager fragmentManager = getSupportFragmentManager();
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // Switch to all changes if no courses are specified
        if (prefs.getString("myCourses", "").isEmpty())
            fragmentManager.beginTransaction().replace(R.id.container, new AllChangesFragment()).commit();
        else
            fragmentManager.beginTransaction().replace(R.id.container, new MyChangesFragment()).commit();

        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_mine:
                    fragmentManager.beginTransaction().replace(R.id.container, new MyChangesFragment()).commit();
                    return true;
                case R.id.action_all:
                    fragmentManager.beginTransaction().replace(R.id.container, new ChangesFragment()).commit();
                    return true;
                case R.id.action_options:
                    OptionsSheet optionsSheet = new OptionsSheet();
                    optionsSheet.show(getSupportFragmentManager(), "optionsSheet");
                    return true;
            }
            return false;
        });
        bottomNavigation.setOnNavigationItemReselectedListener(item -> {
        });

        // Register to receive messages.
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mMessageReceiver, new IntentFilter("changesRefreshed"));
        broadcastManager.registerReceiver(mRefreshingReceiver, new IntentFilter("refreshing"));
        broadcastManager.registerReceiver(mInvalidateCredentialsReceiver, new IntentFilter("invalidateCredentials"));
        broadcastManager.registerReceiver(mRecreateReceiver, new IntentFilter("recreate"));
        broadcastManager.registerReceiver(mFailedReceiver, new IntentFilter("refreshFailed"));

        // Set up swipe to refresh
        swiperefresh = findViewById(R.id.swiperefresh);
        swiperefresh.setOnRefreshListener(() -> new ChangesManager().refreshChanges(getApplicationContext()));

        // Ask for credentials if none have been entered yet
        if (prefs.getString("name", "").isEmpty()) {
            // Show a bottom sheet to add credentials
            CredentialsSheet bottomsheet = new CredentialsSheet();
            bottomsheet.show(getSupportFragmentManager(), "credentialsSheet");
        }


        if (prefs.getBoolean("backgroundRefresh", true)) {
            // Enqueue background workers

            Constraints workConstraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(RefreshWorker.class, 60, TimeUnit.MINUTES)
                    .setInitialDelay(45 - Calendar.getInstance().get(Calendar.MINUTE), TimeUnit.MINUTES)
                    .setConstraints(workConstraints)
                    .addTag("changesRefresh")
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("changesRefresh", ExistingPeriodicWorkPolicy.KEEP, workRequest);

            PeriodicWorkRequest morningWorkRequest = new PeriodicWorkRequest.Builder(RefreshWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(workConstraints)
                    .addTag("morningReinforcement")
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("morningReinforcement", ExistingPeriodicWorkPolicy.KEEP, morningWorkRequest);
        }

        createNotificationChannel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Ignore item, there's only one
        // Show a bottom sheet with information and options

        OptionsSheet optionsSheet = new OptionsSheet();
        optionsSheet.show(getSupportFragmentManager(), "optionsSheet");

        return super.onOptionsItemSelected(item);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("changes", getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);

            NotificationChannel firebasechannel = new NotificationChannel("firebase", getString(R.string.notification_channel_firebase_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.notification_channel_firebase_description));
            Objects.requireNonNull(notificationManager).createNotificationChannel(firebasechannel);
        }
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshingReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mInvalidateCredentialsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecreateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mFailedReceiver);
        super.onDestroy();
    }

    // Part of CoursesSheet
    // Remove courses from myCourses list
    public void deleteCourseItem(final View view) {
        View parent = (View) view.getParent();
        TextView textView = parent.findViewById(R.id.courseTextView);
        myCourses.remove(textView.getTag().toString());
        coursesSheet.adapter.notifyDataSetChanged();
        coursesSheet.refreshTimetable();
        // Vibrate
        (new Resolver()).vibrate(getApplicationContext());
    }

    // OnClick for changeItem
    public void showChangeActions(final View view) {
        if (view.getId() == R.id.cardView && view.getTag() == "edit") {
            selectorSheet = new CoursesTimetableSheet((int) view.getTag(R.id.day), (int) view.getTag(R.id.period));
            selectorSheet.show(getSupportFragmentManager(), "courseSelectorSheet");
        } else if (view.getId() == R.id.infoButton) {
            ActionsSheet actionsSheet = new ActionsSheet(view, (String) view.getTag());
            actionsSheet.show(getSupportFragmentManager(), "courseInfoSheet");
        } else {
            ActionsSheet actionsSheet = new ActionsSheet(view);
            actionsSheet.show(getSupportFragmentManager(), "actionsSheet");
        }
    }

    // OnClick for editing courses from the timetable
    public void toggleCourse(final View view) {
        String course = (String) view.getTag();
        if (myCourses.contains(course)) {
            myCourses.remove(course);
            ((ImageButton) view).setImageResource(R.drawable.ic_star_outline);
        } else {
            myCourses.add(course);
            ((ImageButton) view).setImageResource(R.drawable.ic_star);
            selectorSheet.dismiss();
        }
        coursesSheet.refreshTimetable();
        (new Resolver()).vibrate(getApplicationContext());
    }

    void refreshTimetable() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        Resolver resolver = new Resolver();

        Lesson[][][] allTable = gson.fromJson(prefs.getString("timetableAll", ""), Lesson[][][].class);
        Lesson[][][] myTable = new Lesson[5][][];
        ArrayList<Lesson[]> dayTable = new ArrayList<>();
        ArrayList<Lesson> periodTable = new ArrayList<>();

        if (allTable == null) {
            // Probably q34 - no data yet
            prefs.edit().putString("timetableMine", "").apply();
            LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent("changesRefreshed"));
            return;
        }

        for (int day = 0; day <= 4; day++) {
            for (int period = 0; period < allTable[day].length; period++) {
                for (Lesson lesson : allTable[day][period]) {
                    if (resolver.isFavorite(lesson.getCourse(), getApplicationContext())) {
                        periodTable.add(lesson);
                    }
                }
                dayTable.add(periodTable.toArray(new Lesson[0]));
                periodTable.clear();
            }
            myTable[day] = dayTable.toArray(new Lesson[0][]);
            dayTable.clear();
        }

        prefs.edit().putString("timetableMine", gson.toJson(myTable)).apply();

        Intent intent = new Intent("changesRefreshed");
        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
    }
}