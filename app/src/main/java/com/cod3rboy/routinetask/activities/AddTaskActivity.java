package com.cod3rboy.routinetask.activities;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.fragments.AddTaskFragment;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * An activity to add a new task in the application.
 * This activity hosts a single fragment which displays the UI
 * to add a new task in application.
 */
public class AddTaskActivity extends AppCompatActivity {
    // Unique tag name of the fragment held by the activity
    private static final String FRAGMENT_ADD_TASK = "add_task_fragment";

    private MaterialToolbar mToolbar;
    private AddTaskFragment addTaskFragment;

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
        setContentView(R.layout.activity_add_task);
        mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.getBackground().setAlpha(0);
        mToolbar.setElevation(0);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Creating fragment only if it does not already present. No need to create a new fragment if
        // activity is recreated (with new instance) during configuration changes as new fragment instance has already been
        // created automatically and attached to new activity instance by the system.
        addTaskFragment = (AddTaskFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_ADD_TASK);
        if (addTaskFragment == null) {
            addTaskFragment = new AddTaskFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, addTaskFragment, FRAGMENT_ADD_TASK)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!addTaskFragment.handleBackPress()) {
            // Let the activity perform default action if fragment does not handle back press event
            super.onBackPressed();
        }
    }

    public MaterialToolbar getSupportToolbar() {
        return mToolbar;
    }


}
