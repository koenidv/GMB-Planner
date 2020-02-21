package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
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
import android.widget.CompoundButton;
import android.widget.Switch;
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

        // Set up tabs
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Ignore item, there's only one

        // Show a bottom sheet describing the time of the last refresh and some more actions
        final SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        DateFormat timeFormatter = new SimpleDateFormat(getString(R.string.dateformat_hours), Locale.GERMAN);
        DateFormat dateFormatter = new SimpleDateFormat(getString(R.string.dateformat_coursesrefreshed), Locale.GERMAN);

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
        Long test = prefs.getLong("lastCourseRefresh", 5);
        refreshTextView.setText(getString(R.string.last_refreshed)
                .replace("%refresh", timeFormatter.format(prefs.getLong("lastRefresh", 0)))
                .replace("%change", prefs.getString("lastChange", "?"))
                .replace("%courses", dateFormatter.format(prefs.getLong("lastCourseRefresh", 0))));
        refreshTextView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View v) {
                // Refresh all changes
                swiperefresh.setRefreshing(true);
                prefs.edit().putLong("lastCourseRefresh", 0).commit();
                new ChangesManager().refreshChanges(getApplicationContext());
                infoSheet.dismiss();
            }
        });

        Objects.requireNonNull(infoSheet.findViewById(R.id.coursesButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a bottom sheet to edit favorite courses
                coursesSheet = new CoursesSheet();
                coursesSheet.show(getSupportFragmentManager(), "coursesSheet");
                infoSheet.dismiss();
            }
        });

        Objects.requireNonNull(infoSheet.findViewById(R.id.credentialsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a bottom sheet to edit credentials
                CredentialsSheet bottomsheet = new CredentialsSheet();
                bottomsheet.show(getSupportFragmentManager(), "credentialsSheet");
                infoSheet.dismiss();
            }
        });

        if (Build.VERSION.SDK_INT < 29) {
            // Display force dark switch if below Android 10
            Switch darkSwitch = infoSheet.findViewById(R.id.darkmodeSwitch);
            assert darkSwitch != null;
            darkSwitch.setVisibility(View.VISIBLE);
            if (prefs.getBoolean("forceDark", true))
                darkSwitch.setChecked(true);
            darkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Toggle force dark and recreate the activity to apply changes
                    prefs.edit().putBoolean("forceDark", !prefs.getBoolean("forceDark", true)).apply();
                    recreate();
                }
            });
        }

        Switch backgroundSwitch = infoSheet.findViewById(R.id.backgroundUpdateSwitch);
        assert backgroundSwitch != null;
        backgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Todo: Toggle background updates
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

        Objects.requireNonNull(infoSheet.findViewById(R.id.rateButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Play Store to let the user rate the app
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
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