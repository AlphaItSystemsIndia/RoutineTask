package com.cod3rboy.routinetask.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;

import com.cod3rboy.routinetask.TaskApplication;
import com.cod3rboy.routinetask.logging.Logger;

import java.util.ArrayList;
import java.util.Locale;

import needle.Needle;


/**
 * This class is used to execute database queries asynchronously to insert, update and delete tasks and
 * generates corresponding task events.
 * Class is instantiated to execute only one type of query by setting a mode in the constructor.
 */
public final class DBQueryExecutor {
    private static final String LOG_TAG = DBQueryExecutor.class.getSimpleName();
    private static final String TASK_TYPE_INSERT = "task_type_insert";
    private static final String TASK_TYPE_UPDATE = "task_type_update";
    private static final String TASK_TYPE_DELETE = "task_type_delete";
    private static final String TASK_TYPE_QUERY = "task_type_query";
    private static final String ERROR_SYNC_ON_MAIN_THREAD = "Synchronous database query should not be executed in main thread of application.";

    public interface OnInsertCallback {
        void recordInserted(long rowId);
    }

    public interface OnBulkInsertCallback {
        void recordsInserted(long[] rowIds);
    }

    public interface OnDeleteCallback {
        void recordDeleted(long rowIdDeleted);
    }

    public interface OnBulkDeleteCallback {
        void recordsDeleted(long[] rowIdsDeleted);
    }

    public interface OnUpdateCallback {
        void recordUpdated(long rowId);
    }

    public interface OnQueryCallback {
        void queryCompleted(Cursor data);
    }

    // Do not allow instance creation
    private DBQueryExecutor() {
    }

