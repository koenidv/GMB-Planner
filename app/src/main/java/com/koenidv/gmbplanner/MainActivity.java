package com.koenidv.gmbplanner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.koenidv.gmbplanner.ui.main.SectionsPagerAdapter;
import com.koenidv.gmbplanner.widget.WidgetProvider;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {

    public static CoursesSheet coursesSheet;
    private SwipeRefreshLayout swiperefresh;
    boolean ignoreFirstRefreshed = false;

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

    @Override
    protected void onResume() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        // Refresh if last refresh is more than 15 minutes ago and setup is complete
        if ((Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastRefresh", 0)) > 900 * 1000
                && !prefs.getString("pass", "").isEmpty()) {
            new ChangesManager().refreshChanges(getApplicationContext());
            swiperefresh.setRefreshing(true);
        }
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
        if (prefs.getInt("lastVersion", 123) < 126) {
            prefs.edit().putInt("lastVersion", 126).putLong("lastCourseRefresh", 0).apply();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up tabs
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
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
        });

        // Register to receive messages.
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mMessageReceiver, new IntentFilter("changesRefreshed"));
        broadcastManager.registerReceiver(mRefreshingReceiver, new IntentFilter("refreshing"));
        broadcastManager.registerReceiver(mInvalidateCredentialsReceiver, new IntentFilter("invalidateCredentials"));
        broadcastManager.registerReceiver(mRecreateReceiver, new IntentFilter("recreate"));

        // Set up swipe to refresh
        swiperefresh = findViewById(R.id.swiperefresh);
        swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ChangesManager().refreshChanges(getApplicationContext());
            }
        });

        // Switch to all changes if no courses are specified
        if (prefs.getString("myCourses", "").isEmpty()) {
            viewPager.setCurrentItem(1);
        }
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
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshingReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mInvalidateCredentialsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecreateReceiver);
        super.onDestroy();
    }

    // Part of CoursesSheet
    // Remove courses from myCourses list
    public void deleteCourseItem(final View view) {
        View parent = (View) view.getParent();
        TextView textView = parent.findViewById(R.id.courseTextView);
        coursesSheet.myCourses.remove(textView.getText().toString());
        coursesSheet.adapter.notifyDataSetChanged();
    }

    // OnClick for changeItem
    public void showChangeActions(final View view) {
        ActionsSheet actionsSheet = new ActionsSheet(view);
        actionsSheet.show(getSupportFragmentManager(), "actionsSheet");
    }
}