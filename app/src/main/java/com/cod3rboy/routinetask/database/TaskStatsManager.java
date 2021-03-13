package com.cod3rboy.routinetask.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cod3rboy.routinetask.TaskApplication;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.events.RandomStatsGenerated;
import com.cod3rboy.routinetask.events.StatisticsReset;
import com.cod3rboy.routinetask.events.TaskStatusChanged;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.TodayTaskWidgetProvider;
import com.cod3rboy.routinetask.utilities.AlarmScheduler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.HashSet;

import needle.Needle;

/**
 * TaskStatsManager class provides functionality related to collection and retrieval of
 * Routine Tasks Statistics Info.
 */
public class TaskStatsManager {

    public interface SuccessListener {
        void onSuccess();
    }

    private static final String LOG_TAG = TaskStatsManager.class.getSimpleName();

    private static final String TASK_TYPE_STATS_TASK = "task_stats";

    private static TaskStatsManager singleton = null;

    public static TaskStatsManager getInstance() {
        if (singleton == null) {
            singleton = new TaskStatsManager();
        }
        return singleton;
    }

    private TaskStatsManager() {
    }

    /**
     * This method is used to mark the task that it has completed today.
     * It uses separate thread for operation.
     *
     * @param task     TaskModel object to mark complete
     * @param listener callback to execute when task is completed successfully
     */
    public void setTaskDone(TaskModel task, SuccessListener listener) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_STATS_TASK).execute(() -> {
            if (task == null) return;
            // Here we have to set the task entry into statistics
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                String todayDate = Utilities.getTodayDateString();
                // Query whether any entry of given task for today already exists.
                Cursor c = db.query(DBContract.RoutineEntryTable.TABLE_NAME,
                        new String[]{DBContract.RoutineEntryTable.COL_NAME_TASK_ID},
                        String.format("%s = ? AND %s = ?", DBContract.RoutineEntryTable.COL_NAME_TASK_ID, DBContract.RoutineEntryTable.COL_NAME_DATE),
                        new String[]{String.valueOf(task.getId()), todayDate}, null, null, null);
                int total = c.getCount();
                c.close();
                if (total <= 0) {
                    // Only proceed if entry does not exists.
                    ContentValues cv = new ContentValues();
                    cv.put(DBContract.RoutineEntryTable.COL_NAME_TASK_ID, task.getId());
                    cv.put(DBContract.RoutineEntryTable.COL_NAME_DATE, todayDate);
                    long id = db.insert(DBContract.RoutineEntryTable.TABLE_NAME, null, cv);
                    if (id > 0) {
                        Logger.d(LOG_TAG, String.format("Created Routine task entry with id %d for task id %d and date %s", id, task.getId(), todayDate));
                        if (listener != null) listener.onSuccess();
                    }
                    // Query whether count record of today already exists
                    c = db.query(DBContract.RoutineStatsTable.TABLE_NAME,
                            new String[]{DBContract.RoutineStatsTable._ID, DBContract.RoutineStatsTable.COL_NAME_COUNT},
                            String.format("%s = ?", DBContract.RoutineStatsTable.COL_NAME_DATE), new String[]{todayDate},
                            null, null, null);
                    total = c.getCount();
                    ContentValues cv2 = new ContentValues();
                    if (total <= 0) {
                        // Make first entry
                        cv2.put(DBContract.RoutineStatsTable.COL_NAME_DATE, todayDate);
                        cv2.put(DBContract.RoutineStatsTable.COL_NAME_COUNT, 1);
                        long id2 = db.insert(DBContract.RoutineStatsTable.TABLE_NAME, null, cv2);
                        if (id2 > 0) {
                            Logger.d(LOG_TAG, String.format("Created Routine task stats count record for task id %d with count value %d ", task.getId(), 1));
                        }
                    } else {
                        // Update Existing entry
                        c.moveToFirst();
                        int count = c.getInt(1) + 1;
                        cv2.put(DBContract.RoutineStatsTable.COL_NAME_COUNT, count);
                        int rowsAffected = db.update(DBContract.RoutineStatsTable.TABLE_NAME, cv2,
                                String.format("%s = ?", DBContract.RoutineStatsTable.COL_NAME_DATE),
                                new String[]{todayDate});
                        if (rowsAffected > 0) {
                            Logger.d(LOG_TAG, String.format("Updated Routine task stats count record for task id %d with count value %d ", task.getId(), count));
                        }

                    }
                    c.close();
                    AlarmScheduler.cancelTaskAlarm(task);
                    AlarmScheduler.setTaskAlarm(task, true);
                    // Notify Task Status Changed
                    EventBus.getDefault().post(new TaskStatusChanged(task, true));
                } else {
                    Logger.d(LOG_TAG, String.format("Routine task entry already exists for task id %d and date %s", task.getId(), todayDate));
                }

                // Refresh any Widgets
                TodayTaskWidgetProvider.refreshWidgets();
            }
        });
    }

    /**
     * This method is used to unmark the task that it has completed today. This the undo operation
     * of marking a task complete for today.
     * It uses separate thread for operation.
     *
     * @param task     task to unmark
     * @param listener callback to execute when task is set in completed
     */
    public void setTaskUndone(TaskModel task, SuccessListener listener) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_STATS_TASK).execute(() -> {
            if (task == null) return;
            // Here we have to unset the task entry from statistics (for undo operation)
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                String todayDate = Utilities.getTodayDateString();
                // Query the existing record entry of given task id for today
                Cursor c = db.query(DBContract.RoutineEntryTable.TABLE_NAME,
                        new String[]{DBContract.RoutineEntryTable.COL_NAME_TASK_ID},
                        String.format("%s = ? AND %s = ?", DBContract.RoutineEntryTable.COL_NAME_TASK_ID, DBContract.RoutineEntryTable.COL_NAME_DATE),
                        new String[]{String.valueOf(task.getId()), todayDate}, null, null, null);
                int total = c.getCount();
                c.close();
                if (total > 0) {
                    // Only proceed if entry exists.
                    int rowsAffected = db.delete(DBContract.RoutineEntryTable.TABLE_NAME,
                            String.format("%s = ? AND %s = ?", DBContract.RoutineEntryTable.COL_NAME_TASK_ID, DBContract.RoutineEntryTable.COL_NAME_DATE),
                            new String[]{String.valueOf(task.getId()), todayDate});
                    if (rowsAffected > 0) {
                        Logger.d(LOG_TAG, String.format("Deleted Routine task entry for task id %d and date %s", task.getId(), todayDate));
                        if (listener != null) listener.onSuccess();
                    }
                    // Query whether count record of today already exists
                    c = db.query(DBContract.RoutineStatsTable.TABLE_NAME,
                            new String[]{DBContract.RoutineStatsTable._ID, DBContract.RoutineStatsTable.COL_NAME_COUNT},
                            String.format("%s = ?", DBContract.RoutineStatsTable.COL_NAME_DATE), new String[]{todayDate},
                            null, null, null);
                    total = c.getCount();
                    if (total > 0) {
                        c.moveToFirst();
                        int count = c.getInt(1) - 1;
                        if (count <= 0) {
                            // Delete entry when count becomes zero
                            int deletedRows = db.delete(DBContract.RoutineStatsTable.TABLE_NAME,
                                    String.format("%s = ?", DBContract.RoutineStatsTable.COL_NAME_DATE),
                                    new String[]{todayDate});
                            if (deletedRows > 0) {
                                Logger.d(LOG_TAG, String.format("Deleted Routine task stats count record for date %s with count value %d ", todayDate, 0));
                            }
                        } else {
                            ContentValues cv2 = new ContentValues();
                            cv2.put(DBContract.RoutineStatsTable.COL_NAME_COUNT, count);
                            int updatedRows = db.update(DBContract.RoutineStatsTable.TABLE_NAME, cv2,
                                    String.format("%s = ?", DBContract.RoutineStatsTable.COL_NAME_DATE),
                                    new String[]{todayDate});
                            if (updatedRows > 0) {
                                Logger.d(LOG_TAG, String.format("Updated Routine task stats count record for date %s with count value %d ", todayDate, count));
                            }
                        }
                    }
                    c.close();
                    // Now update completed task alarm to next appropriate weekday
                    AlarmScheduler.setTaskAlarm(task, false);
                    // Notify Task Status Changed
                    EventBus.getDefault().post(new TaskStatusChanged(task, false));
                } else {
                    Logger.d(LOG_TAG, String.format("Routine task entry does not exist for task id %d and date %s", task.getId(), todayDate));
                }
                // Refresh any Widgets
                TodayTaskWidgetProvider.refreshWidgets();
            }
        });
    }


    public void generateRandomTaskStats(final String startDate, final int rangeMin, final int rangeMax) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_STATS_TASK).execute(() -> {
            final int SKIP_DAYS_LIMIT = 3;
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(Objects.requireNonNull(Utilities.getDateObject(startDate)));
            end.setTime(Objects.requireNonNull(Utilities.getDateObject(Utilities.getTodayDateString())));
            int value = -1;
            String date = null;
            // Generate random tasks
            TaskModel[] randomTasks = new TaskModel[rangeMax];
            for (int i = 0; i < randomTasks.length; i++) {
                randomTasks[i] = TaskModel.makeRandomTask();
                randomTasks[i].save(false);
            }
            // Choose the `value` number of random tasks
            HashSet<TaskModel> randomTaskChoices = new HashSet<>();

            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                while (start.compareTo(end) <= 0) {
                    date = Utilities.formatDateObject(start.getTime());
                    value = rangeMin + ((int) (Math.random() * (rangeMax - rangeMin) + 1));
                    // Fill Routine Stats Table
                    ContentValues cv = new ContentValues();
                    cv.put(DBContract.RoutineStatsTable.COL_NAME_DATE, date);
                    cv.put(DBContract.RoutineStatsTable.COL_NAME_COUNT, value);
                    db.insert(DBContract.RoutineStatsTable.TABLE_NAME, null, cv);

                    while (randomTaskChoices.size() < value) {
                        int randIndex = (int) (Math.random() * randomTasks.length);
                        TaskModel randomTask = randomTasks[randIndex];
                        randomTask.setRepeatForDay(start.get(Calendar.DAY_OF_WEEK));
                        randomTaskChoices.add(randomTask);
                    }
                    // Make entries in Routine Task entry table
                    for (TaskModel randomTaskChoice : randomTaskChoices) {
                        ContentValues cv2 = new ContentValues();
                        cv2.put(DBContract.RoutineEntryTable.COL_NAME_TASK_ID, randomTaskChoice.getId());
                        cv2.put(DBContract.RoutineEntryTable.COL_NAME_DATE, date);
                        db.insert(DBContract.RoutineEntryTable.TABLE_NAME, null, cv2);
                    }

                    if (Utilities.getRandomBoolean()) {
                        start.add(Calendar.DAY_OF_MONTH, (int) (Math.random() * SKIP_DAYS_LIMIT + 1));
                    } else {
                        start.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            }
            // Update chosen tasks
            for (TaskModel chosenTask : randomTaskChoices) chosenTask.save(false);

            // Notify Event Subscribers
            EventBus.getDefault().post(new RandomStatsGenerated());
        });
    }


    /**
     * Database Operations are performed so call this on separate thread.
     *
     * @return HashMap containing stats data of tasks count. Keys are dates and values are count.
     */
    public HashMap<String, Integer> getTasksCountStats(Context context) {
        HashMap<String, Integer> countData = new HashMap<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = db.query(DBContract.RoutineStatsTable.TABLE_NAME,
                    new String[]{DBContract.RoutineStatsTable.COL_NAME_DATE, DBContract.RoutineStatsTable.COL_NAME_COUNT},
                    null,
                    null,
                    null,
                    null,
                    DBContract.RoutineStatsTable._ID + " DESC ");
            while (c.moveToNext()) {
                String date = c.getString(0);
                int count = c.getInt(1);
                countData.put(date, count);
                ;
            }
            c.close();
        }
        return countData;
    }


    /**
     * Database Operations are performed so call this on separate thread.
     *
     * @param date date for which all tasks completed on this date to be retrieved
     * @return ArrayList of all time table models generated from retrieved tasks
     */
    public ArrayList<TaskModel> getCompletedTasksOnDate(String date) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT " + DBContract.TasksTable.TABLE_NAME + ".* FROM ");
        query.append(DBContract.TasksTable.TABLE_NAME);
        query.append(" INNER JOIN (SELECT " + DBContract.RoutineEntryTable.COL_NAME_TASK_ID);
        query.append(" FROM " + DBContract.RoutineEntryTable.TABLE_NAME);
        query.append(" WHERE " + DBContract.RoutineEntryTable.COL_NAME_DATE);
        query.append("='" + date + "') as temp ON ");
        query.append(DBContract.TasksTable.TABLE_NAME + "." + DBContract.TasksTable._ID + "=");
        query.append("temp." + DBContract.RoutineEntryTable.COL_NAME_TASK_ID);
        query.append(" ORDER BY " + DBContract.TasksTable._ID + " ASC;");
        return TaskModel.query(query.toString(), null, false);
    }


    public void resetTasksStatistics(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                synchronized (dbHelper) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int rowsAffected = db.delete(DBContract.RoutineEntryTable.TABLE_NAME, "1", null);
                    if (rowsAffected > 0)
                        Logger.d(LOG_TAG, "Deleted all records from Routine entry table");
                    rowsAffected = db.delete(DBContract.RoutineStatsTable.TABLE_NAME, "1", null);
                    if (rowsAffected > 0)
                        Logger.d(LOG_TAG, "Deleted all records from Routine stats table");
                }
                if (EventBus.getDefault().hasSubscriberForEvent(StatisticsReset.class))
                    EventBus.getDefault().post(new StatisticsReset());
            }
        }).start();
    }
}
