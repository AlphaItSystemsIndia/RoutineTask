package com.cod3rboy.routinetask.services;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cod3rboy.routinetask.CountDownTimer;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.models.ReminderModel;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.TaskBroadcastReceiver;
import com.cod3rboy.routinetask.receivers.TaskTimerReceiver;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskTimerService extends IntentService {

    public static final String KEY_EXTRA_TASK = "extra_task";
    public static final String KEY_EXTRA_NOTIFICATION_ID = "notification_id";
    private static final String LOG_TAG = TaskTimerService.class.getSimpleName();

    private CountDownTimer timer;
    private TaskModel task;
    private ReminderModel reminder;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int notificationId;
    private final int MAX_PROGRESS = 100;
    private AtomicBoolean suspendAfterTimer = new AtomicBoolean(false);
    private static TaskTimerService service;
    private NotificationCompat.Action actionCancel;

    public static TaskTimerService getService() {
        return service;
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public TaskTimerService() {
        super(TaskTimerService.class.getSimpleName());
        Logger.d(LOG_TAG, "Constructing TaskTimerService ...");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (service == null) service = this;
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this, TaskBroadcastReceiver.NOTIFICATION_CHANNEL_ID);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(LOG_TAG, "Destroying TaskTimerService ...");
    }

    public TaskModel getTask() {
        return task;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void stopService() {
        timer.cancelTimer();
        suspendAfterTimer.set(true);
        service = null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Logger.d(LOG_TAG, "onHandleIntent Called with intent : " + intent.toString());
        if (!intent.hasExtra(KEY_EXTRA_TASK)) return;
        task = TaskModel.toParcelable(intent.getByteArrayExtra(KEY_EXTRA_TASK), TaskModel.CREATOR);
        reminder = task.getReminder(); // Task Timer service will only run for tasks having reminders set
        if (reminder == null) return;
        long millis = reminder.getDurationInMinutes() * 60 * 1000;
        if (!intent.hasExtra(KEY_EXTRA_NOTIFICATION_ID)) return;
        notificationId = intent.getIntExtra(KEY_EXTRA_NOTIFICATION_ID, -1);
        if (notificationId == -1) return;
        notificationManager.cancel(notificationId);
        // Stop running notification alarm
        if(NotificationAlarmService.getSelf() != null) NotificationAlarmService.getSelf().stop();
        // Create new notification id for this service
        notificationId = new Random(System.currentTimeMillis()).nextInt() & Integer.MAX_VALUE;
        // Show Notification
        Notification notification = makeNotification(task.getTitle(), millis / 1000);
        startForeground(notificationId, notification);
        // Let the background thread do the job
        timer = new CountDownTimer(millis, 1000) {
            private int progress = 0;

            @Override
            public void onTick(long millisLeft) {
                long millisPassed = millis - millisLeft;
                Logger.d(LOG_TAG,
                        String.format("Task Timer - Milliseconds Passed : %d \t Milliseconds Left : %d", millisPassed, millisLeft));
                progress = MAX_PROGRESS - (int) ((millisPassed * 1f * MAX_PROGRESS) / millis);
                updateNotification(millisLeft / 1000, progress);
            }

            @Override
            public void onFinish() {
                Logger.d(LOG_TAG, "Notification Timer Finished.");
                updateFinalNotification();
            }
        };
        timer.startTimer();
        try {
            Logger.d(LOG_TAG, "Waiting to finish Notification Timer");
            timer.join(); // Wait for timer thread to finish
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.d(LOG_TAG, "Timer error has occurred : " + ex.getMessage());
        }
        if (!timer.isCancelled()) {
            // Timer finished normally
            MediaPlayer mp = null;
            if(Utilities.isTaskDurationAlarmEnabled(this)){
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                attrBuilder.setLegacyStreamType(AudioManager.STREAM_ALARM);
                // Play Duration Alarm using Media Player on Alarm Stream
                mp = MediaPlayer.create(
                        this,
                        alarmSound,
                        null,
                        attrBuilder.build(),
                        audioManager.generateAudioSessionId()
                );
                mp.setLooping(true);
                mp.start();
            }
            try {
                while (!suspendAfterTimer.get()) Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.d(LOG_TAG, "Service error has occurred : " + ex.getMessage());
                ex.printStackTrace();
            }
            if(mp != null){
                mp.stop();
                mp.release();
            }
        }
    }

    private Notification makeNotification(String title, long seconds) {
        // Create Notification for the task
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationContent(seconds)))
                .setSmallIcon(R.drawable.ic_list_item_48dp)
                .setOngoing(true)
                .setProgress(MAX_PROGRESS, MAX_PROGRESS, false)
                .setNotificationSilent()
                .setAutoCancel(false);
        Intent cancelIntent = new Intent(this, TaskTimerReceiver.class);
        cancelIntent.putExtra(TaskTimerReceiver.KEY_EXTRA_ACTION, TaskTimerReceiver.ACTION_SERVICE_STOP);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                this,
                -1,
                cancelIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        actionCancel = new NotificationCompat.Action(R.drawable.ic_close, getString(R.string.notification_action_cancel), cancelPendingIntent);
        notificationBuilder.addAction(actionCancel);

        Intent completeTaskIntent = new Intent(getApplicationContext(), TaskTimerReceiver.class);
        completeTaskIntent.putExtra(TaskTimerReceiver.KEY_EXTRA_ACTION, TaskTimerReceiver.ACTION_COMPLETE_TASK);
        PendingIntent completeTaskPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                (int) task.getId(),
                completeTaskIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        notificationBuilder.addAction(
                R.drawable.ic_check,
                getApplicationContext().getString(R.string.notification_action_task_complete),
                completeTaskPendingIntent
        );
        return notificationBuilder.build();
    }

    private void updateNotification(long secsLeft, int progress) {
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationContent(secsLeft)));
        notificationBuilder.setProgress(MAX_PROGRESS, progress, false);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    @SuppressLint("RestrictedApi")
    private void updateFinalNotification() {
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationContent(0)));
        // Make Notification Non-Silent if alarm is not enabled
        if(!Utilities.isTaskDurationAlarmEnabled(this)){
            // Use reflection to enable notification sound because setNotificationSilent() was called on Builder.
            try {
                Field silentField = notificationBuilder.getClass().getDeclaredField("mSilent");
                silentField.setAccessible(true);
                silentField.setBoolean(notificationBuilder, false);
            }catch (NoSuchFieldException | IllegalAccessException ex){
                Logger.e(LOG_TAG, "ERROR! Cannot access mSilent field in notification builder object");
            }
        }
        notificationBuilder.setProgress(0, 0, false);
        Intent dismissIntent = new Intent(this, TaskTimerReceiver.class);
        // Remove cancel action from notification builder
        notificationBuilder.mActions.remove(actionCancel);
        dismissIntent.putExtra(TaskTimerReceiver.KEY_EXTRA_ACTION, TaskTimerReceiver.ACTION_SERVICE_STOP);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                dismissIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        notificationBuilder.addAction(
                R.drawable.ic_close,
                getString(R.string.notification_action_dismiss),
                dismissPendingIntent
        );
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private String getNotificationContent(long seconds) {
        StringBuilder sb = new StringBuilder();
        if (seconds > 0) {
            long hours = seconds / 3600;
            seconds = seconds % 3600;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            if (hours > 0) {
                sb.append(hours);
                sb.append("h ");
            }
            if (minutes > 0) {
                sb.append(minutes);
                sb.append("m ");
            }

            if (seconds > 0) {
                sb.append(seconds);
                sb.append("s ");
            }
            return getString(R.string.notification_task_timer_content, sb.toString());
        }
        return getString(R.string.notification_task_timer_time_up);
    }
}
