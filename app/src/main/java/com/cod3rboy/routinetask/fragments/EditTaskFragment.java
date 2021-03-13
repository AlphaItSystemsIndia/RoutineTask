package com.cod3rboy.routinetask.fragments;


import android.animation.Animator;
import android.app.Activity;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.TaskApplication;
import com.cod3rboy.routinetask.TaskForm;
import com.cod3rboy.routinetask.Tutorials;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.activities.EditTaskActivity;
import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.DatabaseHelper;
import com.cod3rboy.routinetask.database.models.ReminderModel;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.events.TaskDeleted;
import com.cod3rboy.routinetask.events.TaskUpdated;
import com.cod3rboy.routinetask.logging.Logger;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import needle.Needle;

/**
 * This fragment displays UI which is used to edit an existing task.
 * It adds save and delete actions to its activity's action bar.
 * id of the task to edit must me passed to this fragment so that
 * it can fetch the data of existing task using AsyncTask and populate UI with that
 * data before the editing can be done.
 * Note that once AsyncTask is completed it is not started again in the new instance of this fragment
 * created during configuration changes since every widget in UI has unique id attribute which enables system to
 * automatically retain widget values in new fragment instance so there is no need to retrieve those values by starting task again.
 * It also subscribes to the task updated and task deleted events to notify the activity
 * which may have started host activity for result in order to know whether user has perform some action or not.
 */
public class EditTaskFragment extends Fragment {
    private static final String LOG_TAG = EditTaskFragment.class.getSimpleName();
    private static final String KEY_SELECTED_COLOR_POSITION = "selected_color_position";
    // Holds the references to widgets contained in the UI to get values from that references
    // while updating task
    private TaskForm editTaskForm;
    private TaskModel task;
    private Animator colorAnimator;
    // Set when user changed the type of task from non-repeating to repeating or vice-versa.
    private boolean taskTypeChanged = false;

    /**
     * Called when a fragment is to be created
     * Here we retrieve the taskId from fragment arguments, of the task to be edited.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Called when view for this fragment needs to be created.
     * It inflates and returns the view for fragment layout, hold the references to widgets contained in the inflated view
     * and subscribe to event when task is updated.
     *
     * @param inflater           LayoutInflater instance to inflate fragment layout from xml layout resource
     * @param container          ViewGroup instance which will hold the inflated view
     * @param savedInstanceState
     * @return inflated view for the fragment layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.form_task_ui, container, false);
        editTaskForm = new TaskForm(getContext(), view);
        // Load task only when form is created first time
        if (savedInstanceState == null) editTaskForm.loadTask(task);
        editTaskForm.setOnColorChangeListener(new TaskForm.OnColorChangeListener() {
            @Override
            public void colorChanged(int color, int position, View colorView) {
                // Apply selected color to the whole UI
                if (colorAnimator != null && colorAnimator.isRunning()) {
                    colorAnimator.cancel();
                    changeUIColor(color);
                } else {
                    changeUIColor(color, true, colorView);
                }
            }
        });
        editTaskForm.setReminderToggleListener((reminderSet, reminderView) -> {
            if (reminderSet) Tutorials.showTaskReminderTutorial(getActivity(), reminderView);
        });
        // Subscribe to task events
        EventBus.getDefault().register(this);
        Logger.d(LOG_TAG, "onCreateView() - Registered with EventBus");
        return view;
    }


    /**
     * Called after the view for this fragment is created.
     * Here we start the task to retrieve the data model.
     * We start task only if it is not completed to avoid restarting a completed task
     * otherwise user's editing will not persist after configuration changes.
     *
     * @param view               view for the fragment layout
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(() -> changeUIColor(editTaskForm.getSelectedColor()), 0);
        // Restore form state
        editTaskForm.restoreViewState(savedInstanceState);
    }

    public void changeUIColor(int color, boolean animate, View colorView) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Window window = activity.getWindow();
        View v = window.getDecorView();
        v.setBackgroundColor(color);
        window.setNavigationBarColor(color);
        window.setStatusBarColor(color);
        if (animate && colorView != null) { // Animate only when animated=true and tapped color view is also passed
            float halfWidth = colorView.getWidth() / 2f;
            float halfHeight = colorView.getHeight() / 2f;
            int[] coords = new int[2];
            colorView.getLocationInWindow(coords);
            int centerX = (int) (coords[0] + halfWidth);
            int centerY = (int) (coords[1] + halfHeight);
            float startRadius = (float) Math.hypot(halfWidth, halfHeight);
            float endRadius = (float) Math.hypot(v.getWidth(), v.getHeight());
            // Show circular reveal animation
            colorAnimator = ViewAnimationUtils.createCircularReveal(v, centerX, centerY, startRadius, endRadius);
            colorAnimator.setDuration(400);
            colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            colorAnimator.start();
        }
    }

    public void changeUIColor(int color) {
        changeUIColor(color, false, null);
    }

    /**
     * Called when fragment's view is about to get destroyed during fragment's transition from PAUSED to STOPPED state.
     * Cancel the task, save the current taskCompleted status in fragment argument and
     * unsubscribe from task updated event.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // UnSubscribe from task events
        EventBus.getDefault().unregister(this);
        Logger.d(LOG_TAG, "onDestroyView() - Unregistered with EventBus");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save Form State
        editTaskForm.saveViewState(outState);
    }

    /**
     * Called only if setHasOptionMenu is set to true.
     * Inflate the options menu of this fragment
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_edittask, menu);

        // Since menuitem of this fragment gets attached to its activity's toolbar after this method
        // therefore we are displaying tutorial delayed after this method completes.
        getView().post(new Runnable() {
            @Override
            public void run() {
                // Show Tutorial
                Tutorials.showEditTaskTutorial(getActivity(), ((EditTaskActivity) getActivity()).getSupportToolbar());
            }
        });
    }

    /**
     * Called when an option menu is selected in action bar.
     * Note that when an option menu is selected, event is first passed to activity's onOptionsItemSelected
     * and then if it returns false only then this method is called.
     *
     * @param item selected options item
     * @return true indicating event is handled or false to indicate event is not handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (editTaskForm.validate()) {
                    Logger.d(LOG_TAG, "onOptionsItemSelected() - Save option selected");
                    updateTask();
                }
                return true;
            case R.id.action_delete:
                Logger.d(LOG_TAG, "onOptionsItemSelected() - Delete option selected");
                // Show Delete dialog
                new MaterialAlertDialogBuilder(getActivity())
                        .setTitle(R.string.task_del_dialog_title)
                        .setMessage(getResources().getString(R.string.task_del_dialog_message))
                        .setNeutralButton(R.string.task_del_dialog_action_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.task_del_dialog_action_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                task.delete(true);
                                dialog.dismiss();
                            }
                        }).show();
                return true;
            case android.R.id.home:
                Logger.d(LOG_TAG, "onOptionsItemSelected() - Back option selected");
                if (!handleBackPress()) getActivity().finishAfterTransition();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method which updates task in the database.
     */
    private void updateTask() {
        task.setTitle(editTaskForm.getTitle());
        task.setDescription(editTaskForm.getDescription());
        boolean changedFromNonRepeatToRepeat = (task.getRepeatCountInWeek() == 0) && !editTaskForm.isNoRepeatChecked();
        boolean changedFromRepeatToNonRepeat = (task.getRepeatCountInWeek() > 0) && editTaskForm.isNoRepeatChecked();
        // Check whether user changed the task type
        taskTypeChanged = changedFromNonRepeatToRepeat | changedFromRepeatToNonRepeat;
        task.setRepeatSunday(editTaskForm.isSundayChecked());
        task.setRepeatMonday(editTaskForm.isMondayChecked());
        task.setRepeatTuesday(editTaskForm.isTuesdayChecked());
        task.setRepeatWednesday(editTaskForm.isWednesdayChecked());
        task.setRepeatThursday(editTaskForm.isThursdayChecked());
        task.setRepeatFriday(editTaskForm.isFridayChecked());
        task.setRepeatSaturday(editTaskForm.isSaturdayChecked());
        ReminderModel reminder = task.getReminder();
        ReminderModel formReminder = editTaskForm.getReminder();
        if (reminder != null && formReminder != null && !reminder.equals(formReminder)) {
            // Update Reminder
            reminder.setDurationInMinutes(formReminder.getDurationInMinutes());
            reminder.setStartTime(formReminder.getStartTime());
        } else if (reminder != null && formReminder == null) {
            // Delete Reminder
            reminder.detach();
        } else if (reminder == null && formReminder != null) {
            // Set Reminder
            task.setReminder(formReminder);
            formReminder.setTaskId(task.getId());
        }
        task.setColor(editTaskForm.getSelectedColor());
        task.save(true);
    }

