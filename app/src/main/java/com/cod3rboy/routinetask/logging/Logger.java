package com.cod3rboy.routinetask.logging;

import android.util.Log;

import com.cod3rboy.routinetask.BuildConfig;

public class Logger {
    private static Logger sLogger = null;
    private static final String DEFAULT_TAG = "Logger";

    private Logger(){}

    public static void d(String tag, String msg, Throwable tr){
        if(!isDebugMode()) return;
        if(tr == null) Log.d(tag, msg);
        else Log.d(tag, msg, tr);
    }
    public static void d(String tag, String msg){
        d(tag, msg, null);
    }
    public static void d(String msg){
        d(DEFAULT_TAG, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr){
        if(!isDebugMode()) return;
        if(tr == null) Log.e(tag, msg);
        else Log.e(tag, msg, tr);
    }
    public static void e(String tag, String msg){
        e(tag, msg, null);
    }
    public static void e(String msg){
        e(DEFAULT_TAG, msg, null);
    }
    public static void i(String tag, String msg, Throwable tr){
        if(!isDebugMode()) return;
        if(tr == null) Log.i(tag, msg);
        else Log.i(tag, msg, tr);
    }
    public static void i(String tag, String msg){
        i(tag, msg, null);
    }
    public static void i(String msg){
        i(DEFAULT_TAG, msg, null);
    }
    public static void v(String tag, String msg, Throwable tr){
        if(!isDebugMode()) return;
        if(tr == null) Log.v(tag, msg);
        else Log.v(tag, msg, tr);
    }
    public static void v(String tag, String msg){
        v(tag, msg, null);
    }
    public static void v(String msg){
        v(DEFAULT_TAG, msg, null);
    }
    public static void w(String tag, String msg, Throwable tr){
        if(!isDebugMode()) return;
        if(tr == null) Log.w(tag, msg);
        else Log.w(tag, msg, tr);
    }
    public static void w(String tag, String msg){
        w(tag, msg, null);
    }
    public static void w(String msg){
        w(DEFAULT_TAG, msg, null);
    }
    public static void wtf(String tag, String msg, Throwable tr){
        if(!isDebugMode()) return;
        if(tr == null) Log.wtf(tag, msg);
        else Log.wtf(tag, msg, tr);
    }
    public static void wtf(String tag, String msg){
        wtf(tag, msg, null);
    }
    public static void wtf(String msg){
        wtf(DEFAULT_TAG, msg, null);
    }

    private static boolean isDebugMode(){
        return BuildConfig.DEBUG;
    }
}
