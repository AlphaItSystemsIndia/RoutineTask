package com.cod3rboy.routinetask.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.cod3rboy.routinetask.TaskApplication;
import com.cod3rboy.routinetask.database.models.ReminderModel;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.receivers.TaskBroadcastReceiver;

import java.util.Calendar;

public final class AlarmScheduler {
    private AlarmScheduler() {
    }

    /**
     * This method schedules an alarm for repeating or no-repeat tasks. Alarm is only scheduled
     * if reminder is set on the given task.
     *
     * @param task          task for which alarm is to be scheduled
     * @param taskCompleted whether the given task is completed or not
     */
    public static void setTaskAlarm(TaskModel task, boolean taskCompleted) {
        ReminderModel reminder = task.getReminder();
        if (reminder == null) return; // Do not set alarm if there is no reminder set
        long triggerTime = getAlarmTriggerTime(task, taskCompleted);
        if (triggerTime == -1) return; // return if failed to compute trigger time
        // Using reminder id as Pending Intent Request code allows to create unique pending intent for each reminder
        int pendingIntentRequestCode = (int) reminder.getId();
        Context appContext = TaskApplication.getAppContext();
        Intent intent = new Intent(appContext, TaskBroadcastReceiver.class);
        intent.putExtra(TaskBroadcastReceiver.KEY_EXTRA_TASK, TaskModel.toByteArray(task));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                pendingIntentRequestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    /**
     * This method computes alarm trigger time for repeating or no repeat tasks reminders.
     *
     * @param task          repeating or no-repeat task for which reminder trigger time is returned
     * @param taskCompleted whether task is already completed or not
     * @return trigger time in milliseconds or -1 if no reminder is set on task or trigger time cannot be computed
     */
    private static long getAlarmTriggerTime(TaskModel task, boolean taskCompleted) {
        ReminderModel reminder = task.getReminder();
        if (reminder == null) return -1;
        // Get start time for reminder
        Time startTime = reminder.getStartTime();
        // Get Calendar for current time
        Calendar calendar = Calendar.getInstance();
        // Check whether task is repeating
        boolean isTaskRepeating = task.getRepeatCountInWeek() > 0;
        long now = calendar.getTimeInMillis();
        if (isTaskRepeating) { // Get trigger time for repeating tasks
            // Check whether alarm is set for today
            calendar.set(Calendar.HOUR_OF_DAY, startTime.getHours());
            calendar.set(Calendar.MINUTE, startTime.getMinutes());
            calendar.set(Calendar.SECOND, 0);
            long triggerTime = calendar.getTimeInMillis();
            int todayDay = calendar.get(Calendar.DAY_OF_WEEK);
            if (task.isRepeatForDay(todayDay) && (now < triggerTime) && !taskCompleted) {
                return triggerTime; // Return today trigger time
            } else {
                // Find the next day for which task is repeated
                do {
                    calendar.add(Calendar.DAY_OF_WEEK, 1);
                } while (!task.isRepeatForDay(calendar.get(Calendar.DAY_OF_WEEK)));
                // Return trigger time for next day of week for which task is repeated
                return calendar.getTimeInMillis();
            }
        } else { // Get trigger time for no repeat tasks
            // Get trigger time for the day reminder was last modified
            calendar.setTimeInMillis(reminder.getLastModified());
            calendar.set(Calendar.HOUR_OF_DAY, startTime.getHours());
            calendar.set(Calendar.MINUTE, startTime.getMinutes());
            calendar.set(Calendar.SECOND, 0);
            long triggerTime = calendar.getTimeInMillis();
            if (now < triggerTime && !taskCompleted) return triggerTime;
        }
        return -1;
    }

    /**
     * This method is used to cancel scheduled alarm set for repeating and no-repeat tasks.
     *
     * @param task task whose scheduled alarm to cancel. Reminder must be set for this task.
     */
    public static void cancelTaskAlarm(TaskModel task) {
        ReminderModel reminder = task.getReminder();
        if (reminder == null) return; // Cannot cancel alarm if task does not have reminder
        // Using reminder id as Pending Intent Request code to get existing pending intent for reminder
        Context appContext = TaskApplication.getAppContext();
        int pendingIntentRequestCode = (int) reminder.getId();
        Intent intent = new Intent(appContext, TaskBroadcastReceiver.class);
        intent.putExtra(TaskBroadcastReceiver.KEY_EXTRA_TASK, TaskModel.toByteArray(task));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                pendingIntentRequestCode,
                intent,
                0);
        AlarmManager mgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null) mgr.cancel(pendingIntent);
    }

}
