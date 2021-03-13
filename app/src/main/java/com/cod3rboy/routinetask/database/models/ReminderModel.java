package com.cod3rboy.routinetask.database.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.DBQueryExecutor;
import com.cod3rboy.routinetask.events.ReminderCreated;
import com.cod3rboy.routinetask.events.ReminderDeleted;
import com.cod3rboy.routinetask.events.ReminderEvent;
import com.cod3rboy.routinetask.events.ReminderLoaded;
import com.cod3rboy.routinetask.events.ReminderUpdated;
import com.cod3rboy.routinetask.utilities.Time;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;

/**
 * This model class represents the reminder set for a task. It holds reminder data and provides methods to
 * perform CRUD operations.
 */
public class ReminderModel implements Parcelable {
    /**
     * This state enum is used to mark reminder whether it is attached to a task or not.
     * This information is used to update reminder model when changes made to the task also affects
     * the reminder e.g. it may be possible that reminder is detached from task.
     */
    public enum ReminderState {
        ATTACHED, DETACHED
    }

    private long id;
    private long taskId;
    private Time startTime;
    private long durationInMinutes;
    private long lastModified;
    private ReminderState attachment;

    /**
     * This method generates a random Reminder Model for a given task id.
     *
     * @param taskId id of TaskModel for which reminder will be created
     * @return a random reminder model
     */
    public static ReminderModel makeRandomReminder(long taskId) {
        Random random = new Random(System.currentTimeMillis());
        return new ReminderModel(taskId, Time.getRandomTime(), random.nextInt(100));
    }

