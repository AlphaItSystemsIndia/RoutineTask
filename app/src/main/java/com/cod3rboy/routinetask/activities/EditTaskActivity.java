package com.cod3rboy.routinetask.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.events.TaskDeleted;
import com.cod3rboy.routinetask.events.TaskUpdated;
import com.cod3rboy.routinetask.fragments.EditTaskFragment;
import com.cod3rboy.routinetask.logging.Logger;
import com.cod3rboy.routinetask.receivers.TodayTaskWidgetProvider;
import com.google.android.material.appbar.MaterialToolbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * An activity to edit an existing task in the application.
 * This activity hosts a single fragment which displays the UI
 * for editing existing task in application.
 * Intent used to start this activity must contain TaskModel object as parcelable.
 */
public class EditTaskActivity extends AppCompatActivity {
    private static final String LOG_TAG = EditTaskActivity.class.getSimpleName();
    // Key used for the obtain parcelable of TaskModel from intent extras
    public static final String KEY_TASK_PARCEL = "task_parcel";
    // Unique Tag for the fragment held by this activity
    private static final String FRAGMENT_EDIT_TASK = "edit_task_fragment";

    private MaterialToolbar mToolbar;

    private TaskModel task; // task object to edit
    private EditTaskFragment editTaskFragment;

    /**
     * Called when either activity is created for the first time or when activity is recreated
     * during configuration changes
     *
     * @param savedInstanceState is null for the first time activity is created
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState); // Chain call to class hierarchy
        setContentView(R.layout.activity_edit_task);
        mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.getBackground().setAlpha(0);
        mToolbar.setElevation(0);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Get TaskModel parcelable for the activity intent extras
        task = getIntent().getParcelableExtra(KEY_TASK_PARCEL);
        if (task == null) {
            // Finish activity immediately if task is not present
            Logger.e(LOG_TAG, getString(R.string.activity_edit_error));
            finish();
            return;
        }

        // Creating fragment only if it does not already present. No need to create a new fragment if
        // activity is recreated (with new instance) during configuration changes as new fragment instance has already been
        // created automatically and attached to new activity instance by the system.
        editTaskFragment = (EditTaskFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_EDIT_TASK);
        if (editTaskFragment == null) {
            editTaskFragment = new EditTaskFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, editTaskFragment, FRAGMENT_EDIT_TASK)
                    .commit();
        }
        // Set task to edit in the fragment
        editTaskFragment.setTask(task);
    }

    public MaterialToolbar getSupportToolbar() {
        return mToolbar;
    }

    @Override
    public void onBackPressed() {
        if (!editTaskFragment.handleBackPress())
            super.onBackPressed();
    }
}
