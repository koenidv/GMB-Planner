package com.koenidv.gmbplanner;

import android.app.ActivityManager;
import android.content.Context;

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
        if (shouldRun()) {
            (new ChangesManager()).refreshChanges(getApplicationContext(), backgrounded());
            return Result.success();
        } else {
            return Result.retry();
        }
    }

    /**
     * Determines whether the WorkRequest should run
     *
     * @return true if the network request should be sent
     */
    private boolean shouldRun() {
        Calendar time = Calendar.getInstance();

        if (getTags().contains("changesRefreshWhenOnline")) {
            // After failed request
            return true;
        } else if (getTags().contains("morningReinforcement")) {
            // Second worker for mornings
            // Should only run between 6 and 7:59 on weekdays
            if (time.get(Calendar.DAY_OF_WEEK) <= 5) {
                return time.get(Calendar.HOUR_OF_DAY) >= 6 && time.get(Calendar.HOUR_OF_DAY) < 8;
            }
        } else {
            // Default worker
            // Should only run in the backgound between 5 and 22 on weekdays and 20 and 22 on weekends
            if (backgrounded()) {
                if (time.get(Calendar.DAY_OF_WEEK) <= 5) {
                    return time.get(Calendar.HOUR_OF_DAY) >= 5 && time.get(Calendar.HOUR_OF_DAY) < 22;
                } else {
                    return time.get(Calendar.HOUR_OF_DAY) >= 20 && time.get(Calendar.HOUR_OF_DAY) < 22;
                }
            }
        }


        return false;
    }

    /**
     * Determines if the app is running in the background
     *
     * @return false if the application is running in the foreground, true if not
     */
    private boolean backgrounded() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance != IMPORTANCE_FOREGROUND && appProcessInfo.importance != IMPORTANCE_VISIBLE);
    }

}
