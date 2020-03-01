package com.koenidv.gmbplanner.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.koenidv.gmbplanner.ChangesManager;
import com.koenidv.gmbplanner.R;

//  Created by koenidv on 24.02.2020.
//  Just don't ask
public class WidgetProvider extends AppWidgetProvider {

    static int randomNumber;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            // Create an Intent to refresh
            Intent refreshIntent = new Intent(context, WidgetProvider.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Get the layout for the App Widget
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.rootView, pendingIntent);

            randomNumber = (int) (Math.random() * 100);
            Intent serviceIntent = new Intent(context, WidgetRemoteViewsService.class);
            serviceIntent.setData(Uri.fromParts("content", String.valueOf(appWidgetId + randomNumber), null));
            views.setRemoteAdapter(R.id.listView, serviceIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        new ChangesManager().refreshChanges(context);
        super.onEnabled(context);
    }
}