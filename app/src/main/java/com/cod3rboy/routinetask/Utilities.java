package com.cod3rboy.routinetask;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.View;

import com.andrognito.flashbar.Flashbar;
import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.TaskBroadcastReceiver;

import com.mikhaellopez.ratebottomsheet.AskRateBottomSheet;
import com.mikhaellopez.ratebottomsheet.RateBottomSheet;
import com.mikhaellopez.ratebottomsheet.RateBottomSheetManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;


/**
 * Utility class containing static utilities methods
 */
public class Utilities {

    private static final String LOG_TAG = Utilities.class.getSimpleName();

    // Date Time format used in database
    public static String iso8601DateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    // 12-Hour Date Time format used in UI
    public static String uiDateTimeFormat = "hh:mm aa";

    /**
     * Returns a 24-Hour format time (HH:mm) String
     *
     * @param hour   hour of the day
     * @param minute minute of the hour
     * @return 24-Hour formatted time string
     */
    public static String getTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * Utility method to change the date time in dateText From inputFormat to format used in database.
     * Useful when converting date text used in UI to date text compatible with database.
     *
     * @param dateText    date time text in inputFormat
     * @param inputFormat source date time format
     * @return Date Time text formatted with database used format
     */
    public static String formatToDbDate(String dateText, String inputFormat) {
        try {
            Date date = new SimpleDateFormat(inputFormat).parse(dateText);
            SimpleDateFormat outputFormat = new SimpleDateFormat(iso8601DateTimeFormat);
            return outputFormat.format(date);
        } catch (ParseException ex) {
            Log.e(Utilities.class.getName(), ex.getMessage());
            return dateText;
        }
    }

    /**
     * Utility method to change the date time in format used in database to the format specified in outputFormat.
     * Useful when converting database compatible date text to date text used in UI.
     *
     * @param dbDateText   date time text in database used format
     * @param outputFormat destination date time format
     * @return Date Time text formatted with outputFormat format
     */
    public static String formatDbDate(String dbDateText, String outputFormat) {
        try {
            Date date = new SimpleDateFormat(iso8601DateTimeFormat).parse(dbDateText);
            return new SimpleDateFormat(outputFormat).format(date);
        } catch (ParseException ex) {
            Log.e(Utilities.class.getName(), ex.getMessage());
            return dbDateText;
        }
    }

    /**
     * Helper Method to generate a 12-Hour format time string from an hour, minute values in 24-Hour clock format.
     *
     * @param hour integer value representing hour
     * @param min  integer value representing minute of the hour
     */
    public static String formatTime(int hour, int min) {
        return getFormattedTime(getTime(hour, min));
    }

    /**
     * Converts 24-Hour formatted time string to 12-Hour format time string (hh:mm AM|PM) String
     *
     * @param time 24-Hour formatted time string (HH:mm)
     * @return 12-Hour formatted time string
     */
    public static String getFormattedTime(String time) {
        // Create input and output formats for time
        SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm aa");
        try {
            // Parse input date using input format and then format resulted date to output format string
            Date date = inputFormat.parse(time);
            String formattedTime = outputFormat.format(date);
            return formattedTime;
        } catch (Exception ex) {
            // Error while formatting
            Log.e(Utilities.class.getName(), ex.getMessage());
            return time; // Return unformatted time
        }
    }

