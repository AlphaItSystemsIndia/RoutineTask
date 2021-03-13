package com.cod3rboy.routinetask.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class Time {
    // Date Time format used in old database
    public static final String iso8601DateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    // 24-Hour Time format used in new database
    public static final String TIME_FORMAT_24_HOUR = "HH:mm";
    // 12-Hour Time format
    public static final String TIME_FORMAT_12_HOUR = "hh:mm aa";
    private int hours;
    private int minutes;


    /***
     * Create Time Object from time string in 24-Hour format.
     * @param timeText time string in format HH:mm.
     * @return Time object representing given time or Mid Night time if given time string is invalid.
     */
    public static Time from24TimeFormat(String timeText) {
        try {
            Date date = new SimpleDateFormat(TIME_FORMAT_24_HOUR).parse(timeText);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        } catch (ParseException ex) {
            return new Time(0, 0); // Return Mid Night Time
        }
    }

    /***
     * Create Time Object from datetime string in ISO 8601 Date Time format.
     * @param dateTimeText time string in format ISO 8601.
     * @return Time object representing time in given datetime string or Mid Night time if given datetime string is invalid.
     */
    public static Time fromISO8601DateFormat(String dateTimeText) {
        try {
            Date date = new SimpleDateFormat(iso8601DateTimeFormat).parse(dateTimeText);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        } catch (ParseException ex) {
            return new Time(0, 0); // Return Mid Night Time
        }
    }

    public static Time getRandomTime() {
        Random rand = new Random(System.currentTimeMillis());
        return new Time(rand.nextInt(24), rand.nextInt(60));
    }

    /**
     * Creates Time object from hours and minutes in 24-Hour format.
     *
     * @param hours   Hour of Day
     * @param minutes Minute in Hour
     */
    public Time(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    /***
     * Converts Time to 24-Hour format time string.
     * @return 24-Hour time string
     */
    public String to24TimeFormat() {
        SimpleDateFormat format24Time = new SimpleDateFormat(TIME_FORMAT_24_HOUR);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getHours());
        calendar.set(Calendar.MINUTE, getMinutes());
        return format24Time.format(calendar.getTime());
    }

    /***
     * Converts Time to 12-Hour format time string.
     * @return 12-Hour time string
     */
    public String to12TimeFormat() {
        SimpleDateFormat format12Time = new SimpleDateFormat(TIME_FORMAT_12_HOUR);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getHours());
        calendar.set(Calendar.MINUTE, getMinutes());
        return format12Time.format(calendar.getTime()).toLowerCase();
    }
}
