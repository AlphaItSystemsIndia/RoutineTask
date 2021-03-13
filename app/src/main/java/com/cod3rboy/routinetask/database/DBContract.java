package com.cod3rboy.routinetask.database;

import android.provider.BaseColumns;

import com.cod3rboy.routinetask.logging.Logger;

/**
 * Database Contract class to define the database schema
 **/
public final class DBContract {
    // Name of the database
    static final String DATABASE_NAME = "timetableapp.db";
    static final String LOG_TAG = DBContract.class.getSimpleName();
    public static final int[] BG_COLORS = new int[]{
            0xFF202124,
            0xFF5B2B2A,
            0xFF604A1D,
            0xFF635C1F,
            0xFF355823,
            0xFF19504B,
            0xFF2F555D,
            0xFF1F3B5E,
            0xFF42295D,
            0xFF5A2345,
            0xFF442F1B,
            0xFF3C3F43,
    };

    // Prevent instantiation of this class
    private DBContract() {
    }

    /**
     * Static class TasksTable defining schema of tasks table in database.
     * It implements BaseColumns interface to automatically include _ID and _COUNT columns.
     */
    public static final class TasksTable implements BaseColumns {
        // Integer values to indicate true or false
        public static final int COL_VALUE_TRUE = 1;
        public static final int COL_VALUE_FALSE = 0;
        // Name of the table
        public static final String TABLE_NAME = "tasks";
        // Name of the table columns
        public static final String COL_NAME_TITLE = "title";
        public static final String COL_NAME_DESC = "description";
        public static final String COL_NAME_COLOR = "color";
        public static final String COL_NAME_SUNDAY = "sun";
        public static final String COL_NAME_MONDAY = "mon";
        public static final String COL_NAME_TUESDAY = "tue";
        public static final String COL_NAME_WEDNESDAY = "wed";
        public static final String COL_NAME_THURSDAY = "thr";
        public static final String COL_NAME_FRIDAY = "fri";
        public static final String COL_NAME_SATURDAY = "sat";

        public static final int COL_LENGTH_DESC = 128;
        public static final int COL_LENGTH_TITLE = 32;

        // Type of the table columns
        static final String COL_TYPE_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        static final String COL_TYPE_TITLE = "TEXT NOT NULL DEFAULT \"\"";
        static final String COL_TYPE_DESC = "TEXT NOT NULL DEFAULT \"\"";
        static final String COL_TYPE_COLOR = "INTEGER NOT NULL DEFAULT " + DBContract.BG_COLORS[0];
        static final String COL_TYPE_SUNDAY = "int(1) NOT NULL DEFAULT 0";
        static final String COL_TYPE_MONDAY = "int(1) NOT NULL DEFAULT 0";
        static final String COL_TYPE_TUESDAY = "int(1) NOT NULL DEFAULT 0";
        static final String COL_TYPE_WEDNESDAY = "int(1) NOT NULL DEFAULT 0";
        static final String COL_TYPE_THURSDAY = "int(1) NOT NULL DEFAULT 0";
        static final String COL_TYPE_FRIDAY = "int(1) NOT NULL DEFAULT 0";
        static final String COL_TYPE_SATURDAY = "int(1) NOT NULL DEFAULT 0";

