package com.koenidv.gmbplanner;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import static android.content.Context.MODE_PRIVATE;
import static com.koenidv.gmbplanner.MainActivity.coursesSheet;

//  Created by koenidv on 16.02.2020.
public class OptionsSheet extends BottomSheetDialogFragment {

    OptionsSheet() {
    }

    private String appnameTitle;
    private String greetingTitle;
    private String refreshedShort;
    private String refreshedInfo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sheet_options, container, false);

        final SharedPreferences prefs = getContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        DateFormat timeFormatter = new SimpleDateFormat(getString(R.string.dateformat_hours), Locale.GERMAN);
        DateFormat dateFormatter = new SimpleDateFormat(getString(R.string.dateformat_coursesrefreshed), Locale.GERMAN);

        // Append version to app name
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            String version = pInfo.versionName;
            appnameTitle = getString(R.string.info_app);
            appnameTitle = appnameTitle.replace("%version", version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        try {
            String firstname = prefs.getString("realname", "");
            firstname = firstname.substring(0, firstname.indexOf(" "));
            greetingTitle = getString(R.string.info_greeting).replace("%name", firstname);
        } catch (StringIndexOutOfBoundsException e) {
            greetingTitle = getString(R.string.app_name);
        }

        refreshedInfo = getString(R.string.last_refreshed)
                .replace("%refresh", prefs.getLong("lastRefresh", 0) == 0 ? "..." : timeFormatter.format(prefs.getLong("lastRefresh", 0)))
                .replace("%change", prefs.getString("lastChange", "..."))
                .replace("%courses", prefs.getLong("lastCourseRefresh", 0) == 0 ? "..." : dateFormatter.format(prefs.getLong("lastCourseRefresh", 0)));
        if (Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastRefresh", 0) < 900000)
            refreshedShort = getString(R.string.last_refreshed_shortly);
        else if (Calendar.getInstance().getTimeInMillis() - prefs.getLong("lastRefresh", 0) < 3600000)
            refreshedShort = getString(R.string.last_refreshed_hourly);
        else refreshedShort = getString(R.string.last_refreshed_other);


        final TextView titleTextView = view.findViewById(R.id.titleTextView);
        final TextView authorTextView = view.findViewById(R.id.authorTextView);
        final TextView refreshTextView = view.findViewById(R.id.lastRefreshedTextView);

        titleTextView.setText(greetingTitle);
        authorTextView.setVisibility(View.GONE);
        refreshTextView.setText(refreshedShort);

        authorTextView.setOnClickListener(v -> {
            // Link to my instagram
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://instagram.com/halbunsichtbar"));
            startActivity(i);
        });

        // Expand button to show more info
        view.findViewById(R.id.expandButton).setOnClickListener(v -> {
            LinearLayout expandLayout = view.findViewById(R.id.expandLayout);
            ImageButton expandButton = view.findViewById(R.id.expandButton);
            if (expandLayout.getVisibility() == View.GONE) {
                expandLayout.setVisibility(View.VISIBLE);
                titleTextView.setText(appnameTitle);
                authorTextView.setVisibility(View.VISIBLE);
                refreshTextView.setText(refreshedInfo);
                refreshTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.ic_refresh), null);
                expandButton.setImageResource(R.drawable.ic_less);
            } else {
                expandLayout.setVisibility(View.GONE);
                titleTextView.setText(greetingTitle);
                authorTextView.setVisibility(View.GONE);
                refreshTextView.setText(refreshedShort);
                refreshTextView.setCompoundDrawablesRelative(null, null, null, null);
                expandButton.setImageResource(R.drawable.ic_more);
            }
        });

        view.findViewById(R.id.githubButton).setOnClickListener(v -> {
            // Link to this app's GitHub repository
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://github.com/koenidv/gmb-planner"));
            startActivity(i);
        });

        refreshTextView.setOnClickListener(v -> {
            // Force refresh all changes, courses and lessons
            prefs.edit()
                    .putLong("lastCourseRefresh", 0)
                    .putLong("lastTimetableRefresh", 0)
                    /*.putString("courses", "") Do not delete courses by default; would remove added grades */
                    .apply();
            new ChangesManager().refreshChanges(getContext());
            dismiss();
        });

        view.findViewById(R.id.coursesButton).setOnClickListener(v -> {
            // Show a bottom sheet to edit favorite courses
            coursesSheet = new CoursesSheet();
            coursesSheet.show(getActivity().getSupportFragmentManager(), "coursesSheet");
            dismiss();
        });

        view.findViewById(R.id.credentialsButton).setOnClickListener(v -> {
            // Show a bottom sheet to edit credentials
            CredentialsSheet bottomsheet = new CredentialsSheet();
            bottomsheet.show(getActivity().getSupportFragmentManager(), "credentialsSheet");
            dismiss();
        });

        // Dark mode toggle group
        if (Build.VERSION.SDK_INT < 29) {
            // Display force dark switch if below Android 10
            MaterialButtonToggleGroup darkToggleGroup = view.findViewById(R.id.darkmodeToggleGroup);
            darkToggleGroup.setVisibility(View.VISIBLE);

            if (prefs.getBoolean("forceDark", true))
                darkToggleGroup.check(R.id.darkButton);
            else
                darkToggleGroup.check(R.id.lightButton);

            darkToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    prefs.edit().putBoolean("forceDark", checkedId == R.id.darkButton).apply();
                    dismiss();
                    // Send broadcast to recreate
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("recreate"));
                }
            });
        }

        // Compact mode toggle group
        MaterialButtonToggleGroup compactToggleGroup = view.findViewById(R.id.compactToggleGroup);
        if (prefs.getBoolean("compactModeFavorite", false) && prefs.getBoolean("compactModeAll", true))
            compactToggleGroup.check(R.id.compactButton);
        else if (!prefs.getBoolean("compactModeFavorite", false) && prefs.getBoolean("compactModeAll", true))
            compactToggleGroup.check(R.id.autoCompactButton);
        else
            compactToggleGroup.check(R.id.comfortableButton);

        compactToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.compactButton) {
                    prefs.edit().putBoolean("compactModeFavorite", true)
                            .putBoolean("compactModeAll", true)
                            .putBoolean("colorless", true)
                            .apply();
                } else if (checkedId == R.id.autoCompactButton) {
                    prefs.edit().putBoolean("compactModeFavorite", false)
                            .putBoolean("compactModeAll", true)
                            .putBoolean("colorless", false)
                            .apply();
                } else if (checkedId == R.id.comfortableButton) {
                    prefs.edit().putBoolean("compactModeFavorite", false)
                            .putBoolean("compactModeAll", false)
                            .putBoolean("colorless", false)
                            .apply();
                }
                // Broadcast to refresh UI
                Intent doneIntent = new Intent("changesRefreshed");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(doneIntent);
                dismiss();
            }
        });

        // Background update toggle group
        MaterialButtonToggleGroup backgroundToggleGroup = view.findViewById(R.id.backgroundToggleGroup);
        if (prefs.getBoolean("backgroundRefresh", true))
            backgroundToggleGroup.check(R.id.backgroundOnButton);
        else
            backgroundToggleGroup.check(R.id.backgroundOffButton);

        backgroundToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.backgroundOnButton) {
                    prefs.edit().putBoolean("backgroundRefresh", true).apply();

                    // Enqueue background workers
                    Constraints workConstraints = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();
                    PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(RefreshWorker.class, 60, TimeUnit.MINUTES)
                            .setInitialDelay(45 - Calendar.getInstance().get(Calendar.MINUTE), TimeUnit.MINUTES)
                            .setConstraints(workConstraints)
                            .addTag("changesRefresh")
                            .build();
                    WorkManager.getInstance(getContext()).enqueueUniquePeriodicWork("changesRefresh", ExistingPeriodicWorkPolicy.KEEP, workRequest);

                    PeriodicWorkRequest morningWorkRequest = new PeriodicWorkRequest.Builder(RefreshWorker.class, 15, TimeUnit.MINUTES)
                            .setConstraints(workConstraints)
                            .addTag("morningReinforcement")
                            .build();
                    WorkManager.getInstance(getContext()).enqueueUniquePeriodicWork("morningReinforcement", ExistingPeriodicWorkPolicy.KEEP, morningWorkRequest);
                } else {
                    prefs.edit().putBoolean("backgroundRefresh", false).apply();

                    // Cancel background workers
                    WorkManager.getInstance(getContext()).cancelUniqueWork("changesRefresh");
                    WorkManager.getInstance(getContext()).cancelUniqueWork("morningReinforcement");
                }

                dismiss();
            }
        });

        view.findViewById(R.id.manageAccountButton).setOnClickListener(v -> {
            // Open the website to edit the user account.
            // Can't automatically log the user in as that would require a post request.
            Uri uri = Uri.parse("https://mosbacher-berg.de/user/login");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(browserIntent);
            dismiss();
        });

        /*Objects.requireNonNull(view.findViewById(R.id.manageAccountButton)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // For debugging
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RefreshWorker.class)
                        .addTag("changesRefreshWhenOnline")
                        .setInitialDelay(5, TimeUnit.SECONDS)
                        .build();
                WorkManager.getInstance(getContext()).enqueueUniqueWork("changesRefreshWhenOnline", ExistingWorkPolicy.KEEP, workRequest);
                return true;
            }
        });*/

        view.findViewById(R.id.feedbackButton).setOnClickListener(v -> {
            // Get app version
            String version;
            try {
                PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
                version = String.valueOf(pInfo.versionCode);
            } catch (PackageManager.NameNotFoundException nme) {
                version = "?";
            }
            // Send an email
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:"))
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{"sv@gmbwi.de"})
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                    .putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body).replace("%app", version).replace("%android", String.valueOf(Build.VERSION.SDK_INT)));
            // Only open if email client is installed
            if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null)
                startActivity(emailIntent);
            dismiss();
        });

        view.findViewById(R.id.feedbackButton).setOnLongClickListener(v -> {
            prefs.edit().putBoolean("testing_grades", !prefs.getBoolean("testing_grades", false)).apply();
            dismiss();
            return true;
        });

        view.findViewById(R.id.rateButton).setOnClickListener(v -> {
            // Open Play Store to let the user rate the app
            final String appPackageName = getActivity().getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
            dismiss();
        });

        view.findViewById(R.id.rateButton).setOnLongClickListener(v -> {
            prefs.edit().putBoolean("sveaEE", !prefs.getBoolean("sveaEE", false)).apply();
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("changesRefreshed"));
            return true;
        });

        view.findViewById(R.id.doneButton).setOnClickListener(v -> {
            // Dismiss the sheet
            dismiss();
        });


        return view;
    }
}
