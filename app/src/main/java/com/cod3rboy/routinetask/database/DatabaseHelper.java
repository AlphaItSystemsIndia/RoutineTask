package com.cod3rboy.routinetask.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cod3rboy.routinetask.utilities.Time;

/**
 * SQLiteOpenHelper class to create/upgrade SQLite database.
 * Only Single instance of this class exists throughout the application.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Database version code
    private final static int DATABASE_VERSION = 3;
    // Singleton instance of the database helper class
    private static DatabaseHelper singleton = null;

    /**
     * Private constructor to create instance of DatabaseHelper class
     *
     * @param context Must be Context Object of Application Context (getApplicationContext())
     */
    private DatabaseHelper(Context context) {
        // Chain constructor call up the hierarchy
        super(context, DBContract.DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Synchronized factory method to obtain a singleton instance of this class
     *
     * @param c Must be Context Object of Application Context.(getApplicationContext())
     * @return singleton object of DatabaseHelper class
     */
    public synchronized static DatabaseHelper getInstance(Context c) {
        if (singleton == null) {
            // Instantiate singleton if not already created
            singleton = new DatabaseHelper(c);
        }
        return singleton;
    }

    /**
     * This method gets called only if database does not exists on the device.
     * It is called only once when using either getReadableDatabase or getWritableDatabase on SQLiteDatabase
     * object and database does not exists at that time.
     *
     * @param db SqLiteDatabase object passed by the caller
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create database schemas
        db.execSQL(DBContract.TasksTable.getCreationSql());
        db.execSQL(DBContract.RemindersTable.getCreationSql());
        db.execSQL(DBContract.RoutineEntryTable.getCreationSql());
        db.execSQL(DBContract.RoutineStatsTable.getCreationSql());
        db.execSQL(DBContract.PomodoroStatsTable.getCreationSql());
    }

    /**
     * This method gets called every time database is opened.
     * Here we are turning on foreign key constraints on database.
     *
     * @param db SQLiteDatabase object
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * This method gets called only if database already exists on the device but database version mismatched (mostly during an app upgrade).
     * It is called only once when using either getReadableDatabase or getWritableDatabase on SQLiteDatabase
     * object and database version mismatch with the database version of database exists at that time.
     *
     * @param db         SqLiteDatabase object passed by the caller
     * @param oldVersion database version of existing database
     * @param newVersion database version of new database
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do something to migrate to new database
        // Create Statistics Tables for databases with version code 1
        // Change structure of Time Table table with preserving existing data
        String oldTasksTableName = "timetable";
        String oldColumnTime = "time";
        Cursor c = db.rawQuery("SELECT * FROM " + oldTasksTableName + " WHERE 1;", null);
        ContentValues[] tasksValues = new ContentValues[c.getCount()];
        ContentValues[] reminderValues = new ContentValues[c.getCount()];
        int i = 0;
        while (c.moveToNext() && i < tasksValues.length) {
            tasksValues[i] = new ContentValues();
            reminderValues[i] = new ContentValues();
            reminderValues[i].put(DBContract.RemindersTable.COL_NAME_TASK_ID,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable._ID)));
            reminderValues[i].put(DBContract.RemindersTable.COL_NAME_START_TIME,
                    Time.fromISO8601DateFormat(c.getString(c.getColumnIndex(oldColumnTime))).to24TimeFormat());
            tasksValues[i].put(DBContract.TasksTable._ID,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable._ID)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_TITLE,
                    c.getString(c.getColumnIndex(DBContract.TasksTable.COL_NAME_TITLE)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_DESC,
                    c.getString(c.getColumnIndex(DBContract.TasksTable.COL_NAME_DESC)));
            if (oldVersion < 2) {
                tasksValues[i].put(DBContract.TasksTable.COL_NAME_COLOR, DBContract.BG_COLORS[0]);
            } else {
                tasksValues[i].put(DBContract.TasksTable.COL_NAME_COLOR,
                        // Version 2 database uses colors with translucent opacity but version 3 uses opaque colors
                        0xFF000000 | c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_COLOR)));
            }
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_SUNDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_SUNDAY)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_MONDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_MONDAY)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_TUESDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_TUESDAY)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_WEDNESDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_WEDNESDAY)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_THURSDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_THURSDAY)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_FRIDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_FRIDAY)));
            tasksValues[i].put(DBContract.TasksTable.COL_NAME_SATURDAY,
                    c.getInt(c.getColumnIndex(DBContract.TasksTable.COL_NAME_SATURDAY)));
            i++;
        }
        c.close();
        // Database version 2 uses old table name for tasks table and it is referenced by
        // routine_entry table using foreign key constraint. So we have to first get here the
        // content values of routine_entry table and then recreate the table after creating
        // new tasks table and reload the content values.
        ContentValues[] entryValues = null;
        if (oldVersion == 2) {
            c = db.rawQuery("SELECT * FROM " + DBContract.RoutineEntryTable.TABLE_NAME + " WHERE 1;", null);
            // Load content values of routine_entry table
            entryValues = new ContentValues[c.getCount()];
            i = 0;
            while (c.moveToNext() && i < entryValues.length) {
                ContentValues entryValue = new ContentValues();
                entryValue.put(DBContract.RoutineEntryTable._ID, c.getInt(c.getColumnIndex(DBContract.RoutineEntryTable._ID)));
                entryValue.put(DBContract.RoutineEntryTable.COL_NAME_TASK_ID,
                        c.getInt(c.getColumnIndex(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)));
                entryValue.put(DBContract.RoutineEntryTable.COL_NAME_DATE,
                        c.getString(c.getColumnIndex(DBContract.RoutineEntryTable.COL_NAME_DATE)));
                entryValues[i] = entryValue;
                i++;
            }
            c.close();
            // Drop routine_entry table if exists
            db.execSQL("DROP TABLE IF EXISTS " + DBContract.RoutineEntryTable.TABLE_NAME + ";");
        }
        db.execSQL("DROP TABLE IF EXISTS " + oldTasksTableName + ";");
        db.execSQL(DBContract.TasksTable.getCreationSql());

        for (ContentValues cv : tasksValues)
            db.insert(DBContract.TasksTable.TABLE_NAME, null, cv);
        db.execSQL(DBContract.RemindersTable.getCreationSql());
        for (ContentValues cv : reminderValues) {
            db.insert(DBContract.RemindersTable.TABLE_NAME, null, cv);
        }
        if(oldVersion == 2){
            // Recreate routine_entry table and reload data into it
            db.execSQL(DBContract.RoutineEntryTable.getCreationSql());
            for(ContentValues cv : entryValues)
                db.insert(DBContract.RoutineEntryTable.TABLE_NAME, null, cv);
        }
        // For database below version 2, create statistics tables too.
        if (oldVersion < 2) {
            // Create Statistics Tables
            db.execSQL(DBContract.RoutineEntryTable.getCreationSql());
            db.execSQL(DBContract.RoutineStatsTable.getCreationSql());
            db.execSQL(DBContract.PomodoroStatsTable.getCreationSql());
        }
    }
}
