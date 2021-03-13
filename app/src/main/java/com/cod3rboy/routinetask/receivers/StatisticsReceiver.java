package com.cod3rboy.routinetask.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.TaskStatsManager;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.services.NotificationAlarmService;

public class StatisticsReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = StatisticsReceiver.class.getSimpleName();
    public static final String KEY_STATISTICS_TYPE = "statistics_type";
    public static final String KEY_TASK_PARCEL = "task_parcel";
    public static final String KEY_TASK_STATUS = "task_status";
    public static final String KEY_NOTIFICATION_ID = "notification_id";

    public static final int STATISTICS_TYPE_TASK = 400;
    public static final int STATISTICS_TYPE_POMODORO = 401;
    public static final int TASK_STATUS_COMPLETE = 10;
    public static final int TASK_STATUS_NOT_COMPLETE = 11;

    @Override
    public void onReceive(Context context, Intent intent) {
        int stat_type = intent.getIntExtra(KEY_STATISTICS_TYPE, -1);
        if (stat_type == STATISTICS_TYPE_TASK) {
            TaskModel task = intent.getParcelableExtra(KEY_TASK_PARCEL);
            if (task == null) return;
            int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, -1);
            if (notificationId != -1) {
                NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mgr.cancel(notificationId);
                // Stop running notification alarm
                if(NotificationAlarmService.getSelf() != null) NotificationAlarmService.getSelf().stop();
            }
            int task_status = intent.getIntExtra(KEY_TASK_STATUS, -1);
            if (task_status == TASK_STATUS_NOT_COMPLETE) { // Set uncompleted task as completed
                Logger.d(LOG_TAG, "Received intent to mark task with ID-" + task.getId() + " as completed");
                TaskStatsManager.getInstance().setTaskDone(task, () -> {
                    if (Utilities.canPlayTaskCompleteSound(context)) {
                        MediaPlayer player = MediaPlayer.create(context, R.raw.sound_task_complete);
                        player.start();
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                            }
                        });
                    }
                });

            } else if (task_status == TASK_STATUS_COMPLETE) { // Set completed task as uncompleted
                Logger.d(LOG_TAG, "Received intent to mark task with ID-" + task.getId() + " as uncompleted");
                TaskStatsManager.getInstance().setTaskUndone(task, null);
            }
        }
    }
}
