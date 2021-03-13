package com.cod3rboy.routinetask.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.TaskApplication;
import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.services.TodayTaskWidgetService;

import java.util.Calendar;
import java.util.Locale;


public class TodayTaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_WIDGET_UPDATE = "com.cod3rboy.routinetask.action.WIDGET_UPDATE";
    public static final String ACTION_SCHEDULED_WIDGET_UPDATE = "com.cod3rboy.routinetask.action.SCHEDULED_WIDGET_UPDATE";

    private static final int TODAY_PENDING_INTENT_REQUEST_CODE = 101;
    private static final int WIDGET_UPDATE_PENDING_INTENT_REQUEST_CODE = 102;

    public static final String LOG_TAG = TodayTaskWidgetProvider.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_WIDGET_UPDATE)) {
                // Update all widgets
                int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                if (widgetIds != null) {
                    Logger.d(LOG_TAG, "Updating Widgets ...");
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetIds, R.id.lv_task_today);
                }
            } else if (action.equals(ACTION_SCHEDULED_WIDGET_UPDATE)) {
                AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                // Get ids of active widgets
                int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, TodayTaskWidgetProvider.class));
                if (ids == null || ids.length == 0) { // Schedule is triggered but no widgets are present
                    // Cancel future schedules
                    cancelScheduledUpdate(context);
                } else {
                    // Update all widgets
                    Logger.d(LOG_TAG, "Updating Widgets on schedule ...");
                    widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.lv_task_today);
                }
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // Setup the intent for TodayTAskWidgetService which will provide views for the collection.
            Intent i = new Intent(context, TodayTaskWidgetService.class);
            // Add app widget id to the intent extras
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            // Instantiate the RemoteViews object for the app widget layout.
            RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_today_task);
            // Set up the RemoteViews object to use a RemoteViews Adapter
            // This adapter connects to TodayTaskWidgetService through specified intent to
            // populate collection data into widget.
            rViews.setRemoteAdapter(R.id.lv_task_today, i);
            // Empty view is displayed when the collection has no items.
            // It should be in the same layout used to instantiate the remote views object above.
            rViews.setEmptyView(R.id.lv_task_today, R.id.empty_view);

            // Set PendingIntentTemplate to broadcast Widget Action to WidgetActionReceiver
            Intent widgetActionIntent = new Intent(context, WidgetActionReceiver.class);
            widgetActionIntent.setData(Uri.parse(widgetActionIntent.toUri(Intent.URI_INTENT_SCHEME)));
            widgetActionIntent.setAction(WidgetActionReceiver.ACTION_WIDGET_ACTION);
            PendingIntent widgetActionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    widgetActionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rViews.setPendingIntentTemplate(R.id.lv_task_today, widgetActionPendingIntent);

            // Set PendingIntent on header to launch What's Today Fragment in Main Activity
            Intent todayIntent = new Intent(context, MainActivity.class);
            todayIntent.setData(Uri.parse(todayIntent.toUri(Intent.URI_INTENT_SCHEME)));
            todayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            todayIntent.putExtra(MainActivity.KEY_NAV_SELECTED_ITEM, R.id.nav_item_whats_today);
            PendingIntent todayPendingIntent = PendingIntent.getActivity(
                    context,
                    TODAY_PENDING_INTENT_REQUEST_CODE,
                    todayIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
            );
            rViews.setOnClickPendingIntent(R.id.tv_header, todayPendingIntent);

            // Set next update schedule
            setScheduledUpdate(context);

            appWidgetManager.updateAppWidget(appWidgetId, rViews);
        }
    }


    /**
     * This method set a repeating scheduled alarm to update widgets by sending a broadcast SCHEDULED_WIDGET_UPDATE
     * when alarm is triggered. Next schedule is automatically set because it uses repeating alarm.
     *
     * @param context Any Context Object
     */
    private void setScheduledUpdate(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, TodayTaskWidgetProvider.class);
        i.setAction(ACTION_SCHEDULED_WIDGET_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, WIDGET_UPDATE_PENDING_INTENT_REQUEST_CODE, i, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar today = Calendar.getInstance(Locale.getDefault());
        int hours = today.get(Calendar.HOUR_OF_DAY);
        int minutes = today.get(Calendar.MINUTE);
        int seconds = today.get(Calendar.SECOND);

        // Get midnight offset milliseconds of 1 hour ahead
        long midNightOffset = (24 * 60 * 60 - (hours - 1) * 60 * 60 - minutes * 60 - seconds) * 1000;
        long currentTimeMillis = today.getTimeInMillis();
        manager.setRepeating(AlarmManager.RTC_WAKEUP, currentTimeMillis + midNightOffset, 24 * 60 * 60, pi);
    }

    /**
     * This method is used to cancel scheduled alarm to update widgets.
     * When there are no active widgets but because alarm was previously scheduled and is repeating,
     * we can call this method to cancel pending scheduled alarm.
     *
     * @param context Any Context Object
     */
    private void cancelScheduledUpdate(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, TodayTaskWidgetProvider.class);
        i.setAction(ACTION_SCHEDULED_WIDGET_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, WIDGET_UPDATE_PENDING_INTENT_REQUEST_CODE, i, PendingIntent.FLAG_CANCEL_CURRENT);
        manager.cancel(pi);
    }

    /**
     * This method is used to refresh active today task widgets
     * when there is some update like new task created, task updated, etc.
     */
    public static void refreshWidgets() {
        Context appContext = TaskApplication.getAppContext();
        // Get Ids of all active widgets
        int[] widgetIds = AppWidgetManager.getInstance(appContext)
                .getAppWidgetIds(new ComponentName(appContext, TodayTaskWidgetProvider.class));
        if (widgetIds.length == 0) return; // Do nothing if no active widgets found.
        // Send Broadcast to Update Widgets
        Intent widgetIntent = new Intent(appContext, TodayTaskWidgetProvider.class);
        widgetIntent.setAction(TodayTaskWidgetProvider.ACTION_WIDGET_UPDATE);
        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        appContext.sendBroadcast(widgetIntent);
    }
}
