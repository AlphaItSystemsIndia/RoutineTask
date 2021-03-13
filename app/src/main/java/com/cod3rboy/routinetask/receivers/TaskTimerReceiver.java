package com.cod3rboy.routinetask.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.services.TaskTimerService;

public class TaskTimerReceiver extends BroadcastReceiver {
    private final String LOG_TAG = TaskTimerReceiver.class.getSimpleName();
    public static final String KEY_EXTRA_ACTION = "action_key";
    public static final int ACTION_COMPLETE_TASK = 1;
    public static final int ACTION_SERVICE_STOP = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(LOG_TAG, "onReceive called");
        int extraAction = intent.getIntExtra(KEY_EXTRA_ACTION, -1);
        if (extraAction < 0) return;
        TaskTimerService service = TaskTimerService.getService();
        switch (extraAction) {
            case ACTION_COMPLETE_TASK:
                Logger.d(LOG_TAG, "Action :- ACTION_COMPLETE_TASK");
                // Suspend Service
                if(service == null) return;
                service.stopService();
                Intent statsCollectIntent = new Intent(service, StatisticsReceiver.class);
                statsCollectIntent.putExtra(StatisticsReceiver.KEY_TASK_PARCEL, service.getTask());
                statsCollectIntent.putExtra(StatisticsReceiver.KEY_STATISTICS_TYPE, StatisticsReceiver.STATISTICS_TYPE_TASK);
                // Set incomplete task status since task is not yet completed when notification is displayed
                statsCollectIntent.putExtra(StatisticsReceiver.KEY_TASK_STATUS, StatisticsReceiver.TASK_STATUS_NOT_COMPLETE);
                context.sendBroadcast(statsCollectIntent);
                break;
            case ACTION_SERVICE_STOP:
                Logger.d(LOG_TAG, "Action :- ACTION_SERVICE_STOP");
                // Suspend Service
                if(service == null) return;
                service.stopService();
        }
    }
}