    /**
     * Back Press event is passed from parent activity to this callback method in fragment.
     *
     * @return true if fragment handled back press event or false to ignore back press event so that
     * activity can perform default action.
     */
    public boolean handleBackPress() {
        if (!editTaskForm.hasTaskState(task)) {
            // Display dialog for save confirmation
            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle(R.string.save_confirm_dialog_title)
                    .setMessage(getResources().getString(R.string.save_confirm_dialog_message))
                    .setNegativeButton(R.string.save_confirm_dialog_action_discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finishAfterTransition();
                        }
                    })
                    .setPositiveButton(R.string.save_confirm_dialog_action_save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editTaskForm.validate()) {
                                updateTask(); // Insert this new task
                            }
                            dialog.dismiss();
                        }
                    }).show();
            return true;
        }
        return false;
    }


    public void setTask(TaskModel task) {
        this.task = task;
    }

    /**
     * TaskUpdated Event Callback method which EventBus will invoke after task is updated.
     * Here result is set for host activity and it is finished.
     *
     * @param event TaskUpdated event object holding the updated task
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskUpdated(TaskUpdated event) {
        Log.d(LOG_TAG, "EditTaskForm - onTaskUpdated Called");

        // Delete routine entries for the task if task type was changed from repeating to non-repeat task.
        if (taskTypeChanged && task.getRepeatCountInWeek() == 0) {
            // Delete all routine entries except entry with today date (to preserve completion status of task).
            StringBuilder whereClause = new StringBuilder();
            whereClause.append(DBContract.RoutineEntryTable.COL_NAME_TASK_ID)
                    .append(" = ?")
                    .append(" AND ")
                    .append(DBContract.RoutineEntryTable.COL_NAME_DATE)
                    .append(" <> ?");
            Needle.onBackgroundThread().serially().execute(() -> {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(TaskApplication.getAppContext());
                synchronized (dbHelper) {
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    db.delete(DBContract.RoutineEntryTable.TABLE_NAME, whereClause.toString(),
                            new String[]{String.valueOf(task.getId()), Utilities.getTodayDateString()});
                }
            });
        }
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    /**
     * TaskDeleted Event Callback method which EventBus will invoke after task is deleted.
     * Here we just set result for catalyst activity/fragment and finish this fragment's host activity.
     *
     * @param event TaskDeleted event object holding the deleted task
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskDeleted(TaskDeleted event) {
        // Refresh any Widgets
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }
}
