package com.cod3rboy.routinetask.database.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.DBQueryExecutor;
import com.cod3rboy.routinetask.database.TaskStatsManager;
import com.cod3rboy.routinetask.events.TaskCreated;
import com.cod3rboy.routinetask.events.TaskDeleted;
import com.cod3rboy.routinetask.events.TaskEvent;
import com.cod3rboy.routinetask.events.TaskLoaded;
import com.cod3rboy.routinetask.events.TaskUpdated;
import com.cod3rboy.routinetask.events.TasksCreated;
import com.cod3rboy.routinetask.events.TasksDeleted;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.TodayTaskWidgetProvider;
import com.cod3rboy.routinetask.utilities.AlarmScheduler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * This model class represents a task. It stores task data and provides methods to perform CRUD
 * operations.
 * This Class also implements Parcelable so it can be passed around between processes (eg. passed in intents
 * for alarms to get task info to create notifications.)
 * Note that putting Parcelable in PendingIntents does not work and is not reliable in Nougat and later android versions
 * so this class defines two methods to convert parcelable to byte array and read parcelable from byte array.
 */
public class TaskModel implements Parcelable {
    /**
     * Enum representing the status of the task -
     * COMPLETED : Task is completed
     * INCOMPLETE : Task is not yet completed
     * UNKNOWN : It is not known whether task is completed or not
     */
    public enum TaskStatus {
        COMPLETED, INCOMPLETE, UNKNOWN
    }

    private static final String LOG_TAG = TaskModel.class.getSimpleName();
    private long id;
    private String title;
    private String description;
    private int color;
    private boolean repeatSunday;
    private boolean repeatMonday;
    private boolean repeatTuesday;
    private boolean repeatWednesday;
    private boolean repeatThursday;
    private boolean repeatFriday;
    private boolean repeatSaturday;
    private TaskStatus status;
    private ReminderModel reminder;

    /**
     * This method generates a random Task Model.
     *
     * @return a random task model
     */
    public static TaskModel makeRandomTask() {
        TaskModel randomTask = new TaskModel();
        randomTask.title = Utilities.getRandomAlphaString(10, DBContract.TasksTable.COL_LENGTH_TITLE);
        randomTask.description = Utilities.getRandomAlphaString(40, DBContract.TasksTable.COL_LENGTH_DESC);
        randomTask.color = DBContract.BG_COLORS[(int) (Math.random() * DBContract.BG_COLORS.length)];
        randomTask.repeatSunday = Utilities.getRandomBoolean();
        randomTask.repeatMonday = Utilities.getRandomBoolean();
        randomTask.repeatTuesday = Utilities.getRandomBoolean();
        randomTask.repeatWednesday = Utilities.getRandomBoolean();
        randomTask.repeatThursday = Utilities.getRandomBoolean();
        randomTask.repeatFriday = Utilities.getRandomBoolean();
        randomTask.repeatSaturday = Utilities.getRandomBoolean();
        randomTask.status = TaskStatus.INCOMPLETE;
        if (Math.random() > 0.5) randomTask.reminder = ReminderModel.makeRandomReminder(-1);
        return randomTask;
    }

    /**
     * This method is used to synchronously query the tasks models for the given sql command.
     * It does not load other data related to the task. It must not be invoked by Main Thread.
     *
     * @param sql           SQL command string
     * @param selectionArgs selection arguments for SQL command
     * @param loadRelation  Whether to load other data related to task
     * @return List of tasks matched by the sql query
     */
    public static ArrayList<TaskModel> query(String sql, String[] selectionArgs, boolean loadRelation) {
        ArrayList<TaskModel> result = new ArrayList<>();
        Cursor data = DBQueryExecutor.query(sql, selectionArgs);
        while (data.moveToNext()) {
            TaskModel task = makeFromCursor(data);
            result.add(task);
            if (loadRelation) {
                // Load Reminder data
                task.setReminder(ReminderModel.getForTask(task.getId()));
                // Determine whether task is completed or not
                boolean completed = isTaskCompletedToday(task);
                if (completed) task.status = TaskStatus.COMPLETED;
                else task.status = TaskStatus.INCOMPLETE;
            }
        }
        data.close();
        return result;
    }

