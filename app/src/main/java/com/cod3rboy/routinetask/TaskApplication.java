package com.cod3rboy.routinetask;

import android.app.Application;
import android.content.Context;

import com.cod3rboy.crashbottomsheet.CrashBottomSheet;
import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.utilities.AlarmScheduler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Locale;

import needle.Needle;

/**
 * Here Application Class is subclassed solely for the purpose to set task alarms whenever our
 * app starts. Since if user  force closes our app, all set alarms are lost so at the next app
 * startup we will need to set tasks alarms again. We can do this in Activity but then task alarms
 * will get set again and again during Activity Lifecycle. But in case of Application, since application
 * is singleton therefore task alarms are set only the first time when an instance of application is created
 * as user launches our app.
 */
public class TaskApplication extends Application {
    private static final String LOG_TAG = TaskApplication.class.getSimpleName();
    private static Context sApplicationContext = null;

    public static Context getAppContext() {
        return sApplicationContext;
    }

    public TaskApplication() {
        super();
        sApplicationContext = this;
        // Register CrashBottomSheet
        CrashBottomSheet.register(this);
        // Install Event Bus Subscribers Index Class
        EventBus.builder().addIndex(new AppEventBustIndex()).installDefaultEventBus();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Needle.onBackgroundThread().execute(this::refreshAlarmsInBackground);
    }

    /**
     * Here set the task alarms in background thread
     */
    public void refreshAlarmsInBackground() {
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
        Logger.d(LOG_TAG, String.format(Locale.getDefault(),"All %d tasks alarms are refreshed",allTasks.size()));
    }
}
