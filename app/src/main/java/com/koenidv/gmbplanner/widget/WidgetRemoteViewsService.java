package com.koenidv.gmbplanner.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

//  Created by koenidv on 24.02.2020.
public class WidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}