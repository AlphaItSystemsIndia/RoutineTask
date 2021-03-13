package com.cod3rboy.routinetask.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.services.NotificationAlarmService;
import com.cod3rboy.routinetask.services.TaskTimerService;
import com.cod3rboy.routinetask.utilities.AlarmScheduler;

import java.util.Random;

import needle.Needle;

/**
 * Broadcast Receiver to show task notification when an alarm set for a task goes off.
 */
public class TaskBroadcastReceiver extends BroadcastReceiver {
    public static final String KEY_EXTRA_TASK = "key_task";
    public static final String NOTIFICATION_CHANNEL_ID = "task_notification";
    private static final int CONTENT_PENDING_INTENT_REQUEST_CODE = 1003;

    private static final String LOG_TAG = TaskBroadcastReceiver.class.getSimpleName();

    /**
     * Method invoked when broadcast received
     *
     * @param context Context object to access application resources
     * @param intent  Associated Intent with the broadcast
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // @Todo Add automatic screen on feature by acquiring a wakelock
        // Get the time table model from the intent extras
        TaskModel task = TaskModel.toParcelable(intent.getByteArrayExtra(KEY_EXTRA_TASK), TaskModel.CREATOR);
        Logger.d(LOG_TAG, "Alarm has been triggered for following task :- \n" + task.getInfoString());
        boolean alarmEnabled = Utilities.isTaskNotificationAlarmEnabled(context);
        // Generate a random notification id to ensure a separate notification is displayed in case several tasks have same task time.
        Random rand = new Random(System.currentTimeMillis());
        int notificationId = rand.nextInt() & Integer.MAX_VALUE; // Random positive integer
        // Create Notification for the task
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(task.getTitle())
                .setContentText(task.getDescription())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(task.getDescription()))
                .setSmallIcon(R.drawable.ic_list_item_48dp)
                .setOngoing(alarmEnabled)
                .setAutoCancel(true);
        if (task.getReminder().getDurationInMinutes() > 0) {
            Intent taskTimerIntent = new Intent(context, TaskTimerService.class);
            taskTimerIntent.putExtra(TaskTimerService.KEY_EXTRA_TASK, TaskModel.toByteArray(task));
            taskTimerIntent.putExtra(TaskTimerService.KEY_EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent taskTimerPendingIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                taskTimerPendingIntent = PendingIntent.getForegroundService(
                        context,
                        (int) task.getId(),
                        taskTimerIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
            else
                taskTimerPendingIntent = PendingIntent.getService(
                        context,
                        (int) task.getId(),
                        taskTimerIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_play, context.getString(R.string.notification_action_start_task), taskTimerPendingIntent);
        }
        Intent statsCollectIntent = new Intent(context, StatisticsReceiver.class);
        statsCollectIntent.putExtra(StatisticsReceiver.KEY_TASK_PARCEL, task);
        statsCollectIntent.putExtra(StatisticsReceiver.KEY_STATISTICS_TYPE, StatisticsReceiver.STATISTICS_TYPE_TASK);
        statsCollectIntent.putExtra(StatisticsReceiver.KEY_NOTIFICATION_ID, notificationId); // For cancelling notification which action is clicked
        // Set incomplete task status since task is not yet completed when notification is displayed
        statsCollectIntent.putExtra(StatisticsReceiver.KEY_TASK_STATUS, StatisticsReceiver.TASK_STATUS_NOT_COMPLETE);
        // Create unique pending intent associated with each task notification
        PendingIntent statsPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(), // Use task id for uniqueness
                statsCollectIntent,
                PendingIntent.FLAG_CANCEL_CURRENT // Cancel existing before creating new one
        );
        builder.addAction(R.drawable.ic_check, context.getString(R.string.notification_action_task_complete), statsPendingIntent);

        if(alarmEnabled) {
            builder.setNotificationSilent();
            Intent dismissIntent = new Intent(context,NotificationDismissReceiver.class);
            dismissIntent.putExtra(NotificationDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (int) task.getId(),
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            builder.addAction(
                    R.drawable.ic_close,
                    context.getString(R.string.notification_action_dismiss),
                    dismissPendingIntent
            );
        }

        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                CONTENT_PENDING_INTENT_REQUEST_CODE,
                i,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        builder.setContentIntent(pi); // Set pending intent on notification tap
        Notification notification = builder.build();
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Create a notification channel required by Android Oreo and later versions before displaying a notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If channel is already created the it is not recreated.
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            mgr.createNotificationChannel(channel);
        }

        mgr.notify(notificationId, notification);
        AlarmScheduler.cancelTaskAlarm(task);
        // Alarm is cleared we need to set it again to repeat task notification next time
        AlarmScheduler.setTaskAlarm(task, false);
        Logger.d(LOG_TAG, "Alarm is rescheduled for following task :-\n" + task.getInfoString());

        if (alarmEnabled) {
            Logger.d(LOG_TAG, "Starting Alarm Notification Service ...");
            Intent alarmIntent = new Intent(context, NotificationAlarmService.class);
            context.startService(alarmIntent);
        }
    }
}
