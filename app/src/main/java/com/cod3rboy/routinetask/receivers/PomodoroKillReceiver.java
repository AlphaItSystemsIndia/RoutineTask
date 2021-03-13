package com.cod3rboy.routinetask.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.services.PomodoroService;

public class PomodoroKillReceiver extends BroadcastReceiver {
    private final String LOG_TAG = PomodoroKillReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(LOG_TAG,"Received intent to kill " + PomodoroService.class.getSimpleName());
        if (PomodoroService.getService() != null) PomodoroService.getService().suspend();
    }
}