    /**
     * Utility method to get an hour from given date string in given format.
     *
     * @param date   date string
     * @param format format of the date string
     * @return hour of the day or -1
     */
    public static int getHour(String date, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new SimpleDateFormat(format).parse(date));
            return cal.get(Calendar.HOUR_OF_DAY);

        } catch (ParseException ex) {
            Log.e(Utilities.class.getName(), ex.getMessage());
            return -1;
        }
    }

    /**
     * Utility method to get minute of hour from given date string in given format.
     *
     * @param date   date string
     * @param format format of the date string
     * @return Minute of hour or -1
     */
    public static int getMinute(String date, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new SimpleDateFormat(format).parse(date));
            return cal.get(Calendar.MINUTE);

        } catch (ParseException ex) {
            Log.e(Utilities.class.getName(), ex.getMessage());
            return -1;
        }
    }

    /**
     * Helper method to return the current hour of day
     *
     * @return current hour
     */
    public static int getCurrentHour() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Helper Method to return the current minute of hour
     *
     * @return current minute
     */
    public static int getCurrentMinute() {
        return Calendar.getInstance().get(Calendar.MINUTE);
    }

    /**
     * Static Helper method to display a task deleted alert
     */
    public static void showTaskDeleted(Activity activity, int count) {
        String message = null;
        if (count > 1) {
            message = String.format(Locale.getDefault(), activity.getString(R.string.flash_task_deleted), count, "s");
        } else {
            message = String.format(Locale.getDefault(), activity.getString(R.string.flash_task_deleted), count, "");
        }
        new Flashbar.Builder(activity)
                .gravity(Flashbar.Gravity.BOTTOM)
                .title(message)
                .showIcon()
                .icon(R.drawable.ic_bin)
                .message(activity.getString(R.string.flash_swipe_dismiss))
                .duration(Flashbar.DURATION_LONG)
                .enableSwipeToDismiss()
                .dismissOnTapOutside()
                .backgroundColorRes(R.color.colorAccent)
                .showOverlay()
                .overlayColorRes(R.color.modal)
                .overlayBlockable()
                .build()
                .show();
    }

    /**
     * Static Helper method to display a task updated alert
     */
    public static void showTaskUpdated(Activity activity) {
        new Flashbar.Builder(activity)
                .gravity(Flashbar.Gravity.BOTTOM)
                .title(activity.getString(R.string.flash_task_updated))
                .showIcon()
                .icon(R.drawable.ic_tick)
                .message(activity.getString(R.string.flash_swipe_dismiss))
                .duration(Flashbar.DURATION_LONG)
                .enableSwipeToDismiss()
                .dismissOnTapOutside()
                .backgroundColorRes(R.color.colorAccent)
                .showOverlay()
                .overlayColorRes(R.color.modal)
                .overlayBlockable()
                .build()
                .show();
    }

    /**
     * Static Helper method to display a task added alert
     */
    public static void showTaskAdded(Activity activity) {
        new Flashbar.Builder(activity)
                .gravity(Flashbar.Gravity.BOTTOM)
                .title(activity.getString(R.string.flash_task_created))
                .showIcon()
                .icon(R.drawable.ic_check)
                .message(activity.getString(R.string.flash_swipe_dismiss))
                .duration(Flashbar.DURATION_LONG)
                .enableSwipeToDismiss()
                .dismissOnTapOutside()
                .backgroundColorRes(R.color.colorAccent)
                .showOverlay()
                .overlayColorRes(R.color.modal)
                .overlayBlockable()
                .build()
                .show();
    }

    /**
     * Get Today Weekday database field
     */
    public static String getTodayWeekColumn() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return getWeekColumnName(day);
    }

    /**
     * Returns table column name for the given week day number.
     *
     * @param weekDay day of week number (1-Sunday, 2-Monday, ..., 7-Saturday)
     * @return table column name for weekDay or empty string if weekDay is invalid
     */
    public static String getWeekColumnName(int weekDay) {
        switch (weekDay) {
            case Calendar.SUNDAY:
                return DBContract.TasksTable.COL_NAME_SUNDAY;
            case Calendar.MONDAY:
                return DBContract.TasksTable.COL_NAME_MONDAY;
            case Calendar.TUESDAY:
                return DBContract.TasksTable.COL_NAME_TUESDAY;
            case Calendar.WEDNESDAY:
                return DBContract.TasksTable.COL_NAME_WEDNESDAY;
            case Calendar.THURSDAY:
                return DBContract.TasksTable.COL_NAME_THURSDAY;
            case Calendar.FRIDAY:
                return DBContract.TasksTable.COL_NAME_FRIDAY;
            case Calendar.SATURDAY:
                return DBContract.TasksTable.COL_NAME_SATURDAY;
        }
        return "";
    }

    /**
     * Get Today's Formatted Date
     */
    public static String getTodayDate() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMMM");
        return sdf.format(date);
    }

    /**
     * Helper method to show user a rating bottom sheet every period of time until user explicitly
     * disables it.
     *
     * @param activity Activity hosting dialog box
     */
    public static void showRatingReminder(AppCompatActivity activity) {
        new RateBottomSheetManager(activity)
                .setInstallDays(1) // 3 by default
                .setLaunchTimes(3) // 5 by default
                .setRemindInterval(1) // 2 by default
                .setShowAskBottomSheet(true) // True by default
                .setShowLaterButton(true) // True by default
                .setShowCloseButtonIcon(false) // True by default
                .setDebugForceOpenEnable(false)
                .monitor();

        // Show bottom sheet if meets conditions
        // With AppCompatActivity or Fragment
        RateBottomSheet.Companion.showRateBottomSheetIfMeetsConditions(activity, null);
    }

    /**
     * Function to generate a random string of length between min, max
     *
     * @param min Minimum Length of random string
     * @param max Maximum Length of random string
     * @return Random string of length between min and max
     */
    public static String getRandomAlphaString(int min, int max) {
        Random rand = new Random(System.currentTimeMillis());
        // Determine length of resulting string
        int n = min + rand.nextInt(max - min + 1);
        // chose a Character random from this String
        String AlphaString = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int) (AlphaString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaString
                    .charAt(index));
        }
        return sb.toString();
    }

    /**
     * This method is used to generate a random time formatted string.
     *
     * @return Formatted string of random time
     */
    public static String getRandomTimeString() {
        Random rand = new Random(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, rand.nextInt(24));
        cal.set(Calendar.MINUTE, rand.nextInt(60));
        Date d = cal.getTime();
        return formatDbDate(new SimpleDateFormat(iso8601DateTimeFormat).format(d), iso8601DateTimeFormat);
    }

    /**
     * This method is used to generate a random boolean value.
     *
     * @return true or false
     */
    public static boolean getRandomBoolean() {
        double rand = Math.random();
        return rand > 0.5;
    }

    /**
     * This method is used to generate string for today date in yyyy-MM-dd format.
     *
     * @return today date string
     */
    public static String getTodayDateString() {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return df.format(today);
    }

    public static Date getDateObject(String date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            return df.parse(date);
        } catch (java.text.ParseException e) {
            Logger.e(LOG_TAG, e.getMessage());
        }
        return null;
    }

    public static String formatDateObject(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return df.format(date);
    }

    public static String getWeekDay(String dateString, String inputFormat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(inputFormat, Locale.getDefault());
        try {
            Date date = df.parse(dateString);
            cal.setTime(date);
            int weekDay = cal.get(Calendar.DAY_OF_WEEK);
            switch (weekDay) {
                case Calendar.SUNDAY:
                    return "Sun";
                case Calendar.MONDAY:
                    return "Mon";
                case Calendar.TUESDAY:
                    return "Tue";
                case Calendar.WEDNESDAY:
                    return "Wed";
                case Calendar.THURSDAY:
                    return "Thu";
                case Calendar.FRIDAY:
                    return "Fri";
                case Calendar.SATURDAY:
                    return "Sat";
            }
        } catch (java.text.ParseException e) {
            Logger.e(LOG_TAG, e.getMessage());
        }
        return "N/A";
    }

    public static String getNiceHumanWeekDay(String dateString, String inputFormat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(inputFormat, Locale.getDefault());
        try {
            Date date = df.parse(dateString);
            cal.setTime(date);
            Calendar today = Calendar.getInstance();
            boolean isToday = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR);
            if (isToday) return "Today";
            else return getWeekDayName(cal.get(Calendar.DAY_OF_WEEK));
        } catch (java.text.ParseException e) {
            Logger.e(LOG_TAG, e.getMessage());
        }
        return "N/A";
    }

    public static String getWeekDayName(int weekDayNumber) {
        switch (weekDayNumber) {
            case Calendar.SUNDAY:
                return "Sun";
            case Calendar.MONDAY:
                return "Mon";
            case Calendar.TUESDAY:
                return "Tue";
            case Calendar.WEDNESDAY:
                return "Wed";
            case Calendar.THURSDAY:
                return "Thu";
            case Calendar.FRIDAY:
                return "Fri";
            case Calendar.SATURDAY:
                return "Sat";
        }
        return "N/A";
    }

    public static int getWeekDayNumber(String dateString, String inputFormat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(inputFormat, Locale.getDefault());
        try {
            Date date = df.parse(dateString);
            cal.setTime(date);
            return cal.get(Calendar.DAY_OF_WEEK);
        } catch (java.text.ParseException e) {
            Logger.e(LOG_TAG, e.getMessage());
        }
        return -1;
    }


    public static String getFormattedChartDate(String date, String inputFormat) {
        SimpleDateFormat inputSdf = new SimpleDateFormat(inputFormat, Locale.getDefault());
        SimpleDateFormat outputSdf = new SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault());
        try {
            Date d = inputSdf.parse(date);
            Calendar today = Calendar.getInstance();
            Calendar input = Calendar.getInstance();
            input.setTime(d);
            boolean isToday = today.get(Calendar.YEAR) == input.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == input.get(Calendar.DAY_OF_YEAR);
            today.add(Calendar.DAY_OF_YEAR, -1);
            boolean isYesterday = today.get(Calendar.YEAR) == input.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == input.get(Calendar.DAY_OF_YEAR);
            if (isToday) return "Today";
            else if (isYesterday) return "Yesterday";
            else return outputSdf.format(d);
        } catch (java.text.ParseException e) {
            Logger.e(LOG_TAG, e.getMessage());
        }
        return "N/A";
    }

    public static String niceHumanDateFormat(String date, String inputFormat, String outputFormat) {
        SimpleDateFormat inputSdf = new SimpleDateFormat(inputFormat, Locale.getDefault());
        SimpleDateFormat outputSdf = new SimpleDateFormat(outputFormat, Locale.getDefault());
        try {
            Date d = inputSdf.parse(date);
            Calendar today = Calendar.getInstance();
            Calendar input = Calendar.getInstance();
            input.setTime(d);
            boolean isToday = today.get(Calendar.YEAR) == input.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == input.get(Calendar.DAY_OF_YEAR);
            today.add(Calendar.DAY_OF_YEAR, -1);
            boolean isYesterday = today.get(Calendar.YEAR) == input.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == input.get(Calendar.DAY_OF_YEAR);
            if (isToday) return "Today";
            else if (isYesterday) return "Yesterday";
            else return outputSdf.format(d);
        } catch (java.text.ParseException e) {
            Logger.e(LOG_TAG, e.getMessage());
        }
        return "N/A";
    }

    private static final int SECS_IN_HOUR = 60 * 60;
    private static final int SECS_IN_MINUTE = 60;

    public static String formatSecondsToChartValue(long seconds) {
        long hours = seconds / SECS_IN_HOUR;
        seconds = seconds % SECS_IN_HOUR;
        long minutes = seconds / SECS_IN_MINUTE;
        seconds = seconds % SECS_IN_MINUTE;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours);
            sb.append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes);
            sb.append("m ");
        }
        if (seconds > 0) {
            sb.append(seconds);
            sb.append("s ");
        }
        return sb.toString();
    }

    /**
     * This method is used to format duration in minutes to a text presentable to user.
     *
     * @param context           Context object
     * @param durationInMinutes task reminder duration in minutes
     * @return formatted duration text presentable to user
     */
    public static String formatDuration(Context context, long durationInMinutes) {
        if (durationInMinutes <= 0) return context.getString(R.string.label_no_duration);
        long hours = durationInMinutes / 60;
        long minutes = durationInMinutes % 60;
        if (hours > 0) return String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes);
        return String.format(Locale.getDefault(), "%02dm", minutes);
    }

    /**
     * Method to access the settings preferences flag indicating whether completed tasks should
     * always be displayed at the bottom of task list.
     *
     * @param context A Context object
     * @return value of stored boolean preference or default value
     */
    public static boolean canSortCompletedTasks(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(
                context.getString(R.string.settings_sort_task_key),
                Boolean.parseBoolean(context.getString(R.string.settings_sort_task_default_value))
        );
    }

    /**
     * Method to access the settings preferences flag indicating whether a sound should be played
     * when user completes a task.
     *
     * @param context A Context object
     * @return value of store boolean preference or default value
     */
    public static boolean canPlayTaskCompleteSound(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(
                context.getString(R.string.settings_sound_task_complete_key),
                Boolean.parseBoolean(context.getString(R.string.settings_sound_task_complete_default_value))
        );
    }

    /**
     * Method to access the settings preferences value for the first day of the week.
     *
     * @param context A Context object
     * @return {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    public static int getFirstDayOfWeekPreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                context.getString(R.string.settings_first_weekday_key),
                context.getString(R.string.settings_first_weekday_value_monday)
        ));
    }

    /**
     * Method to access the settings preferences value for the time format.
     *
     * @param context A Context object
     * @return "12" for 12-Hour format and "24" for 24-Hour format
     */
    public static String getTimeFormatPreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(
                context.getString(R.string.settings_time_format_key),
                context.getString(R.string.settings_time_format_value_12)
        );
    }

    /**
     * Method to access the settings preferences flag indicating whether a alarm should be played
     * with a task notification.
     *
     * @param context A Context object
     * @return true if alarm should be played otherwise false
     */
    public static boolean isTaskNotificationAlarmEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(
                context.getString(R.string.settings_notification_alarm_key),
                Boolean.parseBoolean(context.getString(R.string.settings_notification_alarm_default_value))
        );
    }

    /**
     * Method to access the settings preferences flag indicating whether a alarm should be played
     * when task duration is completed in the notification.
     *
     * @param context A Context object
     * @return true if alarm should be played otherwise false
     */
    public static boolean isTaskDurationAlarmEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(
                context.getString(R.string.settings_duration_alarm_key),
                Boolean.parseBoolean(context.getString(R.string.settings_duration_alarm_default_value))
        );
    }
}
