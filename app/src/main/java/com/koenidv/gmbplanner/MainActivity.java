package com.koenidv.gmbplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.koenidv.gmbplanner.ui.main.SectionsPagerAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity {

    CoursesSheet coursesSheet;
    private SwipeRefreshLayout swiperefresh;
    // Show that changes are refreshing when the broadcast "refreshing" is received
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            swiperefresh.setRefreshing(true);
        }
    };
    // Show that refreshing is done when the broadcast "changesRefreshed" is received
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            swiperefresh.setRefreshing(false);
        }
    };
    // Show the credentials sheet when the ChangesManager encounters wrong credentials
    private BroadcastReceiver mInvalidateCredentialsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Show a bottom sheet to edit credentials
            CredentialsSheet bottomsheet = new CredentialsSheet();
            bottomsheet.show(getSupportFragmentManager(), "credentialsSheet");
        }
    };

    @Override
    protected void onResume() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        // Refresh if last refresh is more than 30 minutes ago and setup is complete
        if ((Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastRefresh", 0)) > 1800 * 1000
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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        // Register to receive messages.
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mMessageReceiver, new IntentFilter("changesRefreshed"));
        broadcastManager.registerReceiver(mRefreshingReceiver, new IntentFilter("refreshing"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mInvalidateCredentialsReceiver, new IntentFilter("invalidateCredentials"));

        swiperefresh = findViewById(R.id.swiperefresh);
        swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ChangesManager().refreshChanges(getApplicationContext());
            }
        });

        if (prefs.getString("myCourses", "").isEmpty()) {
            viewPager.setCurrentItem(1);
        }
        if (prefs.getString("name", "").isEmpty()) {
            // Show a bottom sheet to add credentials
            CredentialsSheet bottomsheet = new CredentialsSheet();
            bottomsheet.show(getSupportFragmentManager(), "credentialsSheet");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (Build.VERSION.SDK_INT < 29) {
            // Display force dark toggle if below Android 10
            final SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            MenuItem darkModeItem = menu.findItem(R.id.darkmodeItem);
            darkModeItem.setVisible(true);
            if (prefs.getBoolean("forceDark", true))
                darkModeItem.setTitle(R.string.menu_darkmode_disable);
            darkModeItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                // Invert forceDark and recreate activity
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    prefs.edit().putBoolean("forceDark", !prefs.getBoolean("forceDark", true)).apply();
                    recreate();
                    return false;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.refreshItem:
                // Refresh all changes
                swiperefresh.setRefreshing(true);
                new ChangesManager().refreshChanges(getApplicationContext());
                break;
            case R.id.changeAuthorizationItem:
                // Show a bottom sheet to edit credentials
                CredentialsSheet bottomsheet = new CredentialsSheet();
                bottomsheet.show(getSupportFragmentManager(), "credentialsSheet");
                break;
            case R.id.changeCoursesItem:
                // Show a bottom sheet to edit favorite courses
                coursesSheet = new CoursesSheet();
                coursesSheet.show(getSupportFragmentManager(), "coursesSheet");
                break;
            case R.id.informationItem:
                // Show a bottom sheet describing the time of the last refresh and some more actions
                SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                DateFormat dateFormatter = new SimpleDateFormat(getString(R.string.dateformat_hours), Locale.GERMAN);

                final BottomSheetDialog infoSheet = new BottomSheetDialog(this, R.style.AppTheme_Sheet);
                infoSheet.setContentView(R.layout.sheet_information);

                Objects.requireNonNull(infoSheet.findViewById(R.id.authorTextView)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Link to my instagram
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://instagram.com/halbunsichtbar"));
                        startActivity(i);
                    }
                });

                TextView refreshTextView = infoSheet.findViewById(R.id.lastRefreshedTextView);
                assert refreshTextView != null;
                refreshTextView.setText(getString(R.string.last_refreshed)
                        .replace("%refresh", dateFormatter.format(prefs.getLong("lastRefresh", 0)))
                        .replace("%change", prefs.getString("lastChange", "?")));
                refreshTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Refresh all changes
                        swiperefresh.setRefreshing(true);
                        new ChangesManager().refreshChanges(getApplicationContext());
                        infoSheet.dismiss();
                    }
                });

                Objects.requireNonNull(infoSheet.findViewById(R.id.manageAccountButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Open the website to edit the user account.
                        // Can't automatically log the user in as that would require a post request.
                        Uri uri = Uri.parse("https://mosbacher-berg.de/user/login");
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(browserIntent);
                        infoSheet.dismiss();
                    }
                });

                Objects.requireNonNull(infoSheet.findViewById(R.id.feedbackButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Send an email
                        Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO)
                                .setData(Uri.parse("mailto:"))
                                .putExtra(Intent.EXTRA_EMAIL, new String[]{"sv@gmbwi.de"})
                                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                        // Only open if email client is installed
                        if (emailIntent.resolveActivity(getPackageManager()) != null)
                            startActivity(emailIntent);
                        else
                            Snackbar.make(findViewById(R.id.rootview), R.string.error_nomailclient, Snackbar.LENGTH_SHORT);
                        infoSheet.dismiss();
                    }
                });

                Objects.requireNonNull(infoSheet.findViewById(R.id.doneButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Dismiss the sheet
                        infoSheet.dismiss();
                    }
                });

                infoSheet.show();
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshingReceiver);
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
}