package com.cod3rboy.routinetask.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.services.NotificationAlarmService;

public class NotificationDismissReceiver extends BroadcastReceiver {
    public static final String LOG_TAG = NotificationDismissReceiver.class.getSimpleName();
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(LOG_TAG, "onReceive() called");
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        if (notificationId < 0) return;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        // Stop running notification alarm
        if(NotificationAlarmService.getSelf() != null) NotificationAlarmService.getSelf().stop();
    }
}
