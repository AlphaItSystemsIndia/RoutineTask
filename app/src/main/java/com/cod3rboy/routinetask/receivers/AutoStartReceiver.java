package com.cod3rboy.routinetask.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.utilities.AlarmScheduler;

import java.util.ArrayList;

import needle.Needle;

/**
 * This receiver is registered for responding to BOOT_COMPLETED, DATE_CHANGED and TIME_SET intents.
 * It is used to set task alarms on phone boot since all alarms are lost when phone is shutdown.
 * Also when date time is changed by user all alarms are updated to trigger on time relative to
 * changed date time.
 */
public class AutoStartReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = AutoStartReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if ((intentAction == null) ||
                (!intentAction.equals(Intent.ACTION_BOOT_COMPLETED)
                        && !intentAction.equals(Intent.ACTION_DATE_CHANGED)
                        && !intentAction.equals(Intent.ACTION_TIME_CHANGED)))
            return;
        Needle.onBackgroundThread().execute(() -> {
            Logger.d("Started " + AutoStartReceiver.class.getSimpleName() + " with Intent Action " + intentAction);
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM ")
                    .append(DBContract.TasksTable.TABLE_NAME)
                    .append(" INNER JOIN ")
                    .append(DBContract.RemindersTable.TABLE_NAME)
                    .append(" ON ")
                    .append(DBContract.TasksTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.TasksTable._ID)
                    .append("=")
                    .append(DBContract.RemindersTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.RemindersTable.COL_NAME_TASK_ID)
                    .append(";");
            ArrayList<TaskModel> allTasks = TaskModel.query(query.toString(), null, true);
            for (TaskModel task : allTasks) {
                if (task.getReminder() != null)
                    AlarmScheduler.setTaskAlarm(task, task.getStatus() == TaskModel.TaskStatus.COMPLETED);
            }
            Logger.d(LOG_TAG, "Synchronized timings for all task alarms");
        });
    }
}
