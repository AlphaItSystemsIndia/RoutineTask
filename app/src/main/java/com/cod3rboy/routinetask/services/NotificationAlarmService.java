package com.cod3rboy.routinetask.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.Nullable;
import com.cod3rboy.routinetask.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationAlarmService extends IntentService{
    private static final String LOG_TAG = NotificationAlarmService.class.getSimpleName();
    private static NotificationAlarmService self;

    public static NotificationAlarmService getSelf(){
        return self;
    }

    private AtomicBoolean running;
    private MediaPlayer player;

    public NotificationAlarmService() {
        super(NotificationAlarmService.class.getSimpleName());
        Logger.d(LOG_TAG, "Constructing NotificationAlarmService ...");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (self == null) self = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(LOG_TAG, "Destroying NotificationAlarmService ...");
        if(player.isPlaying()) player.stop();
        player.release();
        self = null;

    }

    public void stop(){
        running.set(false);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Logger.d(LOG_TAG, "onHandleIntent() called.");
        running = new AtomicBoolean(true);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_ALARM);
        // Play Notification Alarm using Media Player on Alarm Stream
        player = MediaPlayer.create(
                this,
                alarmSound,
                null,
                attrBuilder.build(),
                audioManager.generateAudioSessionId()
        );
        player.setLooping(true);
        player.start();
        try {
            while (running.get()) Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.e(LOG_TAG, "Service error occurred - " + ex.getMessage());
            ex.printStackTrace();
        }
        player.stop();
    }
}