    /**
     * This Method executes insert query asynchronously to insert a record in database.
     *
     * @param tableName      name of the table in which task will be inserted
     * @param nullColumnHack SQLite null column hack
     * @param cv             an instance of ContentValues containing column-value pairs to be inserted in database
     * @param callback       callback to execute after insert is complete
     */
    public static void insertAsync(final String tableName, final String nullColumnHack, final ContentValues cv, OnInsertCallback callback) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_INSERT).serially().execute(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            long newRowId;
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                newRowId = db.insert(tableName, nullColumnHack, cv);
            }
            if (newRowId > 0) {
                Logger.d(LOG_TAG, String.format(Locale.getDefault(), "New record is inserted in %s with id %d", tableName, newRowId));
                if (callback != null) callback.recordInserted(newRowId);
            }
        });
    }

    /**
     * This Method executes insert query synchronously to insert a record in database. It should always
     * be executed from background thread.
     *
     * @param tableName      name of the table in which task will be inserted
     * @param nullColumnHack SQLite null column hack
     * @param cv             an instance of ContentValues containing column-value pairs to be inserted in database
     * @return rowId of record inserted into database or -1 if insertion failed
     */
    public static long insert(final String tableName, final String nullColumnHack, final ContentValues cv) {
        // Verify that invoking thread is not Main Thread
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException(ERROR_SYNC_ON_MAIN_THREAD);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
        long newRowId;
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            newRowId = db.insert(tableName, nullColumnHack, cv);
        }
        if (newRowId > 0) {
            Logger.d(LOG_TAG, String.format(Locale.getDefault(), "New record is inserted in %s with id %d", tableName, newRowId));
            return newRowId;
        }
        return -1;
    }

    /**
     * This Method executes insert query asynchronously to insert new records in bulk in database.
     *
     * @param tableName      name of the table in which task will be inserted
     * @param nullColumnHack SQLite null column hack
     * @param contentValues  an array of ContentValues containing column-value pairs to be inserted in database
     * @param callback       callback to execute after bulk insert is complete
     */
    public static void insertAsync(final String tableName, final String nullColumnHack, final ContentValues[] contentValues, OnBulkInsertCallback callback) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_INSERT).serially().execute(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            ArrayList<Long> rowIds = new ArrayList<>();
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                for (ContentValues cv : contentValues) {
                    long newRowId = db.insert(tableName, nullColumnHack, cv);
                    if (newRowId > 0) {
                        Logger.d(LOG_TAG, String.format(Locale.getDefault(), "New record is inserted in %s with id %d", tableName, newRowId));
                        rowIds.add(newRowId);
                    } else {
                        rowIds.add((long) -1);
                    }
                }
            }
            if (callback != null) {
                long[] newRowIds = new long[rowIds.size()];
                for (int i = 0; i < newRowIds.length; i++) newRowIds[i] = rowIds.get(i);
                callback.recordsInserted(newRowIds);
            }
        });
    }

    /**
     * This Method executes insert query synchronously to insert new records in bulk in database. It
     * should always be executed in a background thread.
     *
     * @param tableName      name of the table in which task will be inserted
     * @param nullColumnHack SQLite null column hack
     * @param contentValues  an array of ContentValues containing column-value pairs to be inserted in database
     * @return array of rowIds of records inserted into database. IDs for failed insertions are set to -1 in array.
     */
    public static long[] insert(final String tableName, final String nullColumnHack, final ContentValues[] contentValues) {
        // Verify that invoking thread is not Main Thread
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException(ERROR_SYNC_ON_MAIN_THREAD);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
        ArrayList<Long> rowIds = new ArrayList<>();
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (ContentValues cv : contentValues) {
                long newRowId = db.insert(tableName, nullColumnHack, cv);
                if (newRowId > 0) {
                    Logger.d(LOG_TAG, String.format(Locale.getDefault(), "New record is inserted in %s with id %d", tableName, newRowId));
                    rowIds.add(newRowId);
                } else {
                    rowIds.add((long) -1);
                }
            }
        }
        long[] newRowIds = new long[rowIds.size()];
        for (int i = 0; i < newRowIds.length; i++) newRowIds[i] = rowIds.get(i);
        return newRowIds;
    }

    /**
     * This Method executes delete query asynchronously to delete an existing record from database.
     *
     * @param tableName name of the table from which task will be deleted
     * @param id        id of the record to be deleted
     * @param callback  callback to execute after record is deleted
     */
    public static void deleteAsync(final String tableName, final long id, OnDeleteCallback callback) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_DELETE).serially().execute(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            int rowsAffected;
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                rowsAffected = db.delete(tableName, String.format("%s = ?", DBContract.TasksTable._ID), new String[]{String.valueOf(id)});
            }
            if (rowsAffected > 0) {
                Logger.d(LOG_TAG, String.format(Locale.getDefault(), "Record with id %d is deleted from %s", id, tableName));
                if (callback != null) callback.recordDeleted(id);
            }
        });
    }

    /**
     * This Method executes delete query synchronously to delete an existing record from database. It
     * should always be executed in a background thread.
     *
     * @param tableName name of the table from which task will be deleted
     * @param id        id of the record to be deleted
     * @return true if task was deleted or false if deletion failed
     */
    public static boolean delete(final String tableName, final long id) {
        // Verify that invoking thread is not Main Thread
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException(ERROR_SYNC_ON_MAIN_THREAD);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
        int rowsAffected;
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            rowsAffected = db.delete(tableName, String.format("%s = ?", DBContract.TasksTable._ID), new String[]{String.valueOf(id)});
        }
        if (rowsAffected > 0) {
            Logger.d(LOG_TAG, String.format(Locale.getDefault(), "Record with id %d is deleted from %s", id, tableName));
            return true;
        }
        return false;
    }

    /**
     * This Method executes delete query synchronously to delete existing records in bulk from database.
     * It should always be executed in a background thread.
     *
     * @param tableName name of the table from which task will be deleted
     * @param ids       array of IDs of the records to be deleted
     * @return true if all tasks were deleted and false if tasks deletion failed fully or partially.
     */
    public static boolean delete(final String tableName, final long[] ids) {
        // Verify that invoking thread is not Main Thread
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException(ERROR_SYNC_ON_MAIN_THREAD);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
        ArrayList<Long> deletedIds = new ArrayList<>();
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (long id : ids) {
                int rowsAffected = db.delete(tableName, String.format("%s = ?", DBContract.TasksTable._ID), new String[]{String.valueOf(id)});
                if (rowsAffected > 0) {
                    Logger.d(LOG_TAG, String.format(Locale.getDefault(), "Record with id %d is deleted from %s", id, tableName));
                    deletedIds.add(id);
                }
            }
        }
        return ids.length == deletedIds.size();
    }

    /**
     * This Method executes delete query asynchronously to delete existing records in bulk from database.
     *
     * @param tableName name of the table from which task will be deleted
     * @param ids       array of IDs of the records to be deleted
     * @param callback  callback to executes after records are deleted
     */
    public static void deleteAsync(final String tableName, final long[] ids, OnBulkDeleteCallback callback) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_DELETE).serially().execute(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            ArrayList<Long> deletedIds = new ArrayList<>();
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                for (long id : ids) {
                    int rowsAffected = db.delete(tableName, String.format("%s = ?", DBContract.TasksTable._ID), new String[]{String.valueOf(id)});
                    if (rowsAffected > 0) {
                        Logger.d(LOG_TAG, String.format(Locale.getDefault(), "Record with id %d is deleted from %s", id, tableName));
                        deletedIds.add(id);
                    }
                }
            }
            if (!deletedIds.isEmpty() && callback != null) {
                long[] idsDeleted = new long[deletedIds.size()];
                for (int i = 0; i < idsDeleted.length; i++) idsDeleted[i] = deletedIds.get(i);
                callback.recordsDeleted(idsDeleted);
            }
        });
    }

    /**
     * This Method executes update query synchronously to update an existing record in database. It
     * should always be executed from background thread.
     *
     * @param tableName name of the table in which task will be updated
     * @param id        id of the record to be updated
     * @param cv        an instance of ContentValues containing updated column-value pairs
     * @return true if record was updated and false if update was failed
     */
    public static boolean update(final String tableName, final long id, final ContentValues cv) {
        // Verify that invoking thread is not Main Thread
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException(ERROR_SYNC_ON_MAIN_THREAD);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
        int rowsAffected;
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            rowsAffected = db.update(tableName, cv, String.format("%s = ?", DBContract.TasksTable._ID), new String[]{String.valueOf(id)});
        }
        if (rowsAffected > 0) {
            Logger.d(LOG_TAG, String.format(Locale.getDefault(), "Record with id %d in %s is updated", id, tableName));
            return true;
        }
        return false;
    }

    /**
     * This Method executes update query asynchronously to update an existing record in database.
     *
     * @param tableName name of the table in which task will be updated
     * @param id        id of the record to be updated
     * @param cv        an instance of ContentValues containing updated column-value pairs
     * @param callback  callback to execute after a record is updated
     */
    public static void updateAsync(final String tableName, final long id, final ContentValues cv, OnUpdateCallback callback) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_UPDATE).serially().execute(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            int rowsAffected;
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                rowsAffected = db.update(tableName, cv, String.format("%s = ?", DBContract.TasksTable._ID), new String[]{String.valueOf(id)});
            }
            if (rowsAffected > 0) {
                Logger.d(LOG_TAG, String.format(Locale.getDefault(), "Record with id %d in %s is updated", id, tableName));
                if (callback != null) callback.recordUpdated(id);
            }
        });
    }

    /**
     * This method is used to asynchronously query records from database. In order to get the query
     * results, OnQueryCallback is specified in argument. Cursor passed in the callback argument is
     * not needed to be closed.
     *
     * @param sql           SQL query string
     * @param selectionArgs selection arguments array for SQL query string
     * @param callback      Callback to execute after query is completed and to pass on results
     */
    public static void queryAsync(String sql, String[] selectionArgs, OnQueryCallback callback) {
        Needle.onBackgroundThread().withTaskType(TASK_TYPE_QUERY).serially().execute(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
            Cursor result;
            synchronized (dbHelper) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                result = db.rawQuery(sql, selectionArgs);
            }
            if (callback != null) callback.queryCompleted(result);
            if (!result.isClosed()) result.close();
        });
    }

    /**
     * This method is used to synchronously query records from database. This method should always
     * be executed in a background thread.
     *
     * @param sql           SQL query string
     * @param selectionArgs selection arguments array for SQL query string
     * @return Cursor to the results of the query
     */
    public static Cursor query(String sql, String[] selectionArgs) {
        // Verify that invoking thread is not Main Thread
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException(ERROR_SYNC_ON_MAIN_THREAD);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
        Cursor result;
        synchronized (dbHelper) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            result = db.rawQuery(sql, selectionArgs);
        }
        return result;
    }
}