        /**
         * Helper method which creates and return a SQL statement to create schema for table.
         *
         * @return SQL query string to create table with schema in this class
         */
        static String getCreationSql() {
            String query = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s,%s %s, %s %s, %s %s);";
            query = String.format(query,
                    TABLE_NAME,
                    _ID, COL_TYPE_ID,
                    COL_NAME_TITLE, COL_TYPE_TITLE,
                    COL_NAME_DESC, COL_TYPE_DESC,
                    COL_NAME_COLOR, COL_TYPE_COLOR,
                    COL_NAME_SUNDAY, COL_TYPE_SUNDAY,
                    COL_NAME_MONDAY, COL_TYPE_MONDAY,
                    COL_NAME_TUESDAY, COL_TYPE_TUESDAY,
                    COL_NAME_WEDNESDAY, COL_TYPE_WEDNESDAY,
                    COL_NAME_THURSDAY, COL_TYPE_THURSDAY,
                    COL_NAME_FRIDAY, COL_TYPE_FRIDAY,
                    COL_NAME_SATURDAY, COL_TYPE_SATURDAY);
            Logger.d(LOG_TAG, TasksTable.class.getSimpleName() + " Creation Query : " + query);
            return query;
        }
    }

    /**
     * Static class RemindersTable defining schema of reminders table in database.
     * It implements BaseColumns interface to automatically include _ID and _COUNT columns.
     */
    public static final class RemindersTable implements BaseColumns {
        // Name of the Table
        public static final String TABLE_NAME = "reminders";

        // Name of table columns
        public static final String COL_NAME_TASK_ID = "task_id";
        public static final String COL_NAME_START_TIME = "start_time"; // 24-Hour Time Format HH:mm
        public static final String COL_NAME_DURATION = "duration"; // in minutes
        public static final String COL_NAME_LAST_MODIFIED = "last_modified"; // in long timestamp

        // Type of the table columns
        static final String COL_TYPE_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        static final String COL_TYPE_TASK_ID = "INTEGER NOT NULL";
        static final String COL_TYPE_START_TIME = "TEXT NOT NULL";
        static final String COL_TYPE_DURATION = "INTEGER NOT NULL DEFAULT 0";
        static final String COL_TYPE_LAST_MODIFIED = "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP";

        /**
         * Helper method which creates and return a SQL statement to create schema for table.
         *
         * @return SQL query string to create table with schema in this class
         */
        static String getCreationSql() {
            String query = "CREATE TABLE %s (%s %s, %s %s, %s %s, %s %s, %s %s, CONSTRAINT fk_tasks_reminders FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE, UNIQUE(%s));";
            query = String.format(query,
                    TABLE_NAME,
                    _ID, COL_TYPE_ID,
                    COL_NAME_TASK_ID, COL_TYPE_TASK_ID,
                    COL_NAME_START_TIME, COL_TYPE_START_TIME,
                    COL_NAME_DURATION, COL_TYPE_DURATION,
                    COL_NAME_LAST_MODIFIED, COL_TYPE_LAST_MODIFIED,
                    COL_NAME_TASK_ID, TasksTable.TABLE_NAME, TasksTable._ID,
                    COL_NAME_TASK_ID);
            Logger.d(LOG_TAG, RemindersTable.class.getSimpleName() + " Creation Query : " + query);
            return query;
        }
    }

    /**
     * Static class RoutineEntryTable defining schema of table in database to store an entry for each completed task per day.
     * It implements BaseColumns interface to automatically include _ID and _COUNT columns.
     */
    public static final class RoutineEntryTable implements BaseColumns {
        // Name of the table
        public static final String TABLE_NAME = "routine_entry";

        // Name of the table columns
        public static final String COL_NAME_TASK_ID = "task_id";
        public static final String COL_NAME_DATE = "date";

        // Type of the table columns
        static final String COL_TYPE_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        static final String COL_TYPE_TASK_ID = "INTEGER NOT NULL";
        static final String COL_TYPE_DATE = "TEXT NOT NULL";

        /**
         * Helper method which creates and return a SQL statement to create schema for table.
         *
         * @return SQL query string to create table with schema in this class
         */
        static String getCreationSql() {
            String query = "CREATE TABLE %s (%s %s, %s %s, %s %s, CONSTRAINT fk_tasks FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE, UNIQUE(%s, %s, %s));";
            query = String.format(query,
                    TABLE_NAME,
                    _ID, COL_TYPE_ID,
                    COL_NAME_TASK_ID, COL_TYPE_TASK_ID,
                    COL_NAME_DATE, COL_TYPE_DATE,
                    COL_NAME_TASK_ID, TasksTable.TABLE_NAME, TasksTable._ID,
                    _ID, COL_NAME_TASK_ID, COL_NAME_DATE);
            Logger.d(LOG_TAG, RoutineEntryTable.class.getSimpleName() + " Creation Query : " + query);
            return query;
        }
    }

    /**
     * Static class RoutineStatsTable defining schema of table in database to store statistics of completed tasks.
     * It implements BaseColumns interface to automatically include _ID and _COUNT columns.
     */
    public static final class RoutineStatsTable implements BaseColumns {
        // Name of the table
        static final String TABLE_NAME = "routine_stats";

        // Name of the table columns
        public static final String COL_NAME_DATE = "date";
        public static final String COL_NAME_COUNT = "count";

        // Type of the table columns
        static final String COL_TYPE_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        static final String COL_TYPE_DATE = "TEXT NOT NULL";
        static final String COL_TYPE_COUNT = "INTEGER NOT NULL";

        /**
         * Helper method which creates and return a SQL statement to create schema for table.
         *
         * @return SQL query string to create table with schema in this class
         */
        static String getCreationSql() {
            String query = "CREATE TABLE %s (%s %s, %s %s, %s %s, UNIQUE(%s));";
            query = String.format(query,
                    TABLE_NAME,
                    _ID, COL_TYPE_ID,
                    COL_NAME_DATE, COL_TYPE_DATE,
                    COL_NAME_COUNT, COL_TYPE_COUNT,
                    COL_NAME_DATE);
            Logger.d(LOG_TAG, RoutineStatsTable.class.getSimpleName() + " Creation Query : " + query);
            return query;
        }
    }

    /**
     * Static class PomodoroStatsTable defining schema of table in database to store statistics of pomodoro time.
     * It implements BaseColumns interface to automatically include _ID and _COUNT columns.
     */
    public static final class PomodoroStatsTable implements BaseColumns {
        // Name of the table
        public static final String TABLE_NAME = "pomodoro_stats";

        // Name of the table columns
        public static final String COL_NAME_SECONDS = "seconds";
        public static final String COL_NAME_DATE = "date";

        // Type of the table columns
        static final String COL_TYPE_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        static final String COL_TYPE_SECONDS = "INTEGER NOT NULL";
        static final String COL_TYPE_DATE = "TEXT NOT NULL";

        /**
         * Helper method which creates and return a SQL statement to create schema for table.
         *
         * @return SQL query string to create table with schema in this class
         */
        static String getCreationSql() {
            String query = "CREATE TABLE %s (%s %s, %s %s, %s %s, UNIQUE(%s));";
            query = String.format(query,
                    TABLE_NAME,
                    _ID, COL_TYPE_ID,
                    COL_NAME_DATE, COL_TYPE_DATE,
                    COL_NAME_SECONDS, COL_TYPE_SECONDS,
                    COL_NAME_DATE);
            Logger.d(LOG_TAG, PomodoroStatsTable.class.getSimpleName() + " Creation Query : " + query);
            return query;
        }
    }
}
