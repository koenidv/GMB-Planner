package com.koenidv.gmbplanner;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Objects;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//  Created by koenidv on 16.02.2020.
public class CredentialsSheet extends BottomSheetDialogFragment {

    private BottomSheetDialog loaderSheet;
    private boolean setup_changes, setup_courses, setup_timetable;

    // Receive broadcasts to hide loaderSheet when everything is loaded
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setup_changes = setup_changes || intent.getBooleanExtra("setup_changes", false);
            setup_courses = setup_courses || intent.getBooleanExtra("setup_courses", false);
            setup_timetable = setup_timetable || intent.getBooleanExtra("setup_timetable", false);

            if (setup_changes && setup_courses && setup_timetable) {
                loaderSheet.dismiss();
            }
        }
    };

    public CredentialsSheet() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_credentials, container, false);

        final SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();

        // Create loader sheet with a random message
        loaderSheet = new BottomSheetDialog(getActivity());
        loaderSheet.setContentView(R.layout.sheet_loader);
        loaderSheet.setCancelable(false);
        String[] loadingMessages = getResources().getStringArray(R.array.loading_facts);
        ((TextView) Objects.requireNonNull(loaderSheet.findViewById(R.id.loadingTextView))).setText(loadingMessages[(new Random()).nextInt(loadingMessages.length)]);

        // Register to receive messages.
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.registerReceiver(mMessageReceiver, new IntentFilter("changesRefreshed"));

        final EditText nameEditText = view.findViewById(R.id.nameEditText),
                passEditText = view.findViewById(R.id.passEditText);
        final Button saveButton = view.findViewById(R.id.saveButton),
                cancelButton = view.findViewById(R.id.cancelButton);
        final TextView explanationTextView = view.findViewById(R.id.explanationTextView);

        // Setup
        if (prefs.getString("name", "").isEmpty()) {
            // First setup
            setCancelable(false);
            cancelButton.setVisibility(View.GONE);
            explanationTextView.setText((new Resolver()).fromHtml(getString(R.string.start_credentials_explanation)));
            explanationTextView.setMovementMethod(LinkMovementMethod.getInstance());
            explanationTextView.setVisibility(View.VISIBLE);
        } else if (prefs.getString("pass", "").isEmpty()) {
            // Credentials invalidated
            setCancelable(false);
            cancelButton.setVisibility(View.GONE);
            explanationTextView.setText(R.string.credentials_invalidated);
            explanationTextView.setVisibility(View.VISIBLE);
        }

        // Show entered username, if any, but not the password
        nameEditText.setText(prefs.getString("name", ""));
        nameEditText.requestFocus();

        // Enable save button only when text fields are populated
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (nameEditText.getText().toString().isEmpty() || passEditText.getText().toString().isEmpty())
                    saveButton.setEnabled(false);
                else saveButton.setEnabled(true);
            }
        };

        nameEditText.addTextChangedListener(textWatcher);
        passEditText.addTextChangedListener(textWatcher);

        view.findViewById(R.id.cancelButton).setOnClickListener(v -> {
            // Disable password manager on cancel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nameEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
                passEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            }
            dismiss();
        });

        saveButton.setOnClickListener(v -> {
            // Save the entered values to SharedPrefs and force refresh
            dismiss();
            prefsEdit.putString("name", nameEditText.getText().toString())
                    .putString("pass", passEditText.getText().toString())
                    .putLong("lastCourseRefresh", 0)
                    .putLong("lastTimetableRefresh", 0)
                    .apply();
            new ChangesManager().refreshChanges(Objects.requireNonNull(getContext()));
            // Broadcast to show that contents are refreshing
            Intent intent = new Intent("refreshing");
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

            // Show loader sheet
            loaderSheet.show();
        });

        return view;
    }
}
