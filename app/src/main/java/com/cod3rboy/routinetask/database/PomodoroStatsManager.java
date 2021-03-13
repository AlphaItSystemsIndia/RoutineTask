package com.cod3rboy.routinetask.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.events.RandomStatsGenerated;
import com.cod3rboy.routinetask.events.StatisticsReset;
import com.cod3rboy.routinetask.logging.Logger;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

/**
 * PomodoroStatsManager class provides functionality related to collection and retrieval of
 * Pomodoro Statistics Info.
 */
public class PomodoroStatsManager {
    private Context mContext; // Context required for performing database operations
    private static final String LOG_TAG = PomodoroStatsManager.class.getSimpleName();
    /**
     * Constructor
     * @param context Any context object
     */
    public PomodoroStatsManager(Context context){
        this.mContext = context;
    }

    /**
     * This method is used to either create a new record for first pomodoro time or it appends the new
     * pomodoro time to pomodoro time in old record. It maintains just one record in database table to
     * keep track of total time spent in pomodoro by user.
     * @param millis Time in milliseconds spent by user in new pomodoro
     */
    public void addPomodoroTimeToStats(long millis){
        // perform operation on separate thread
        (new Thread(){
            @Override
            public void run() {
                // Convert milliseconds to seconds
                int seconds = (int) Math.floor(millis / 1000f);
                String todayDate = Utilities.getTodayDateString();
                // Get reference to database
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(mContext);
                synchronized (dbHelper){
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    // Query any existing record
                    Cursor c = db.query(DBContract.PomodoroStatsTable.TABLE_NAME,
                            new String[]{DBContract.PomodoroStatsTable._ID, DBContract.PomodoroStatsTable.COL_NAME_SECONDS},
                            String.format("%s = ?", DBContract.PomodoroStatsTable.COL_NAME_DATE),
                            new String[]{todayDate},
                            null,
                            null,
                            null);
                    int count = c.getCount();
                    if(count == 0){ // Insert the first pomodoro time record
                        c.close();
                        ContentValues cv = new ContentValues();
                        cv.put(DBContract.PomodoroStatsTable.COL_NAME_SECONDS, seconds);
                        cv.put(DBContract.PomodoroStatsTable.COL_NAME_DATE, todayDate);
                        long id = db.insert(DBContract.PomodoroStatsTable.TABLE_NAME,null, cv);
                        if(id > 0){
                            Logger.d(LOG_TAG, String.format("Added %d pomodoro seconds to pomodoro statistics.", seconds));
                        }
                    }else{ // Update existing pomodoro time record
                        c.moveToFirst();
                        long id = c.getInt(0);
                        int lastSeconds = c.getInt(1);
                        c.close();
                        ContentValues cv = new ContentValues();
                        cv.put(DBContract.PomodoroStatsTable.COL_NAME_SECONDS, lastSeconds + seconds);
                        int rowsAffected = db.update(DBContract.PomodoroStatsTable.TABLE_NAME, cv,
                                String.format("%s = ?", DBContract.PomodoroStatsTable._ID),
                                new String[]{String.valueOf(id)});
                        if(rowsAffected > 0){
                            Logger.d(LOG_TAG, String.format("Added %d pomodoro seconds to pomodoro statistics.", seconds));
                        }
                    }
                }
            }
        }).start();
    }


    public void generateRandomPomodoroStats(final String startDate,final int rangeMin,final int rangeMax){
        new Thread(()->{
            final int SKIP_DAYS_LIMIT = 3;
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(Objects.requireNonNull(Utilities.getDateObject(startDate)));
            end.setTime(Objects.requireNonNull(Utilities.getDateObject(Utilities.getTodayDateString())));
            int value = -1;
            String date = null;
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(mContext);
            synchronized (dbHelper){
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                while(start.compareTo(end) <= 0){
                    date = Utilities.formatDateObject(start.getTime());
                    value = rangeMin + ((int)(Math.random() * (rangeMax-rangeMin) + 1));
                    ContentValues cv = new ContentValues();
                    cv.put(DBContract.PomodoroStatsTable.COL_NAME_DATE, date);
                    cv.put(DBContract.PomodoroStatsTable.COL_NAME_SECONDS, value);
                    db.insert(DBContract.PomodoroStatsTable.TABLE_NAME,null, cv);
                    if(Utilities.getRandomBoolean()){
                        start.add(Calendar.DAY_OF_MONTH, (int)(Math.random() * SKIP_DAYS_LIMIT + 1));
                    }else{
                        start.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            }
            EventBus.getDefault().post(new RandomStatsGenerated());
        }).start();
    }

    /**
     * Database Operations are performed so call this on separate thread.
     * @return HashMap containing stats data of pomodoro. Keys are dates and values are seconds.
     */
    public HashMap<String,Integer> getPomodoroStats(){
        HashMap<String, Integer> statsData = new HashMap<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(mContext);
        synchronized (dbHelper){
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = db.query(DBContract.PomodoroStatsTable.TABLE_NAME,
                    new String[]{DBContract.PomodoroStatsTable.COL_NAME_DATE,DBContract.PomodoroStatsTable.COL_NAME_SECONDS},
                    null,
                    null,
                    null,
                    null,
                    DBContract.PomodoroStatsTable._ID + " DESC ");
            while(c.moveToNext()){
                String date = c.getString(0);
                int seconds = c.getInt(1);
                statsData.put(date, seconds);;
            }
            c.close();
        }
        return statsData;
    }

    public void resetPomodoroStatistics(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(mContext);
                synchronized (dbHelper){
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int rowsAffected = db.delete(DBContract.PomodoroStatsTable.TABLE_NAME, "1", null);
                    if(rowsAffected > 0)
                        Logger.d(LOG_TAG, "Deleted all records from Pomodoro Stats table");
                }
                if(EventBus.getDefault().hasSubscriberForEvent(StatisticsReset.class))
                    EventBus.getDefault().post(new StatisticsReset());
            }
        }).start();
    }

}