    /**
     * This method is used to asynchronously fetch task model for given id.
     *
     * @param taskId       id of task model to fetch
     * @param loadRelation whether to load other data related to task
     */
    public static void fetch(long taskId, boolean loadRelation) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("SELECT * FROM ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(" WHERE ")
                .append(DBContract.TasksTable._ID)
                .append(" = ?;");
        DBQueryExecutor.queryAsync(
                cmdBuilder.toString(),
                new String[]{String.valueOf(taskId)},
                result -> {
                    TaskModel task = null;
                    if (result != null && result.moveToFirst()) {
                        task = makeFromCursor(result);
                    }
                    result.close();
                    if (task != null) {
                        if (loadRelation) {
                            // Load Reminder data
                            task.setReminder(ReminderModel.getForTask(taskId));
                            // Determine whether task is completed or not
                            boolean completed = isTaskCompletedToday(task);
                            if (completed) task.status = TaskStatus.COMPLETED;
                            else task.status = TaskStatus.INCOMPLETE;
                        }
                        // Notify Subscribers
                        postEvent(new TaskLoaded(task), TaskLoaded.class);
                    }
                }
        );
    }

    /**
     * This method synchronously retrieves task model for a given task id. This method should
     * not invoked by Main Thread.
     *
     * @param taskId       id of task for which task model will be fetched
     * @param loadRelation whether to load other data related to task
     * @return Task model is returned if task found with given id otherwise null is returned
     */
    public static TaskModel get(long taskId, boolean loadRelation) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("SELECT * FROM ")
                .append(DBContract.TasksTable.TABLE_NAME)
                .append(" WHERE ")
                .append(DBContract.TasksTable._ID)
                .append(" = ?;");
        Cursor dataCursor = DBQueryExecutor.query(cmdBuilder.toString(), new String[]{String.valueOf(taskId)});
        TaskModel task = null;
        if (dataCursor != null && dataCursor.moveToFirst())
            task = makeFromCursor(dataCursor);
        dataCursor.close();
        if (task != null && loadRelation) {
            // Load Reminder Data
            task.setReminder(ReminderModel.getForTask(taskId));
            // Determine whether task is completed or not
            boolean completed = isTaskCompletedToday(task);
            if (completed) task.status = TaskStatus.COMPLETED;
            else task.status = TaskStatus.INCOMPLETE;
        }
        return task;
    }

    private static boolean isTaskCompletedToday(TaskModel task) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("SELECT ");
        cmdBuilder.append(DBContract.RoutineEntryTable._ID);
        cmdBuilder.append(" FROM ");
        cmdBuilder.append(DBContract.RoutineEntryTable.TABLE_NAME);
        cmdBuilder.append(" WHERE ");
        cmdBuilder.append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID);
        cmdBuilder.append(" = ?");
        boolean taskRepeatable = task.getRepeatCountInWeek() > 0;
        String[] queryArgs;
        if (taskRepeatable) {
            // Repeatable tasks have multiple entries and we should only check for today date
            cmdBuilder.append(" AND ");
            cmdBuilder.append(DBContract.RoutineEntryTable.COL_NAME_DATE);
            cmdBuilder.append(" = ?");
            queryArgs = new String[]{
                    String.valueOf(task.getId()),
                    Utilities.getTodayDateString()
            };
        } else {
            queryArgs = new String[]{
                    String.valueOf(task.getId())
            };
        }
        cmdBuilder.append(";");
        Cursor queryResult = DBQueryExecutor.query(cmdBuilder.toString(), queryArgs);
        boolean completed = false;
        if (queryResult != null) {
            if (queryResult.getCount() > 0) completed = true;
            queryResult.close();
        }
        return completed;
    }

    /**
     * This methods asynchronously inserts a new task model into database and set the rowId in the
     * task object.
     *
     * @param task           New task model to insert
     * @param insertRelation Whether to insert data related to task
     */
    public static void insertAsync(TaskModel task, boolean insertRelation) {
        DBQueryExecutor.insertAsync(
                DBContract.TasksTable.TABLE_NAME,
                null,
                task.makeContentValues(),
                taskId -> {
                    task.id = taskId;
                    if (insertRelation) {
                        // Also insert task reminder
                        ReminderModel reminder = task.getReminder();
                        if (reminder != null) {
                            reminder.setTaskId(task.id);
                            reminder.save(false);
                            // Schedule the task alarm
                            Logger.d(LOG_TAG, "insert(task, relation) - Scheduling task alarm for newly created task");
                            AlarmScheduler.setTaskAlarm(task, false);
                        }
                    }
                    // Refresh Widgets
                    TodayTaskWidgetProvider.refreshWidgets();
                    // Notify Subscribers
                    postEvent(new TaskCreated(task), TaskCreated.class);
                }
        );
    }

    /**
     * This methods synchronously inserts a new task model into database and set the rowId in the
     * task object. It must not be executed on Main Thread.
     *
     * @param task           New task model to insert
     * @param insertRelation Whether to insert data related to task
     * @return true  if tasks is successfully inserted otherwise false
     */
    public static boolean insert(TaskModel task, boolean insertRelation) {
        long rowId = DBQueryExecutor.insert(
                DBContract.TasksTable.TABLE_NAME,
                null,
                task.makeContentValues()
        );
        if (rowId > 0) {
            task.id = rowId;
            if (insertRelation) {
                // Also insert task reminder
                ReminderModel reminder = task.getReminder();
                if (reminder != null) {
                    reminder.setTaskId(task.id);
                    reminder.save(false);
                    // Schedule the task alarm
                    Logger.d(LOG_TAG, "insert(task, relation) - Scheduling task alarm for newly created task");
                    AlarmScheduler.setTaskAlarm(task, false);
                }
            }
            // Refresh Widgets
            TodayTaskWidgetProvider.refreshWidgets();
            return true;
        }
        return false;
    }

    /**
     * This methods asynchronously inserts multiple task models into database and set the rowIds in the
     * task objects.
     *
     * @param tasks          New task models to insert
     * @param insertRelation Whether to insert data related to task
     */
    public static void insertAsync(ArrayList<TaskModel> tasks, boolean insertRelation) {
        ContentValues[] values = new ContentValues[tasks.size()];
        for (int i = 0; i < values.length; i++) values[i] = tasks.get(i).makeContentValues();
        DBQueryExecutor.insertAsync(
                DBContract.TasksTable.TABLE_NAME,
                null,
                values,
                taskIds -> {
                    for (int i = 0; i < taskIds.length; i++) {
                        tasks.get(i).id = taskIds[i];
                        if (insertRelation) {
                            // Also insert task reminder
                            ReminderModel reminder = tasks.get(i).getReminder();
                            if (reminder != null) {
                                reminder.setTaskId(tasks.get(i).id);
                                reminder.save(false);
                                // Schedule the task alarm
                                Logger.d(LOG_TAG, "insert(tasks, relation) - Scheduling task alarm for newly created task");
                                AlarmScheduler.setTaskAlarm(tasks.get(i), false);
                            }
                        }
                    }
                    // Refresh Widgets
                    TodayTaskWidgetProvider.refreshWidgets();
                    // Notify Subscribers
                    postEvent(new TasksCreated(tasks), TasksCreated.class);
                }
        );
    }

    /**
     * This methods synchronously inserts multiple task models into database and set the rowIds in the
     * task objects. It must not be executed on Main Thread.
     *
     * @param tasks          New task models to insert
     * @param insertRelation Whether to insert data related to task
     * @return true if and only if all tasks are successfully inserted otherwise false
     */
    public static boolean insert(ArrayList<TaskModel> tasks, boolean insertRelation) {
        ContentValues[] values = new ContentValues[tasks.size()];
        for (int i = 0; i < values.length; i++) values[i] = tasks.get(i).makeContentValues();
        long[] rowIds = DBQueryExecutor.insert(
                DBContract.TasksTable.TABLE_NAME,
                null,
                values
        );
        boolean success = true;
        for (int i = 0; i < rowIds.length; i++) {
            if (rowIds[i] <= 0) success = false;
            tasks.get(i).id = rowIds[i];
            if (insertRelation) {
                // Also insert task reminder
                ReminderModel reminder = tasks.get(i).getReminder();
                if (reminder != null) {
                    reminder.setTaskId(tasks.get(i).id);
                    reminder.save(false);
                    // Schedule the task alarm
                    Logger.d(LOG_TAG, "insert(tasks, relation) - Scheduling task alarm for newly created task");
                    AlarmScheduler.setTaskAlarm(tasks.get(i), false);
                }
            }
        }
        // Refresh Widgets
        TodayTaskWidgetProvider.refreshWidgets();
        return success;
    }

    /**
     * This method asynchronously updates an existing task model in database.
     *
     * @param task           Existing task model to update
     * @param updateRelation Whether to update other data related to task
     */
    public static void updateAsync(TaskModel task, boolean updateRelation) {
        DBQueryExecutor.updateAsync(
                DBContract.TasksTable.TABLE_NAME,
                task.getId(),
                task.makeContentValues(),
                taskId -> {
                    if (updateRelation) {
                        // Update task reminder
                        ReminderModel taskReminder = task.getReminder();
                        if (taskReminder != null) {
                            if (!taskReminder.isAttached()) {
                                // Delete reminder if not attached with task
                                taskReminder.delete(false);
                                // Cancel the reminder alarm
                                Logger.d(LOG_TAG, "update(task, relation) - Cancelling task alarm with id " + taskReminder.getId());
                                AlarmScheduler.cancelTaskAlarm(task);
                                task.setReminder(null);
                            } else {
                                // Update / Add task alarm
                                taskReminder.save(false);
                                Logger.d(LOG_TAG, "update(task, relation) - Updating task alarm with id " + taskReminder.getId());
                                boolean taskCompleted = isTaskCompletedToday(task);
                                AlarmScheduler.setTaskAlarm(task, taskCompleted);
                            }
                        }
                    }
                    // Refresh Widgets
                    TodayTaskWidgetProvider.refreshWidgets();
                    // Notify Subscribers
                    postEvent(new TaskUpdated(task), TaskUpdated.class);
                }
        );
    }

    /**
     * This method synchronously updates an existing task model in database. It must not be executed on Main Thread.
     *
     * @param task           Existing task model to update
     * @param updateRelation Whether to update other data related to task
     * @return true if task is updated successfully otherwise false
     */
    public static boolean update(TaskModel task, boolean updateRelation) {
        boolean success = DBQueryExecutor.update(
                DBContract.TasksTable.TABLE_NAME,
                task.getId(),
                task.makeContentValues()
        );
        if (success) {
            if (updateRelation) {
                // Update task reminder
                ReminderModel taskReminder = task.getReminder();
                if (taskReminder != null) {
                    if (!taskReminder.isAttached()) {
                        // Delete reminder if not attached with task
                        taskReminder.delete(false);
                        // Cancel the reminder alarm
                        Logger.d(LOG_TAG, "update(task, relation) - Cancelling task alarm with id " + taskReminder.getId());
                        AlarmScheduler.cancelTaskAlarm(task);
                        task.setReminder(null);
                    } else {
                        taskReminder.save(false);
                        // Update task alarm
                        Logger.d(LOG_TAG, "update(task, relation) - Updating task alarm with id " + taskReminder.getId());
                        boolean taskCompleted = isTaskCompletedToday(task);
                        AlarmScheduler.setTaskAlarm(task, taskCompleted);
                    }
                }
            }
            // Refresh Widgets
            TodayTaskWidgetProvider.refreshWidgets();
            return true;
        }
        return false;
    }

    /**
     * This method asynchronously deletes an existing task model from database.
     *
     * @param task Existing task model to delete
     */
    public static void deleteAsync(TaskModel task) {
        DBQueryExecutor.deleteAsync(
                DBContract.TasksTable.TABLE_NAME,
                task.getId(),
                taskId -> {
                    // Cancel task alarm (if set)
                    ReminderModel reminder = task.getReminder();
                    if (reminder != null) {
                        // Cancel task alarm
                        Logger.d(LOG_TAG, "delete(task) - Cancelling task alarm with id " + reminder.getId());
                        AlarmScheduler.cancelTaskAlarm(task);
                    }
                    // Refresh Widgets
                    TodayTaskWidgetProvider.refreshWidgets();
                    // Notify Subscribers
                    postEvent(new TaskDeleted(task), TaskDeleted.class);
                }
        );
    }

    /**
     * This method synchronously deletes an existing task model from database.
     * It must not be executed on Main Thread.
     *
     * @param task Existing task model to delete
     * @return true if task is deleted successfully otherwise false
     */
    public static boolean delete(TaskModel task) {
        boolean success = DBQueryExecutor.delete(
                DBContract.TasksTable.TABLE_NAME,
                task.getId()
        );
        if (success) {
            // Cancel task alarm (if set)
            ReminderModel reminder = task.getReminder();
            if (reminder != null) {
                // Cancel task alarm
                Logger.d(LOG_TAG, "delete(task) - Cancelling task alarm with id " + reminder.getId());
                AlarmScheduler.cancelTaskAlarm(task);
            }
        }
        // Refresh Widgets
        TodayTaskWidgetProvider.refreshWidgets();
        return success;
    }

    /**
     * This method asynchronously deletes multiple task models from database.
     *
     * @param tasks Existing task models to delete
     */
    public static void deleteAsync(ArrayList<TaskModel> tasks) {
        long[] ids = new long[tasks.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = tasks.get(i).getId();
        DBQueryExecutor.deleteAsync(
                DBContract.TasksTable.TABLE_NAME,
                ids,
                tasksIds -> {
                    // Cancel tasks alarms (if set)
                    for (TaskModel task : tasks) {
                        ReminderModel reminder = task.getReminder();
                        if (reminder != null) {
                            // Cancel task alarm
                            Logger.d(LOG_TAG, "delete(tasks, relation) - Cancelling task alarm with id " + reminder.getId());
                            AlarmScheduler.cancelTaskAlarm(task);
                        }
                    }
                    // Refresh Widgets
                    TodayTaskWidgetProvider.refreshWidgets();
                    // Notify Subscribers
                    postEvent(new TasksDeleted(tasks), TasksDeleted.class);
                }
        );
    }

    /**
     * This method synchronously deletes multiple task models from database.
     * It must not be executed on Main Thread.
     *
     * @param tasks Existing task models to delete
     * @return true if and only if all tasks are deleted successfully otherwise false.
     */
    public static boolean delete(ArrayList<TaskModel> tasks) {
        long[] ids = new long[tasks.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = tasks.get(i).getId();
        boolean success = DBQueryExecutor.delete(
                DBContract.TasksTable.TABLE_NAME,
                ids
        );
        // @todo determine which IDs were successfully deleted

        // Cancel tasks alarms (if set)
        for (TaskModel task : tasks) {
            ReminderModel reminder = task.getReminder();
            if (reminder != null) {
                // Cancel task alarm
                Logger.d(LOG_TAG, "delete(tasks, relation) - Cancelling task alarm with id " + reminder.getId());
                AlarmScheduler.cancelTaskAlarm(task);
            }
        }
        // Refresh Widgets
        TodayTaskWidgetProvider.refreshWidgets();
        return success;
    }

    // @todo Simplify tasks marking method definitions by utilizing the pre-fetched task status info.
    public static void markAsComplete(TaskModel task) {
        TaskStatsManager.getInstance().setTaskDone(task, () -> task.status = TaskStatus.COMPLETED);
    }

    public static void markAsPending(TaskModel task) {
        TaskStatsManager.getInstance().setTaskUndone(task, () -> task.status = TaskStatus.INCOMPLETE);
    }

    /**
     * This method is used to make a TaskModel from a cursor holding task data.
     *
     * @param dataCursor cursor holding task data which must already be in correct position
     * @return task model if cursor has data otherwise null is returned
     */
    public static TaskModel makeFromCursor(Cursor dataCursor) {
        TaskModel task = new TaskModel();
        task.id = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable._ID));
        task.title = dataCursor.getString(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_TITLE));
        task.description = dataCursor.getString(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_DESC));
        task.color = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_COLOR));
        task.repeatSunday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_SUNDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        task.repeatMonday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_MONDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        task.repeatTuesday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_TUESDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        task.repeatWednesday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_WEDNESDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        task.repeatThursday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_THURSDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        task.repeatFriday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_FRIDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        task.repeatSaturday = dataCursor.getInt(dataCursor.getColumnIndex(DBContract.TasksTable.COL_NAME_SATURDAY)) == DBContract.TasksTable.COL_VALUE_TRUE;
        return task;
    }

    /**
     * This method posts events to the subscribers through EventBus.
     *
     * @param event      a task event to post
     * @param eventClass Class type of the event object
     */
    private static void postEvent(TaskEvent event, Class<? extends TaskEvent> eventClass) {
        EventBus eventBus = EventBus.getDefault();
        if (eventBus.hasSubscriberForEvent(eventClass))
            eventBus.post(event);
    }

    /**
     * Default constructor
     */
    public TaskModel() {
        id = -1;
        title = "";
        description = "";
        color = DBContract.BG_COLORS[0];
        repeatSunday = false;
        repeatMonday = false;
        repeatTuesday = false;
        repeatWednesday = false;
        repeatThursday = false;
        repeatFriday = false;
        repeatSaturday = false;
        status = TaskStatus.UNKNOWN;
        reminder = null;
    }

    /**
     * Copy constructor
     *
     * @param task TaskModel object used to create copy
     */
    public TaskModel(TaskModel task) {
        id = -1;
        title = task.getTitle();
        description = task.getDescription();
        color = task.getColor();
        repeatSunday = task.isRepeatSunday();
        repeatMonday = task.isRepeatMonday();
        repeatTuesday = task.isRepeatTuesday();
        repeatWednesday = task.isRepeatWednesday();
        repeatThursday = task.isRepeatThursday();
        repeatFriday = task.isRepeatFriday();
        repeatSaturday = task.isRepeatSaturday();
        status = TaskStatus.UNKNOWN;
        reminder = null;
        if (task.getReminder() != null) reminder = new ReminderModel(task.getReminder());
    }

    protected TaskModel(Parcel in) {
        id = in.readLong();
        title = in.readString();
        description = in.readString();
        color = in.readInt();
        // Reading Boolean Flags from 0 and 1 values
        repeatSunday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        repeatMonday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        repeatTuesday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        repeatWednesday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        repeatThursday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        repeatFriday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        repeatSaturday = in.readInt() == DBContract.TasksTable.COL_VALUE_TRUE;
        // Read related data
        reminder = in.readParcelable(ReminderModel.class.getClassLoader());
    }

    public static final Creator<TaskModel> CREATOR = new Creator<TaskModel>() {
        @Override
        public TaskModel createFromParcel(Parcel in) {
            return new TaskModel(in);
        }

        @Override
        public TaskModel[] newArray(int size) {
            return new TaskModel[size];
        }
    };

    /* Getters and Setters for the state */
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isRepeatSunday() {
        return repeatSunday;
    }

    public void setRepeatSunday(boolean repeat) {
        repeatSunday = repeat;
    }

    public boolean isRepeatMonday() {
        return repeatMonday;
    }

    public void setRepeatMonday(boolean repeat) {
        repeatMonday = repeat;
    }

    public boolean isRepeatTuesday() {
        return repeatTuesday;
    }

    public void setRepeatTuesday(boolean repeat) {
        repeatTuesday = repeat;
    }

    public boolean isRepeatWednesday() {
        return repeatWednesday;
    }

    public void setRepeatWednesday(boolean repeat) {
        repeatWednesday = repeat;
    }

    public boolean isRepeatThursday() {
        return repeatThursday;
    }

    public void setRepeatThursday(boolean repeat) {
        repeatThursday = repeat;
    }

    public boolean isRepeatFriday() {
        return repeatFriday;
    }

    public void setRepeatFriday(boolean repeat) {
        repeatFriday = repeat;
    }

    public boolean isRepeatSaturday() {
        return repeatSaturday;
    }

    public void setRepeatSaturday(boolean repeat) {
        repeatSaturday = repeat;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setReminder(ReminderModel reminder) {
        this.reminder = reminder;
    }

    public ReminderModel getReminder() {
        return reminder;
    }

    /**
     * Method to make ContentValues object from task data.
     *
     * @return ContentValues created from task data.
     */
    private ContentValues makeContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBContract.TasksTable.COL_NAME_TITLE, title);
        cv.put(DBContract.TasksTable.COL_NAME_DESC, description);
        cv.put(DBContract.TasksTable.COL_NAME_COLOR, color);
        cv.put(DBContract.TasksTable.COL_NAME_SUNDAY, getRepeatFlagValue(repeatSunday));
        cv.put(DBContract.TasksTable.COL_NAME_MONDAY, getRepeatFlagValue(repeatMonday));
        cv.put(DBContract.TasksTable.COL_NAME_TUESDAY, getRepeatFlagValue(repeatTuesday));
        cv.put(DBContract.TasksTable.COL_NAME_WEDNESDAY, getRepeatFlagValue(repeatWednesday));
        cv.put(DBContract.TasksTable.COL_NAME_THURSDAY, getRepeatFlagValue(repeatThursday));
        cv.put(DBContract.TasksTable.COL_NAME_FRIDAY, getRepeatFlagValue(repeatFriday));
        cv.put(DBContract.TasksTable.COL_NAME_SATURDAY, getRepeatFlagValue(repeatSaturday));
        return cv;
    }

    private int getRepeatFlagValue(boolean flag) {
        return (flag ? DBContract.TasksTable.COL_VALUE_TRUE : DBContract.TasksTable.COL_VALUE_FALSE);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getId());
        dest.writeString(getTitle());
        dest.writeString(getDescription());
        dest.writeInt(getColor());
        dest.writeInt(getRepeatFlagValue(repeatSunday));
        dest.writeInt(getRepeatFlagValue(repeatMonday));
        dest.writeInt(getRepeatFlagValue(repeatTuesday));
        dest.writeInt(getRepeatFlagValue(repeatWednesday));
        dest.writeInt(getRepeatFlagValue(repeatThursday));
        dest.writeInt(getRepeatFlagValue(repeatFriday));
        dest.writeInt(getRepeatFlagValue(repeatSaturday));
        // Write related data
        dest.writeParcelable(getReminder(), 0);
    }

    /**
     * Converts Parcelable Object to byte array for pending intents
     *
     * @param parcelable parcelable object to convert to bytes
     * @return ordered byte array representation of parcelable
     */
    public static byte[] toByteArray(Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] result = parcel.marshall();
        parcel.recycle();
        return result;
    }

    /**
     * Create Parcelable object from byte array in Pending Intents
     *
     * @param bytes   ordered byte array represents parcelable
     * @param creator CREATOR which can create parcelable type (static field of parcelable)
     * @param <T>     Type of the Parcelable
     * @return returns Parcelable read from ordered byte array
     */
    public static <T> T toParcelable(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        T result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }

    /**
     * This method is used to check whether this task is repeated for given day of the week.
     *
     * @param day must be passed from Calendar.get(Calendar.DAY_OF_WEEK)
     * @return true if task is set for given day or false
     */
    public boolean isRepeatForDay(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return isRepeatMonday();
            case Calendar.TUESDAY:
                return isRepeatTuesday();
            case Calendar.WEDNESDAY:
                return isRepeatWednesday();
            case Calendar.THURSDAY:
                return isRepeatThursday();
            case Calendar.FRIDAY:
                return isRepeatFriday();
            case Calendar.SATURDAY:
                return isRepeatSaturday();
            case Calendar.SUNDAY:
                return isRepeatSunday();
        }
        return false;
    }

    public void setRepeatForDay(int day) {
        switch (day) {
            case Calendar.MONDAY:
                setRepeatMonday(true);
                break;
            case Calendar.TUESDAY:
                setRepeatTuesday(true);
                break;
            case Calendar.WEDNESDAY:
                setRepeatWednesday(true);
                break;
            case Calendar.THURSDAY:
                setRepeatThursday(true);
                break;
            case Calendar.FRIDAY:
                setRepeatFriday(true);
                break;
            case Calendar.SATURDAY:
                setRepeatSaturday(true);
                break;
            case Calendar.SUNDAY:
                setRepeatSunday(true);
        }
    }

    /**
     * This helper method is used to get the no of days in a week for which this task repeats.
     *
     * @return int total no of weekdays for which task repeats
     */
    public int getRepeatCountInWeek() {
        int count = 0;
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            if (isRepeatForDay(i)) count++;
        }
        return count;
    }

    /**
     * This method returns simple human readable string representation of this TaskModel.
     *
     * @return Human readable string representation
     */
    public String getInfoString() {
        StringBuilder sb = new StringBuilder();
        sb.append("###### Task Model ######\n\t");
        sb.append("ID : " + getId() + "\n\t");
        sb.append("Title : " + getTitle() + "\n\t");
        sb.append("Description : " + getDescription() + "\n\t");
        sb.append("Color : " + Integer.toHexString(getColor()) + "\n\t");
        sb.append("Repeat for Days : ");
        if (getRepeatCountInWeek() > 0) {
            sb.append(isRepeatSunday() ? "SUN " : "");
            sb.append(isRepeatMonday() ? "MON " : "");
            sb.append(isRepeatTuesday() ? "TUE " : "");
            sb.append(isRepeatWednesday() ? "WED " : "");
            sb.append(isRepeatThursday() ? "THU " : "");
            sb.append(isRepeatFriday() ? "FRI " : "");
            sb.append(isRepeatSaturday() ? "SAT " : "");
        } else {
            sb.append("None");
        }
        sb.append("\n");
        return sb.toString();
    }


    /**
     * This method inserts new task or updates existing task in database. This method can be
     * invoked synchronously or asynchronously by using async parameter. It can be executed
     * synchronously only on the background thread otherwise it will fail.
     *
     * @param async true to execute asynchronously or false to execute synchronously
     * @return When async is false then return value true indicates success and false indicates failure.
     * When async is true, then return value is always true and it should be ignored.
     */
    public boolean save(boolean async) {
        if (getId() > 0) {
            if (!async) return TaskModel.update(this, true);
            TaskModel.updateAsync(this, true);
        } else {
            if (!async) return TaskModel.insert(this, true);
            TaskModel.insertAsync(this, true);
        }
        return true;
    }


    /**
     * This method deletes existing task from the database. This method can be invoked
     * synchronously or asynchronously by using async parameter. It can be executed synchronously
     * only on the background thread otherwise it will fail.
     *
     * @param async true to execute asynchronously or false to execute synchronously
     * @return always false for non-existing task. For existing tasks following apply -
     * When async is false, a true return value indicates successful deletion and false indicates failure.
     * When async is true, return value is always true and it should be ignored.
     */
    public boolean delete(boolean async) {
        if (getId() > 0) {
            if (!async) return TaskModel.delete(this);
            TaskModel.deleteAsync(this);
            return true;
        }
        return false;
    }
}
