package com.cod3rboy.routinetask.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.database.PomodoroStatsManager;
import com.cod3rboy.routinetask.events.PomodoroStart;
import com.cod3rboy.routinetask.events.PomodoroStop;
import com.cod3rboy.routinetask.events.PomodoroUpdate;
import com.cod3rboy.routinetask.CountDownTimer;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.PomodoroKillReceiver;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.Random;

public class PomodoroService extends IntentService {

    public static final String LOG_TAG = PomodoroService.class.getSimpleName();

    public static final String CHANNEL_ID_POMODORO = "pomodoro_notification";

    private static final int CONTENT_PENDING_INTENT_REQUEST_CODE = 1001;
    private static final int STOP_PENDING_INTENT_REQUEST_CODE = 1002;

    // Field holding the instance of running service
    private static PomodoroService mService;
    public static String KEY_SECONDS = "secs";
    private boolean suspend = false;
    private MediaPlayer mPlayer;
    private CountDownTimer mTimer;
    private long mMillis;
    private long mMillisPassed;

    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private int mNotificationId;

    private PomodoroStatsManager mStatsManager;

    public PomodoroService() {
        super("Pomodoro Service");
        mService = this;
        // 1 secs is added initially since first tick is after 1 sec which is missed.
        mMillisPassed = 1000;
        mStatsManager = new PomodoroStatsManager(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.d(LOG_TAG, "Started Pomodoro Countdown Service - onHandleIntent(Intent)");
        EventBus.getDefault().post(new PomodoroStart());
        mMillis = 1000 * intent.getIntExtra(KEY_SECONDS, 0);
        registerNotificationChannel();
        createNotification(getString(R.string.str_pomodoro_running), mMillis / 1000);


        mTimer = new CountDownTimer(mMillis, 1000) {
            @Override
            public void onTick(long millisLeft) {
                Logger.d(LOG_TAG,
                        String.format("Pomodoro Countdown - Milliseconds Passed : %d \t Milliseconds Left : %d", mMillisPassed, millisLeft));
                mMillisPassed += mMillis - millisLeft;
                mMillis = millisLeft;
                updateNotification(millisLeft / 1000);
                EventBus.getDefault().post(new PomodoroUpdate((int) (millisLeft / 1000)));
            }

            @Override
            public void onFinish() {
                // Do nothing
            }
        };

        mTimer.startTimer();

        try {
            mTimer.join(); // Wait for timer thread to finish
            if (!mTimer.isCancelled()) { // If Timer is finished normally
                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
                AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                attrBuilder.setLegacyStreamType(AudioManager.STREAM_ALARM);
                mPlayer = MediaPlayer.create(
                        PomodoroService.this,
                        R.raw.sound_pomodoro,
                        attrBuilder.build(),
                        audioManager.generateAudioSessionId()
                );
                mPlayer.setLooping(true);
                mPlayer.start();
                Logger.d(LOG_TAG, "Pomodoro countdown has completed");
                // Prevent method return until timer is finished
                while (!isSuspend()) {
                    if (mMillis >= 0)
                        updateNotification(
                                getString(R.string.str_pomodoro_timeout),
                                getString(R.string.str_pomodoro_timeout_desc)
                        );
                    mMillis = -1;
                    Thread.sleep(100);
                }
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            } else {
                updateNotification(
                        getString(R.string.str_pomodoro_cancel),
                        getString(R.string.str_pomodoro_cancel_desc)
                );
                Logger.d(LOG_TAG, String.format("Pomodoro cancelled when %d seconds left", mMillis));
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.d(LOG_TAG, "Pomodoro error has occurred : " + ex.getMessage());
            EventBus.getDefault().post(new PomodoroStop(PomodoroStop.Status.FAILED));
        }

        // After Pomodoro is over add no of seconds passed to Pomodoro Statistics
        mStatsManager.addPomodoroTimeToStats(mMillisPassed);

        EventBus.getDefault().post(new PomodoroStop(PomodoroStop.Status.SUCCESS));

        Logger.d("Exiting Pomodoro Service!");
        this.stopSelf();
    }

    public void stopPomodoro() {
        suspend();
    }

    public synchronized void suspend() {
        this.suspend = true;
        this.mTimer.cancelTimer();
    }

    public synchronized boolean isSuspend() {
        return this.suspend;
    }

    public int getSecondsLeft() {
        return (int) (mMillis / 1000);
    }

    /* Static method to access service instance to communicate with service */
    public static PomodoroService getService() {
        return mService;
    }

    @Override
    public void onDestroy() {
        Logger.d(PomodoroService.class.getSimpleName() + " is being destroyed - onDestroy()");
        cancelNotification();
        mService = null;
        super.onDestroy();
    }

    // Register Notification Channel ID. No effect if notification channel is already registered
    private void registerNotificationChannel() {
        // Create a notification channel required by Android Oreo and later versions before displaying a notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If channel is already created the it is not recreated.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_POMODORO,
                    this.getString(R.string.pomodoro_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(this.getString(R.string.pomodoro_channel_desc));
            NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mgr.createNotificationChannel(channel);
        }
    }

    private void createNotification(String title, long seconds) {
        // Create Notification for the task
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_POMODORO)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationContent(seconds)))
                .setSmallIcon(R.drawable.ic_pomodoro)
                //.setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false);
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.putExtra(MainActivity.KEY_NAV_SELECTED_ITEM, R.id.nav_item_pomodoro);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                CONTENT_PENDING_INTENT_REQUEST_CODE,
                i,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        mNotificationBuilder.setContentIntent(pi); // Set pending intent on notification tap
        Intent stopSelf = new Intent(this, PomodoroKillReceiver.class);
        PendingIntent pendingStopSelf = PendingIntent.getBroadcast(
                this, STOP_PENDING_INTENT_REQUEST_CODE,
                stopSelf,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.pomodoro_action_stop), pendingStopSelf);
        Notification notification = mNotificationBuilder.build();
        //notification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        // Generate a random notification id to ensure a separate notification is displayed in case several tasks have same task time.
        Random rand = new Random(System.currentTimeMillis());
        mNotificationId = rand.nextInt();
        // Must call for this service as it is a foreground service
        startForeground(mNotificationId, notification);
    }

    private void updateNotification(long seconds) {
        mNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationContent(seconds)));
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
    }

    private void updateNotification(String title, String styleText) {
        mNotificationBuilder.setContentTitle(title);
        mNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(styleText));
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
    }

    public void cancelNotification() {
        mNotificationManager.cancel(mNotificationId);
        mNotificationId = -1;
        mNotificationManager = null;
        mNotificationBuilder = null;
    }

    private String getNotificationContent(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "Time Left : %d " + ((mins > 1) ? "Mins" : "Min") + " %d " + ((secs > 1) ? "Secs" : "Sec"), mins, secs);
    }
}
