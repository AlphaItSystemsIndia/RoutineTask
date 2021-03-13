package com.cod3rboy.routinetask.services;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.models.ReminderModel;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.WidgetActionReceiver;

import java.util.ArrayList;

public class TodayTaskWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TasksViewFactory(this.getApplicationContext(), intent);
    }

    class TasksViewFactory implements RemoteViewsFactory {
        private final String LOG_TAG = TasksViewFactory.class.getSimpleName();
        private Context mContext;
        private final int mAppWidgetId;
        private ArrayList<TaskModel> tasksData;

        public TasksViewFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            tasksData = new ArrayList<>();
        }

        @Override
        public void onCreate() {
            loadDataSet();
        }

        private void loadDataSet() {
            Logger.d(LOG_TAG, "Loading today tasks into widget");
            StringBuilder query = new StringBuilder();
            // clear old task data
            tasksData.clear();
            query.append("SELECT temp2.*, ")
                    .append("ifnull(")
                    .append(DBContract.RemindersTable.COL_NAME_START_TIME)
                    .append(", \"99:99\") AS start_time FROM (SELECT ")
                    .append(DBContract.TasksTable.TABLE_NAME)
                    .append(".*, ifnull(temp.completed, 0) AS completed FROM ")
                    .append(DBContract.TasksTable.TABLE_NAME)
                    .append(" LEFT OUTER JOIN (SELECT ")
                    .append(DBContract.RoutineEntryTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                    .append(", count(")
                    .append(DBContract.RoutineEntryTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                    .append(") AS completed FROM ")
                    .append(DBContract.RoutineEntryTable.TABLE_NAME)
                    .append(" WHERE ")
                    .append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                    .append("=\"")
                    .append(Utilities.getTodayDateString())
                    .append("\" GROUP BY ")
                    .append(DBContract.RoutineEntryTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                    .append(") AS temp ON ")
                    .append(DBContract.TasksTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.TasksTable._ID)
                    .append("=")
                    .append("temp.")
                    .append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                    .append(" WHERE ")
                    .append(DBContract.TasksTable.TABLE_NAME)
                    .append(".")
                    .append(Utilities.getTodayWeekColumn())
                    .append("=")
                    .append(DBContract.TasksTable.COL_VALUE_TRUE)
                    .append(") AS temp2 LEFT OUTER JOIN ")
                    .append(DBContract.RemindersTable.TABLE_NAME)
                    .append(" ON temp2.")
                    .append(DBContract.TasksTable._ID)
                    .append("=")
                    .append(DBContract.RemindersTable.TABLE_NAME)
                    .append(".")
                    .append(DBContract.RemindersTable.COL_NAME_TASK_ID)
                    .append(" ORDER BY ")
                    .append(Utilities.canSortCompletedTasks(mContext) ? "temp2.completed ASC, " : "")
                    .append("start_time ASC, lower(temp2.")
                    .append(DBContract.TasksTable.COL_NAME_TITLE)
                    .append(") ASC;");
            Thread thread = new Thread(()->{
                tasksData.addAll(TaskModel.query(query.toString(), null, true));
            });
            thread.start();
            try{
                thread.join();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        @Override
        public void onDataSetChanged() {
            loadDataSet();
        }

        @Override
        public void onDestroy() {
            tasksData.clear();
        }

        @Override
        public int getCount() {
            return tasksData.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            // Construct a remote views task item based on the app widget task item xml file
            // and set the content based on position
            TaskModel taskAtPosition = tasksData.get(position);
            String title = taskAtPosition.getTitle();
            boolean taskCompleted = taskAtPosition.getStatus() == TaskModel.TaskStatus.COMPLETED;
            RemoteViews rvTaskItem = new RemoteViews(mContext.getPackageName(), R.layout.widget_today_task_item);
            rvTaskItem.setInt(R.id.root, "setBackgroundColor", taskAtPosition.getColor());
            rvTaskItem.setTextViewText(R.id.tv_task_title, title);
            ReminderModel reminder = taskAtPosition.getReminder();
            if (reminder != null) {
                // Set Start Time
                if(Utilities.getTimeFormatPreference(mContext).equals(mContext.getString(R.string.settings_time_format_value_24)))
                    rvTaskItem.setTextViewText(R.id.tv_task_time, reminder.getStartTime().to24TimeFormat());
                else
                    rvTaskItem.setTextViewText(R.id.tv_task_time, reminder.getStartTime().to12TimeFormat());
                // Set Duration
                if(reminder.getDurationInMinutes() > 0){
                    rvTaskItem.setViewVisibility(R.id.tv_duration, View.VISIBLE);
                    rvTaskItem.setTextViewText(R.id.tv_duration, Utilities.formatDuration(mContext, reminder.getDurationInMinutes()));
                }else{
                    rvTaskItem.setViewVisibility(R.id.tv_duration, View.GONE);
                    rvTaskItem.setTextViewText(R.id.tv_duration, "");
                }
            } else {
                rvTaskItem.setTextViewText(R.id.tv_task_time, mContext.getString(R.string.no_reminder));
            }

            if (taskCompleted) {
                rvTaskItem.setInt(R.id.tv_task_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG);
                rvTaskItem.setInt(R.id.btn_check, "setImageResource", R.drawable.ic_checkbox_checked);
                rvTaskItem.setContentDescription(R.id.btn_check, mContext.getString(R.string.widget_uncheck_content_desc));
            } else {
                rvTaskItem.setInt(R.id.tv_task_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
                rvTaskItem.setInt(R.id.btn_check, "setImageResource", R.drawable.checkbox_circle);
                rvTaskItem.setContentDescription(R.id.btn_check, mContext.getString(R.string.widget_check_content_desc));
            }

            // Prepare extras bundles for Widget Action broadcast
            Bundle editActionExtras = new Bundle();
            editActionExtras.putInt(WidgetActionReceiver.EXTRA_ACTION_ID, WidgetActionReceiver.ACTION_ID_EDIT_TASK);
            editActionExtras.putParcelable(WidgetActionReceiver.KEY_TASK_PARCEL, taskAtPosition);
            Bundle completeActionExtras = new Bundle();
            completeActionExtras.putInt(WidgetActionReceiver.EXTRA_ACTION_ID, WidgetActionReceiver.ACTION_ID_COMPLETE_TASK);
            completeActionExtras.putParcelable(WidgetActionReceiver.KEY_TASK_PARCEL, taskAtPosition);
            if (taskCompleted)
                completeActionExtras.putInt(WidgetActionReceiver.KEY_TASK_STATUS, WidgetActionReceiver.TASK_STATUS_COMPLETE);
            else
                completeActionExtras.putInt(WidgetActionReceiver.KEY_TASK_STATUS, WidgetActionReceiver.TASK_STATUS_NOT_COMPLETE);

            // Create fillIn intents for Widget Action broadcast
            Intent editFillInIntent = new Intent();
            editFillInIntent.putExtras(editActionExtras);
            Intent completeFillInIntent = new Intent();
            completeFillInIntent.putExtras(completeActionExtras);

            // Set separate fillIn intents on list item and image button
            rvTaskItem.setOnClickFillInIntent(R.id.first_child, editFillInIntent);
            rvTaskItem.setOnClickFillInIntent(R.id.btn_check, completeFillInIntent);

            return rvTaskItem;
        }


        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return tasksData.get(position).getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
