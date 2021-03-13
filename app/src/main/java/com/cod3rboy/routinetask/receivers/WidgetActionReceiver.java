package com.cod3rboy.routinetask.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.cod3rboy.routinetask.activities.EditTaskActivity;
import com.cod3rboy.routinetask.database.models.TaskModel;

/**
 * This broadcast receiver is used to get user actions from the widget.
 */
public class WidgetActionReceiver extends BroadcastReceiver {
    public static final String ACTION_WIDGET_ACTION = "com.cod3rboy.routinetask.action.WIDGET_ACTION";

    public static final String EXTRA_ACTION_ID = "action_id_extra";

    public static final String KEY_TASK_PARCEL = "task_parcel";
    public static final String KEY_TASK_STATUS = StatisticsReceiver.KEY_TASK_STATUS;

    public static final int ACTION_ID_EDIT_TASK = 101;
    public static final int ACTION_ID_COMPLETE_TASK = 102;
    public static final int ACTION_ID_UNKNOWN = 100;

    public static final int TASK_STATUS_COMPLETE = StatisticsReceiver.TASK_STATUS_COMPLETE;
    public static final int TASK_STATUS_NOT_COMPLETE = StatisticsReceiver.TASK_STATUS_NOT_COMPLETE;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(ACTION_WIDGET_ACTION)) {
            int extra_action_id = intent.getIntExtra(EXTRA_ACTION_ID, ACTION_ID_UNKNOWN);
            if (extra_action_id == ACTION_ID_EDIT_TASK) {
                TaskModel task = intent.getParcelableExtra(KEY_TASK_PARCEL);
                if (task != null) {
                    Intent i = new Intent(context, EditTaskActivity.class);
                    i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
                    i.putExtra(EditTaskActivity.KEY_TASK_PARCEL, task);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(i);
                }
            } else if (extra_action_id == ACTION_ID_COMPLETE_TASK) {
                TaskModel task = intent.getParcelableExtra(KEY_TASK_PARCEL);
                int task_status = intent.getIntExtra(KEY_TASK_STATUS, -1);
                if (task != null) {
                    Intent i = new Intent(context, StatisticsReceiver.class);
                    i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
                    i.putExtra(StatisticsReceiver.KEY_STATISTICS_TYPE, StatisticsReceiver.STATISTICS_TYPE_TASK);
                    i.putExtra(StatisticsReceiver.KEY_TASK_PARCEL, task);
                    i.putExtra(StatisticsReceiver.KEY_TASK_STATUS, task_status);
                    context.sendBroadcast(i);
                }
            }
        }
    }

}
