package com.koenidv.gmbplanner;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

//  Created by koenidv on 22.02.2020.
public class RefreshWorker extends Worker {

    public RefreshWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NotNull
    @Override
    public Result doWork() {

        if (!foregrounded() && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 5) {
            Log.d("GMB Planner", "Refreshing in background");
            (new ChangesManager()).refreshChanges(getApplicationContext(), true);
        }

        return Result.success();
    }

    private boolean foregrounded() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }

}
