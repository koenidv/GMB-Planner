package com.koenidv.gmbplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.koenidv.gmbplanner.ui.main.SectionsPagerAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);

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

        //new ChangesManager().refreshChanges(getApplicationContext());

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
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
            case R.id.refreshItem:
                swiperefresh.setRefreshing(true);
                new ChangesManager().refreshChanges(getApplicationContext());
                break;
            case R.id.manageAccountItem:
                // Open the website to edit the user account.
                // Can't automatically log the user in as that would require a post request.
                Uri uri = Uri.parse("https://mosbacher-berg.de/user/login");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(browserIntent);
                break;
            case R.id.feedbackItem:
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