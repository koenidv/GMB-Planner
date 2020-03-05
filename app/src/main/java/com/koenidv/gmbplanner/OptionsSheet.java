package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
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
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
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
        final Resolver resolver = new Resolver();
        final Gson gson = new Gson();

        Objects.requireNonNull(view.findViewById(R.id.authorTextView)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Link to my instagram
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://instagram.com/halbunsichtbar"));
                startActivity(i);
            }
        });

        TextView refreshTextView = view.findViewById(R.id.lastRefreshedTextView);
        assert refreshTextView != null;
        refreshTextView.setText(getString(R.string.last_refreshed)
                .replace("%refresh", prefs.getLong("lastRefresh", 0) == 0 ? "..." : timeFormatter.format(prefs.getLong("lastRefresh", 0)))
                .replace("%change", prefs.getString("lastChange", "..."))
                .replace("%courses", prefs.getLong("lastCourseRefresh", 0) == 0 ? "..." : dateFormatter.format(prefs.getLong("lastCourseRefresh", 0))));
        refreshTextView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View v) {
                // Force refresh all changes, courses and lessons
                prefs.edit().putLong("lastCourseRefresh", 0).putLong("lastTimetableRefresh", 0).commit();
                new ChangesManager().refreshChanges(getContext());
                dismiss();
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.coursesButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a bottom sheet to edit favorite courses
                coursesSheet = new CoursesSheet();
                coursesSheet.show(getActivity().getSupportFragmentManager(), "coursesSheet");
                dismiss();
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.credentialsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a bottom sheet to edit credentials
                CredentialsSheet bottomsheet = new CredentialsSheet();
                bottomsheet.show(getActivity().getSupportFragmentManager(), "credentialsSheet");
                dismiss();
            }
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

            darkToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                @Override
                public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                    if (isChecked) {
                        prefs.edit().putBoolean("forceDark", checkedId == R.id.darkButton).apply();
                        dismiss();
                        // Send broadcast to recreate
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("recreate"));
                    }
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

        compactToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.compactButton) {
                        prefs.edit().putBoolean("compactModeFavorite", true)
                                .putBoolean("compactModeAll", true).apply();
                    } else if (checkedId == R.id.autoCompactButton) {
                        prefs.edit().putBoolean("compactModeFavorite", false)
                                .putBoolean("compactModeAll", true).apply();
                    } else if (checkedId == R.id.comfortableButton) {
                        prefs.edit().putBoolean("compactModeFavorite", false)
                                .putBoolean("compactModeAll", false).apply();
                    }
                    // Broadcast to refresh UI
                    Intent doneIntent = new Intent("changesRefreshed");
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(doneIntent);
                    dismiss();
                }
            }
        });

        // Background update toggle group
        MaterialButtonToggleGroup backgroundToggleGroup = view.findViewById(R.id.backgroundToggleGroup);
        if (prefs.getBoolean("backgroundRefresh", true))
            backgroundToggleGroup.check(R.id.backgroundOnButton);
        else
            backgroundToggleGroup.check(R.id.backgroundOffButton);

        backgroundToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
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
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.manageAccountButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the website to edit the user account.
                // Can't automatically log the user in as that would require a post request.
                Uri uri = Uri.parse("https://mosbacher-berg.de/user/login");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(browserIntent);
                dismiss();
            }
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

        Objects.requireNonNull(view.findViewById(R.id.feedbackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get app version
                String version;
                try {
                    PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
                    version = String.valueOf(pInfo.versionCode);
                } catch (PackageManager.NameNotFoundException nme) {
                    version = "?";
                }
                // Send an email
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{"sv@gmbwi.de"})
                        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                        .putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body).replace("%app", version).replace("%android", String.valueOf(Build.VERSION.SDK_INT)));
                // Only open if email client is installed
                if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null)
                    startActivity(emailIntent);
                dismiss();
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.feedbackButton)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                prefs.edit().putBoolean("colorless", !prefs.getBoolean("colorless", false)).apply();
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("changesRefreshed"));
                return true;
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.rateButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Play Store to let the user rate the app
                final String appPackageName = getActivity().getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                dismiss();
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.rateButton)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                prefs.edit().putBoolean("sveaEE", !prefs.getBoolean("sveaEE", false)).apply();
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("changesRefreshed"));
                return true;
            }
        });

        Objects.requireNonNull(view.findViewById(R.id.doneButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the sheet
                dismiss();
            }
        });


        return view;
    }
}