    /**
     * This method asynchronously fetches reminder model for a given task id. When reminder is loaded
     * it notifies all registered subscribers.
     *
     * @param taskId id of task for which reminder will be loaded
     */
    public static void fetchForTask(long taskId) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("SELECT * FROM ")
                .append(DBContract.RemindersTable.TABLE_NAME)
                .append(" WHERE ")
                .append(DBContract.RemindersTable.COL_NAME_TASK_ID)
                .append(" = ?;");
        DBQueryExecutor.queryAsync(
                cmdBuilder.toString(),
                new String[]{String.valueOf(taskId)},
                result -> {
                    ReminderModel reminder = null;
                    if (result.moveToFirst()) {
                        reminder = makeFromCursor(result);
                    }
                    result.close();
                    if (reminder != null) // When reminder is not set for a task, it is null.
                        // Notify Subscribers
                        postEvent(new ReminderLoaded(reminder), ReminderLoaded.class);
                }
        );
    }

    /**
     * This method synchronously retrieves reminder model for a given task id. This method should
     * not invoked by Main Thread.
     *
     * @param taskId id of task for which reminder will be retrieved
     * @return Reminder model for given task if reminder is set for that task otherwise null is returned
     */
    public static ReminderModel getForTask(long taskId) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("SELECT * FROM ")
                .append(DBContract.RemindersTable.TABLE_NAME)
                .append(" WHERE ")
                .append(DBContract.RemindersTable.COL_NAME_TASK_ID)
                .append(" = ?;");
        Cursor dataCursor = DBQueryExecutor.query(cmdBuilder.toString(), new String[]{String.valueOf(taskId)});
        ReminderModel reminder = null;
        if (dataCursor.moveToFirst()) {
            reminder = makeFromCursor(dataCursor);
        }
        dataCursor.close();
        return reminder;
    }

    /**
     * This methods inserts a new reminder model into database asynchronously and set rowId on
     * reminder object.
     *
     * @param reminder New reminder model to insert
     */
    public static void insertAsync(ReminderModel reminder) {
        // Set last modified time of reminder to now
        reminder.lastModified = System.currentTimeMillis();
        DBQueryExecutor.insertAsync(
                DBContract.RemindersTable.TABLE_NAME,
                null,
                reminder.makeContentValues(),
                reminderId -> {
                    reminder.id = reminderId;
                    // Notify Subscribers
                    postEvent(new ReminderCreated(reminder), ReminderCreated.class);
                }
        );
    }

    /**
     * This methods inserts a new reminder model into database synchronously and set the rowId on
     * reminder object. It must not be executed on Main Thread.
     *
     * @param reminder New reminder model to insert
     * @return true only if reminder was inserted otherwise false
     */
    public static boolean insert(ReminderModel reminder) {
        // Set last modified time of reminder to now
        reminder.lastModified = System.currentTimeMillis();
        long rowId = DBQueryExecutor.insert(
                DBContract.RemindersTable.TABLE_NAME,
                null,
                reminder.makeContentValues()
        );
        if (rowId > 0) {
            reminder.id = rowId;
            return true;
        }
        return false;
    }


    /**
     * This method updates an existing reminder model in database asynchronously.
     *
     * @param reminder Existing reminder model to update
     */
    public static void updateAsync(ReminderModel reminder) {
        // Update the last modified time of reminder to now
        reminder.lastModified = System.currentTimeMillis();
        DBQueryExecutor.updateAsync(
                DBContract.RemindersTable.TABLE_NAME,
                reminder.getId(),
                reminder.makeContentValues(),
                reminderId -> {
                    // Notify Subscribers
                    postEvent(new ReminderUpdated(reminder), ReminderUpdated.class);
                }
        );
    }

    /**
     * This method updates an existing reminder model in database synchronously.
     * It must not be executed on Main Thread.
     *
     * @param reminder Existing reminder model to update
     * @return true only if reminder was updated otherwise false
     */
    public static boolean update(ReminderModel reminder) {
        // Update the last modified time of reminder to now
        reminder.lastModified = System.currentTimeMillis();
        boolean updated = DBQueryExecutor.update(
                DBContract.RemindersTable.TABLE_NAME,
                reminder.getId(),
                reminder.makeContentValues()
        );
        return updated;
    }

    /**
     * This method deletes an existing reminder model from database asynchronously.
     *
     * @param reminder Existing reminder model to delete
     */
    public static void deleteAsync(ReminderModel reminder) {
        DBQueryExecutor.deleteAsync(
                DBContract.RemindersTable.TABLE_NAME,
                reminder.getId(),
                reminderId -> {
                    // Notify Subscribers
                    postEvent(new ReminderDeleted(reminder), ReminderDeleted.class);
                }
        );
    }

    /**
     * This method deletes an existing reminder model from database synchronously.
     * It must not be executed on Main Thread.
     *
     * @param reminder Existing reminder model to delete
     * @return true only if reminder was deleted otherwise false
     */
    public static boolean delete(ReminderModel reminder) {
        boolean deleted = DBQueryExecutor.delete(
                DBContract.RemindersTable.TABLE_NAME,
                reminder.getId()
        );
        return deleted;
    }

    /**
     * This method is used to make a ReminderModel from a cursor holding reminder data.
     *
     * @param dataCursor cursor holding reminder data which must already be in correct position
     * @return reminder model if cursor has data otherwise null is returned
     */
    private static ReminderModel makeFromCursor(Cursor dataCursor) {
        ReminderModel model = new ReminderModel(
                dataCursor.getLong(dataCursor.getColumnIndex(DBContract.RemindersTable.COL_NAME_TASK_ID)),
                Time.from24TimeFormat(dataCursor.getString(dataCursor.getColumnIndex(DBContract.RemindersTable.COL_NAME_START_TIME))),
                dataCursor.getLong(dataCursor.getColumnIndex(DBContract.RemindersTable.COL_NAME_DURATION))
        );
        model.id = dataCursor.getLong(dataCursor.getColumnIndex(DBContract.RemindersTable._ID));
        model.lastModified = dataCursor.getLong(dataCursor.getColumnIndex(DBContract.RemindersTable.COL_NAME_LAST_MODIFIED));
        return model;
    }

    /**
     * This method posts events to the subscribers through EventBus.
     *
     * @param event      a reminder event to post
     * @param eventClass Class type of the event object
     */
    private static void postEvent(ReminderEvent event, Class<? extends ReminderEvent> eventClass) {
        EventBus eventBus = EventBus.getDefault();
        if (eventBus.hasSubscriberForEvent(eventClass))
            eventBus.post(event);
    }

    public ReminderModel(Time startTime, long durationInMinutes) {
        this(-1, startTime, durationInMinutes);
    }

    public ReminderModel(long taskId, Time startTime, long durationInMinutes) {
        this.id = -1;
        this.setTaskId(taskId);
        this.setStartTime(startTime);
        this.setDurationInMinutes(durationInMinutes);
        this.lastModified = System.currentTimeMillis();
    }

    public ReminderModel(ReminderModel reminder) {
        this.id = -1;
        this.setTaskId(-1);
        this.setStartTime(new Time(reminder.getStartTime().getHours(), reminder.getStartTime().getMinutes()));
        this.setDurationInMinutes(reminder.getDurationInMinutes());
        this.lastModified = System.currentTimeMillis();
    }

    protected ReminderModel(Parcel in) {
        id = in.readLong();
        this.setTaskId(in.readLong());
        this.setStartTime(Time.from24TimeFormat(in.readString()));
        this.setDurationInMinutes(in.readLong());
        this.attachment = ReminderState.valueOf(in.readString());
        this.lastModified = in.readLong();
    }

    public static final Creator<ReminderModel> CREATOR = new Creator<ReminderModel>() {
        @Override
        public ReminderModel createFromParcel(Parcel in) {
            return new ReminderModel(in);
        }

        @Override
        public ReminderModel[] newArray(int size) {
            return new ReminderModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(taskId);
        dest.writeString(startTime.to24TimeFormat());
        dest.writeLong(durationInMinutes);
        dest.writeString(attachment.name());
        dest.writeLong(lastModified);
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
        // Attach if task id is valid otherwise detach
        if (this.taskId > 0) attach();
        else detach();
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public void setDurationInMinutes(long durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public long getTaskId() {
        return taskId;
    }

    public long getId() {
        return id;
    }

    public Time getStartTime() {
        return startTime;
    }

    public long getDurationInMinutes() {
        return durationInMinutes;
    }

    public long getLastModified() {
        return lastModified;
    }

    /**
     * This method attaches reminder to a task.
     */
    public void attach() {
        attachment = ReminderState.ATTACHED;
    }

    /**
     * This method detaches reminder from a task.
     */
    public void detach() {
        attachment = ReminderState.DETACHED;
    }

    /**
     * Whether reminder is attached to a task or not.
     *
     * @return true if reminder is attached with a task otherwise false
     */
    public boolean isAttached() {
        return attachment == ReminderState.ATTACHED;
    }

    /**
     * This method inserts new reminder or updates existing reminder in database. This method can be
     * invoked synchronously or asynchronously by using async parameter. It can be executed
     * synchronously only on the background thread otherwise it will fail.
     *
     * @param async true to execute asynchronously or false to execute synchronously
     * @return When async is false then return value true indicates success and false indicates failure.
     * When async is true, then return value is always true and it should be ignored.
     */
    public boolean save(boolean async) {
        if (getId() > 0) {
            if (!async) return ReminderModel.update(this);
            ReminderModel.updateAsync(this);
        } else {
            if (!async) return ReminderModel.insert(this);
            ReminderModel.insertAsync(this);
        }
        return true;
    }

    /**
     * This method deletes existing reminder from the database. This method can be invoked
     * synchronously or asynchronously by using async parameter. It can be executed synchronously
     * only on the background thread otherwise it will fail.
     *
     * @param async true to execute asynchronously or false to execute synchronously
     * @return always false for non-existing reminder. For existing reminder following apply -
     * When async is false, a true return value indicates successful deletion and false indicates failure.
     * When async is true, return value is always true and it should be ignored.
     */
    public boolean delete(boolean async) {
        if (getId() > 0) {
            if (!async) return ReminderModel.delete(this);
            ReminderModel.deleteAsync(this);
            return true;
        }
        return false;
    }

    /**
     * This method creates content values from reminder data while creating or updating model.
     *
     * @return ContentValues holding reminder data
     */
    private ContentValues makeContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBContract.RemindersTable.COL_NAME_TASK_ID, getTaskId());
        cv.put(DBContract.RemindersTable.COL_NAME_START_TIME, getStartTime().to24TimeFormat());
        cv.put(DBContract.RemindersTable.COL_NAME_DURATION, getDurationInMinutes());
        cv.put(DBContract.RemindersTable.COL_NAME_LAST_MODIFIED, getLastModified());
        return cv;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReminderModel)) return false;
        ReminderModel reminder = (ReminderModel) obj;
        return reminder.getDurationInMinutes() == this.getDurationInMinutes()
                && reminder.getStartTime().getMinutes() == this.getStartTime().getMinutes()
                && reminder.getStartTime().getHours() == this.getStartTime().getHours();
    }

    /**
     * Creates a copy of existing {@link ReminderModel} object.
     *
     * @param reminder {@link ReminderModel} object whose copy will be created
     * @return copy of reminder object or null if reminder is null
     */
    public static ReminderModel copy(@Nullable ReminderModel reminder) {
        if(reminder == null) return null;
        return new ReminderModel(reminder);
    }
}
