package com.cod3rboy.routinetask.background;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.events.TaskCreated;
import com.cod3rboy.routinetask.events.TaskDeleted;
import com.cod3rboy.routinetask.events.TaskStatusChanged;
import com.cod3rboy.routinetask.events.TaskUpdated;
import com.cod3rboy.routinetask.events.TasksCreated;
import com.cod3rboy.routinetask.events.TasksDeleted;
import com.cod3rboy.routinetask.logging.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import java.util.Calendar;

public class TasksLoader extends AsyncTaskLoader<ArrayList<TaskModel>> {
    private static final String LOG_TAG = TasksLoader.class.getSimpleName();

    public enum TasksType {
        TODAY_TASKS, REPEATING_TASKS, NO_REPEAT_TASKS
    }

    private TasksType tasksTypeToLoad;
    private ArrayList<TaskModel> mData;

    public TasksLoader(@NonNull Context context, TasksType tasksTypeToLoad) {
        super(context);
        this.tasksTypeToLoad = tasksTypeToLoad;
        this.mData = null;
    }

    @Override
    protected void onStartLoading() {
        Logger.d(LOG_TAG, "onStartLoading()");
        // Monitoring data for changes
        registerTasksListener();
        if (takeContentChanged() || mData == null) {
            forceLoad();
            return;
        }
        deliverResult(mData);
    }

    @Override
    public ArrayList<TaskModel> loadInBackground() {
        Logger.d(LOG_TAG, "loadInBackground()- Executing task");
        ArrayList<TaskModel> tasksData = new ArrayList<>();
        switch (tasksTypeToLoad) {
            case TODAY_TASKS:
                tasksData = loadTodayTasks();
                break;
            case REPEATING_TASKS:
                tasksData = loadRepeatingTasks();
                break;
            case NO_REPEAT_TASKS:
                tasksData = loadNoRepeatTasks();
                break;
        }
        Logger.d(LOG_TAG, "Tasks Data Size - " + tasksData.size());
        return tasksData;
    }

