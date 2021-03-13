package com.cod3rboy.routinetask.fragments;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.cod3rboy.routinetask.TaskForm;
import com.cod3rboy.routinetask.Tutorials;
import com.cod3rboy.routinetask.activities.AddTaskActivity;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.events.TaskCreated;
import com.cod3rboy.routinetask.logging.Logger;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * The fragment displays the UI to add a new task in the application.
 * It also creates options menu contain save and home actions for saving new task and
 * discarding new task respectively.
 * This fragment also subscribes to receive event when a new task is created.
 * This fragment also set the result after it finishes to notify catalyst activity/fragment (since this fragment's activity
 * may be started for result i.e. startActivityForResult) about whether task was added or not.
 */
public class AddTaskFragment extends Fragment {
    private static final String LOG_TAG = AddTaskFragment.class.getSimpleName();

    private static final String KEY_SELECTED_COLOR_POSITION = "selected_color_position";
    // Holds the references to widgets contained in the UI to get values from that references
    // while inserting task
    private TaskForm addTaskForm;

    private Animator colorAnimator;

    /**
     * Called when a fragment is to be created.
     *
     * @param savedInstanceState Saved Instance State
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //Log.e(this.getClass().getName(), "onCreate called. ID " + hashCode());
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    /**
     * Called when view for this fragment needs to be created.
     * It inflates and returns the view for fragment layout, hold the references to widgets contained in the inflated view
     * and subscribe to event when a new task is added.
     *
     * @param inflater           LayoutInflater instance to inflate fragment layout from xml layout resource
     * @param container          ViewGroup instance which will hold the inflated view
     * @param savedInstanceState Saved Instance State
     * @return inflated view for the fragment layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.e(this.getClass().getName(), "onCreateView called. ID " + hashCode());
        View v = inflater.inflate(R.layout.form_task_ui, container, false);

        addTaskForm = new TaskForm(getActivity(), v);
        addTaskForm.setOnColorChangeListener(new TaskForm.OnColorChangeListener() {
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

        addTaskForm.setReminderToggleListener((reminderSet, reminderView) -> {
            if (reminderSet) Tutorials.showTaskReminderTutorial(getActivity(), reminderView);
        });

        // Register for task insert event
        EventBus.getDefault().register(this);
        Logger.d(LOG_TAG, "onCreateView() - Registered with EventBus");
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(() -> changeUIColor(addTaskForm.getSelectedColor()), 0);
        // Restore form state
        addTaskForm.restoreViewState(savedInstanceState);
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
     * It unsubscribe to task added event.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        Logger.d(LOG_TAG, "onDestroyView() - Unregistered with EventBus");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save Form State
        addTaskForm.saveViewState(outState);
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
        inflater.inflate(R.menu.options_add_task, menu);
        // Since menuitem of this fragment gets attached to its activity's toolbar after this method
        // therefore we are displaying tutorial delayed after this method completes.
        getView().post(new Runnable() {
            @Override
            public void run() {
                // Show Action Tutorial
                Tutorials.showAddTaskTutorial(getActivity(), ((AddTaskActivity) getActivity()).getSupportToolbar());
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
            case android.R.id.home: // Back Button pressed
                Logger.d(LOG_TAG, "onOptionsItemSelected() - Back option selected");
                if (!handleBackPress()) getActivity().finish();
                break;
            case R.id.action_create:
                if (addTaskForm.validate()) {
                    Logger.d(LOG_TAG, "Create menu action clicked to insert a new task");
                    insertTask(); // Insert this new task
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method which inserts a new task in the database.
     */
    private void insertTask() {
        TaskModel task = new TaskModel();
        task.setTitle(addTaskForm.getTitle());
        task.setDescription(addTaskForm.getDescription());
        task.setRepeatSunday(addTaskForm.isSundayChecked());
        task.setRepeatMonday(addTaskForm.isMondayChecked());
        task.setRepeatTuesday(addTaskForm.isTuesdayChecked());
        task.setRepeatWednesday(addTaskForm.isWednesdayChecked());
        task.setRepeatThursday(addTaskForm.isThursdayChecked());
        task.setRepeatFriday(addTaskForm.isFridayChecked());
        task.setRepeatSaturday(addTaskForm.isSaturdayChecked());
        task.setReminder(addTaskForm.getReminder());
        task.setColor(addTaskForm.getSelectedColor());
        task.save(true);
    }

    /**
     * Back Press event is passed from parent activity to this callback method in fragment.
     *
     * @return true if fragment handled back press event or false to ignore back press event so that
     * activity can perform default action.
     */
    public boolean handleBackPress() {
        if (!addTaskForm.getTitle().trim().isEmpty()
                || !addTaskForm.getDescription().trim().isEmpty()) {
            // Display dialog for save confirmation
            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle(R.string.save_confirm_dialog_title)
                    .setMessage(getResources().getString(R.string.save_confirm_dialog_message))
                    .setNegativeButton(R.string.save_confirm_dialog_action_discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    })
                    .setPositiveButton(R.string.save_confirm_dialog_action_save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (addTaskForm.validate()) {
                                insertTask(); // Insert this new task
                            }
                            dialog.dismiss();
                        }
                    }).show();
            return true;
        }
        return false;
    }

    /**
     * Event callback method to be called by EventBus when TaskCreated event is produced.
     * Here we just need to set the result for activity hosting this fragment and then finish it.
     *
     * @param event TaskCreated event object holding the created task
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskCreated(TaskCreated event) {
        getActivity().setResult(AppCompatActivity.RESULT_OK);
        getActivity().finish();
    }
}