    private ArrayList<TaskModel> loadTodayTasks() {
        Logger.d(LOG_TAG, "Loading today tasks");
        StringBuilder query = new StringBuilder();
        query.append("SELECT temp2.*, ")
                .append("ifnull(")
                .append(DBContract.RemindersTable.COL_NAME_START_TIME)
                .append(", \"99:99\") AS start_time FROM (SELECT ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(".*, ifnull(temp.completed, 0) AS completed FROM ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(" LEFT OUTER JOIN (SELECT ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME)
                .append(".")
                .append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                .append(", count(")
                .append(DBContract.RoutineEntryTable.TABLE_NAME)
                .append(".")
                .append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                .append(") AS completed FROM ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME)
                .append(" WHERE ")
                .append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                .append("=\"")
                .append(Utilities.getTodayDateString())
                .append("\" GROUP BY ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME)
                .append(".")
                .append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                .append(") AS temp ON ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(".")
                .append(DBContract.TasksTable._ID)
                .append("=")
                .append("temp.")
                .append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                .append(" WHERE ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(".")
                .append(Utilities.getTodayWeekColumn())
                .append("=")
                .append(DBContract.TasksTable.COL_VALUE_TRUE)
                .append(") AS temp2 LEFT OUTER JOIN ")
                .append(DBContract.RemindersTable.TABLE_NAME)
                .append(" ON temp2.")
                .append(DBContract.TasksTable._ID)
                .append("=")
                .append(DBContract.RemindersTable.TABLE_NAME)
                .append(".")
                .append(DBContract.RemindersTable.COL_NAME_TASK_ID)
                .append(" ORDER BY ")
                .append(Utilities.canSortCompletedTasks(getContext()) ? "temp2.completed ASC, " : "")
                .append("start_time ASC, lower(temp2.")
                .append(DBContract.TasksTable.COL_NAME_TITLE)
                .append(") ASC;");
        return TaskModel.query(query.toString(), null, true);
    }

    private ArrayList<TaskModel> loadRepeatingTasks() {
        Logger.d(LOG_TAG, "Loading repeating tasks");
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(" WHERE ");
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            query.append("(")
                    .append(Utilities.getWeekColumnName(i))
                    .append(" = ")
                    .append(DBContract.TasksTable.COL_VALUE_TRUE)
                    .append(")");
            if (i != Calendar.SATURDAY) query.append(" OR ");
        }
        query.append(" ORDER BY ")
                .append(DBContract.TasksTable._ID)
                .append(" DESC;");
        return TaskModel.query(query.toString(), null, true);
    }

    private ArrayList<TaskModel> loadNoRepeatTasks() {
        Logger.d(LOG_TAG, "Loading no repeat tasks");
        StringBuilder query = new StringBuilder();
        query.append("SELECT ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(".*, ifnull(temp.completed,0) AS completed FROM ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(" LEFT OUTER JOIN (SELECT ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME).append(".").append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                .append(", count(").append(DBContract.RoutineEntryTable.TABLE_NAME).append(".").append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                .append(") AS completed FROM ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME)
                .append(" WHERE ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME).append(".").append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                .append("=\"").append(Utilities.getTodayDateString()).append("\"")
                .append(" GROUP BY ")
                .append(DBContract.RoutineEntryTable.TABLE_NAME).append(".").append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                .append(") AS temp ON ")
                .append(DBContract.TasksTable.TABLE_NAME).append(".").append(DBContract.TasksTable._ID)
                .append("=")
                .append("temp").append(".").append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                .append(" WHERE ");
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            query.append(Utilities.getWeekColumnName(i))
                    .append(" = ")
                    .append(DBContract.TasksTable.COL_VALUE_FALSE);
            if (i != Calendar.SATURDAY) query.append(" AND ");
        }
        query.append(" ORDER BY ")
                .append(Utilities.canSortCompletedTasks(getContext()) ? " completed ASC, " : "")
                .append(DBContract.TasksTable._ID)
                .append(" DESC;");
        return TaskModel.query(query.toString(), null, true);
    }

    @Override
    public void deliverResult(@Nullable ArrayList<TaskModel> data) {
        Logger.d(LOG_TAG, "deliverResult() called");
        mData = data;
        super.deliverResult(data);
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();
        // Stop monitoring data for changes
        unregisterTasksListener();
        // Clear data
        if (mData != null) {
            mData.clear();
            mData = null;
        }
    }

    private void registerTasksListener() {
        EventBus bus = EventBus.getDefault();
        if (!bus.isRegistered(this)) bus.register(this);
    }

    private void unregisterTasksListener() {
        EventBus bus = EventBus.getDefault();
        if (bus.isRegistered(this)) bus.unregister(this);
    }

    /**
     * EventBus subscriber method which will be invoked when a new task is created.
     *
     * @param event TaskCreated event object
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskCreated(TaskCreated event) {
        Logger.d(LOG_TAG, "onTaskCreated() called");
        // Data Changed
        onContentChanged();
    }

    /**
     * EventBus subscriber method which will be invoked when new tasks are created.
     *
     * @param event TasksCreated event object
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTasksCreated(TasksCreated event) {
        // Data Changed
        onContentChanged();
    }

    /**
     * EventBus subscriber method which will be invoked when a task is deleted.
     *
     * @param event TaskDeleted event object
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskDeleted(TaskDeleted event) {
        // Data Changed
        onContentChanged();
    }

    /**
     * EventBus subscriber method which will be invoked when tasks are deleted.
     *
     * @param event TasksDeleted event object
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTasksDeleted(TasksDeleted event) {
        // Data Changed
        onContentChanged();
    }

    /**
     * EventBus subscriber method which will be invoked when a task is updated.
     *
     * @param event TaskUpdated event object
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskUpdated(TaskUpdated event) {
        // Data Changed
        onContentChanged();
    }

    /**
     * EventBus subscriber method which will be invoked when a task status is changed.
     *
     * @param event TaskStatusChanged event object
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskStatusChanged(TaskStatusChanged event) {
        if (Utilities.canPlayTaskCompleteSound(getContext()) && event.isCompleted()) {
            // Play sound
            MediaPlayer player = MediaPlayer.create(getContext(), R.raw.sound_task_complete);
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
        }
        // Data Changed
        onContentChanged();
    }
}