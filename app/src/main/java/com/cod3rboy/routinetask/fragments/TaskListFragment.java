package com.cod3rboy.routinetask.fragments;

import android.app.Activity;
import android.app.Dialog;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;

import com.cod3rboy.routinetask.ActionbarModeCallback;
import com.cod3rboy.routinetask.TaskViewHolder;
import com.cod3rboy.routinetask.Tutorials;
import com.cod3rboy.routinetask.activities.AddTaskActivity;
import com.cod3rboy.routinetask.BuildConfig;
import com.cod3rboy.routinetask.activities.EditTaskActivity;
import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.TaskListAdapter;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.background.TasksLoader;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.events.TaskDeleted;
import com.cod3rboy.routinetask.events.TasksDeleted;
import com.cod3rboy.routinetask.logging.Logger;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;


/**
 * This is a Fragment class used to display the list of the tasks added by the user.
 * This class helps to create a new task by starting a separate activity (AddTaskActivity).
 * It uses Context Action Bar (using ActionMode interface) to provide Edit and Delete actions
 * for each task in the list.
 * This class uses AsyncTaskLoader to load/update tasks in the list asynchronously.
 */

public class TaskListFragment extends RecyclerViewFragment implements LoaderManager.LoaderCallbacks<ArrayList<TaskModel>> {

    private static final String LOG_TAG = TaskListFragment.class.getSimpleName();

    private static final String KEY_SELECTED_POSITIONS = "selected_positions";

    public static final int LAYOUT_TYPE_LIST = 128;
    public static final int LAYOUT_TYPE_GRID = 256;

    private static final int GRID_LAYOUT_COLUMNS = 2;

    // Request codes for the activities which need to be started for results
    public static final int NEW_TASK_REQUEST_CODE = 155; // For Activity which adds a new task
    public static final int EDIT_TASK_REQUEST_CODE = 156; // For Activity which edits a new task

    // ActionMode which provides callback methods to handle context action bar events
    private ActionbarModeCallback actionMode;

    private FloatingActionButton mFab;
    private int fabAnchorId;

    private int mLayoutType;

    // Adapter for RecyclerView
    private TaskListAdapter adapter;

    // Type of fragment
    private int fragmentType;

    // Layout resource for the list item
    private int itemLayoutId;

    // Selected Items Positions
    private ArrayList<Integer> selectedPositions;

    public static TaskListFragment getInstance(int fragmentType) {
        TaskListFragment fragment = new TaskListFragment();
        Bundle args = new Bundle();
        args.putInt(MainActivity.KEY_FRAGMENT_TYPE, fragmentType);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * This method is invoked by super class to get the layout resource id for this fragment so that
     * it can inflate fragment layout in onCreateView method.
     *
     * @return int layout resource id
     */
    @Override
    public int getLayoutResourceId() {
        return R.layout.fragment_tasks_list;
    }

    /**
     * This method is invoked by super class to get the resource id of RecyclerView in fragment layout.
     * This method helps super class to obtain and hold RecyclerView reference from inflated fragment layout in
     * onCreateView method.
     *
     * @return int resource id of RecyclerView
     */
    @Override
    public int getRecyclerViewId() {
        return R.id.tasks_list;
    }

    /**
     * Called when a fragment instance is created
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tells host activity that this fragment has options menu to append in the activity's action bar so that
        // activity can call this fragment's onCreateOptionsMenu callback.
        setHasOptionsMenu(true);
        Logger.d(LOG_TAG, "onCreate()");
        // Set fragment type
        fragmentType = (getArguments() == null) ? 0 : getArguments().getInt(MainActivity.KEY_FRAGMENT_TYPE);
        // Display Rating Reminder
        Utilities.showRatingReminder((MainActivity) getActivity());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) getActivity()).onSectionAttached(fragmentType);
        if (fragmentType == MainActivity.FRAGMENT_TODAY)
            ((MainActivity) getActivity()).refreshActionBar(Utilities.getTodayDate());
        else
            ((MainActivity) getActivity()).refreshActionBar();
        Logger.d(LOG_TAG, "onAttach() with fragmentType - " + fragmentType);
        // Start tasks loader
        Logger.d(LOG_TAG, "Starting Loader to load tasks");
        LoaderManager.getInstance(this).initLoader(fragmentType, null, TaskListFragment.this);
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
        inflater.inflate(R.menu.options_home, menu);
        MenuItem addRandomTasksItem = menu.findItem(R.id.action_add_random);
        addRandomTasksItem.setVisible(BuildConfig.DEBUG);
        MenuItem actionChangeLayout = menu.findItem(R.id.action_change_layout);
        updateLayoutMenuItem(actionChangeLayout);

        // Since menuitem of this fragment gets attached to its activity's toolbar after this method
        // therefore we are displaying tutorial delayed after this method completes.
        getView().post(new Runnable() {
            @Override
            public void run() {
                // Show Tutorial
                Tutorials.showFabAndDrawerTutorial(getActivity(), mFab, ((MainActivity) getActivity()).getSupportToolbar());
            }
        });
    }

    /**
     * Updates the state of layout change menu item account to the current `mLayoutType`.
     */
    private void updateLayoutMenuItem(MenuItem actionChangeLayout) {
        // Verify menu item is correct
        if (actionChangeLayout.getItemId() != R.id.action_change_layout) return;

        if (hasGridLayout()) {
            actionChangeLayout.setTitle(getString(R.string.title_option_layout_list));
            actionChangeLayout.setIcon(R.drawable.ic_layout_list);
        } else if (hasListLayout()) {
            actionChangeLayout.setTitle(getString(R.string.title_option_layout_grid));
            actionChangeLayout.setIcon(R.drawable.ic_layout_grid);
        }
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
            case R.id.action_add_random:
                Logger.d(LOG_TAG, "Add Random(Green Plus icon) option clicked in option menu");
                showRandomTaskGeneratorDialog();
                return true;
            case R.id.action_change_layout:
                if (mLayoutType == LAYOUT_TYPE_LIST) mLayoutType = LAYOUT_TYPE_GRID;
                else if (mLayoutType == LAYOUT_TYPE_GRID) mLayoutType = LAYOUT_TYPE_LIST;
                determineItemLayoutResource();
                adapter.setItemLayoutResourceId(itemLayoutId);
                updateLayoutManager();
                updateLayoutMenuItem(item);
                saveLayoutTypePreference();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * This method is used to save the current layout type into preferences.
     */
    private void saveLayoutTypePreference() {
        switch (fragmentType) {
            case MainActivity.FRAGMENT_TODAY:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putInt(getString(R.string.pref_whats_today_layout_type), mLayoutType)
                        .apply();
                break;
            case MainActivity.FRAGMENT_ROUTINE:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putInt(getString(R.string.pref_routine_tasks_layout_type), mLayoutType)
                        .apply();
                break;
            case MainActivity.FRAGMENT_ONE_TIME:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putInt(getString(R.string.pref_one_time_tasks_layout_type), mLayoutType)
                        .apply();
        }
    }

    /**
     * This method is used to display a dialog to generate any given number of
     * random routine tasks.
     */
    private void showRandomTaskGeneratorDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setTitle("Generate Random Tasks");
        dialog.setContentView(R.layout.dialog_number_picker);
        EditText inputEditText = dialog.findViewById(R.id.inputEditText);
        Button createButton = dialog.findViewById(R.id.btnCreate);
        Button cancelButton = dialog.findViewById(R.id.btnCancel);
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            Logger.d(LOG_TAG, "Dialog cancelled for random record generation");
        });
        createButton.setOnClickListener(v -> {
            int count = Integer.parseInt(inputEditText.getText().toString());
            Logger.d(LOG_TAG, String.format("Generating %d random task records", count));
            ArrayList<TaskModel> randomTasks = new ArrayList<>();
            for (int i = 0; i < count; i++) randomTasks.add(TaskModel.makeRandomTask());
            TaskModel.insertAsync(randomTasks, true);
            dialog.dismiss();
        });
        dialog.show();
    }


    /**
     * Called when an activity, started by this fragment for result, finishes after invoking setResult.
     * This method is used to restart the list loading task based on the results of the Activity
     * which adds new task or Activity which modifies existing task.
     *
     * @param requestCode Request code set during startActivityForResult
     * @param resultCode  Result code set during setResult
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == NEW_TASK_REQUEST_CODE)
                Utilities.showTaskAdded(getActivity());
            else if (requestCode == EDIT_TASK_REQUEST_CODE)
                Utilities.showTaskUpdated(getActivity());
        }
    }

    /**
     * Called to create and return view for this fragment.
     * Here we get the super class inflated layout resource for this fragment and setup LayoutManager and EmptyView.
     *
     * @param inflater           LayoutInflater instance
     * @param container          Root container
     * @param savedInstanceState Bundle instance
     * @return View for the fragment layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Logger.d(LOG_TAG, "onCreateView() called");
        // Super class inflates the layout resource for this fragment
        View v = super.onCreateView(inflater, container, savedInstanceState);
        // Set RecyclerView Layout Manager
        setUserPreferredLayoutManager();
        // Determine layout to use for item
        determineItemLayoutResource();
        // Create and set an empty adapter
        adapter = new TaskListAdapter(getActivity(), itemLayoutId, new ArrayList<>());
        // Set adapter item click listener
        adapter.addItemClickListener(this::onAdapterItemClicked);
        adapter.addSelectionModeListener(this::onSelectionModeChanged);
        // Obtain empty view reference
        View emptyView = v.findViewById(R.id.empty_view);
        setEmptyView(emptyView);
        mFab = v.findViewById(R.id.btn_fab);
        mFab.setOnClickListener((view) -> {
            Logger.d(LOG_TAG, "Floating Action Button is clicked to add new task");
            // Launch add task activity
            Intent i = new Intent(this.getActivity(), AddTaskActivity.class);
            startActivityForResult(i, NEW_TASK_REQUEST_CODE);
        });
        // Register Event Bus
        EventBus.getDefault().register(this);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister Event Bus
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskDeleted(TaskDeleted event) {
        Utilities.showTaskDeleted(getActivity(), 1);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTasksDeleted(TasksDeleted event) {
        Utilities.showTaskDeleted(getActivity(), event.getModels().size());
    }

    /**
     * Determines which layout to use for item views in the list. This determination is based on the
     * type of fragment and the layout type selected by the user.
     */
    private void determineItemLayoutResource() {
        if (fragmentType == MainActivity.FRAGMENT_ONE_TIME || fragmentType == MainActivity.FRAGMENT_TODAY) {
            itemLayoutId = R.layout.today_task_list_item;
            if (hasGridLayout()) itemLayoutId = R.layout.today_task_grid_item;
        } else {
            itemLayoutId = R.layout.task_list_item;
            if (hasGridLayout()) itemLayoutId = R.layout.task_grid_item;
        }
    }

    /**
     * This method is used to set layout manager for the recycler view depending upon the
     * preferences of user. User can either prefer Linear Layout or Grid Layout.
     * Note: Only call this method in OnCreateView or when it is ensured that recyclerview is created
     * and added to root view.
     */
    private void setUserPreferredLayoutManager() {
        switch (fragmentType) {
            case MainActivity.FRAGMENT_TODAY:
                mLayoutType = PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getInt(getString(R.string.pref_whats_today_layout_type), LAYOUT_TYPE_LIST);
                break;
            case MainActivity.FRAGMENT_ROUTINE:
                mLayoutType = PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getInt(getString(R.string.pref_routine_tasks_layout_type), LAYOUT_TYPE_LIST);
                break;
            case MainActivity.FRAGMENT_ONE_TIME:
                mLayoutType = PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getInt(getString(R.string.pref_one_time_tasks_layout_type), LAYOUT_TYPE_LIST);
        }
        updateLayoutManager();
    }

    private void startActionMode() {
        // Start action mode
        actionMode = new ActionbarModeCallback(
                getActivity(),
                getActivity().getMenuInflater(),
                adapter
        );
        actionMode.setActionModeStateListener(new ActionbarModeCallback.ActionModeStateListener() {
            @Override
            public void onCreateActionMode() {
                hideFab();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onDestroyActionMode() {
                showFab();
                if (adapter.isSelectionModeActive()) {
                    adapter.clearSelection();
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });
        getActivity().startActionMode(actionMode);
    }

    private void hideFab() {
        mFab.animate()
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .scaleX(0.3f)
                .scaleY(0.3f)
                .withEndAction(() -> {
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
                    fabAnchorId = params.getAnchorId();
                    params.setAnchorId(View.NO_ID);
                    mFab.setLayoutParams(params);
                    mFab.setAlpha(0f);
                    mFab.setVisibility(View.GONE);
                }).start();
    }

    private void showFab() {
        mFab.setVisibility(View.VISIBLE);
        mFab.animate()
                .alpha(1f)
                .setDuration(400)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    mFab.setAlpha(1f);
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
                    params.setAnchorId(fabAnchorId);
                    mFab.setLayoutParams(params);
                }).start();
    }

    /**
     * Updates the layout manager of the recycler view according to the current `mLayoutType`.
     */
    private void updateLayoutManager() {
        if (mLayoutType == LAYOUT_TYPE_LIST) {
            setLayoutManager(new LinearLayoutManager(getActivity()));
        } else if (mLayoutType == LAYOUT_TYPE_GRID) {
            setLayoutManager(new GridLayoutManager(getActivity(), GRID_LAYOUT_COLUMNS));
        }
    }

    public boolean hasListLayout() {
        return mLayoutType == LAYOUT_TYPE_LIST;
    }

    public boolean hasGridLayout() {
        return mLayoutType == LAYOUT_TYPE_GRID;
    }

    /**
     * This method is invoked when an item in the RecyclerView is clicked.
     *
     * @param vh viewholder associated with clicked item
     * @param v  view associated with clicked item
     */
    public void onAdapterItemClicked(TaskViewHolder vh, View v, int position, boolean selectionModeActive) {
        if (selectionModeActive) {
            actionMode.refreshState();
        } else {
            TaskModel taskToEdit = adapter.getTaskAtPosition(position);
            // Start activity to edit selected adapter task.
            Intent i = new Intent(getActivity(), EditTaskActivity.class);
            i.putExtra(EditTaskActivity.KEY_TASK_PARCEL, taskToEdit);

            ActivityOptionsCompat options = ActivityOptionsCompat.makeClipRevealAnimation(
                    v,
                    0,
                    0,
                    v.getWidth(),
                    v.getHeight());
            startActivityForResult(i, TaskListFragment.EDIT_TASK_REQUEST_CODE, options.toBundle());
        }
    }

    public void onSelectionModeChanged(boolean isModeActive) {
        if (isModeActive) startActionMode();
        else if (actionMode != null) actionMode.finishActionMode();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter.getSelectedItemCount() > 0) {
            outState.putIntegerArrayList(KEY_SELECTED_POSITIONS, adapter.getSelectedPositions());
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            selectedPositions = savedInstanceState.getIntegerArrayList(KEY_SELECTED_POSITIONS);
        }
    }

    @NonNull
    @Override
    public Loader<ArrayList<TaskModel>> onCreateLoader(int id, @Nullable Bundle args) {
        TasksLoader.TasksType tasksType;
        if (id == MainActivity.FRAGMENT_ROUTINE) {
            tasksType = TasksLoader.TasksType.REPEATING_TASKS;
        } else if (id == MainActivity.FRAGMENT_ONE_TIME) {
            tasksType = TasksLoader.TasksType.NO_REPEAT_TASKS;
        } else {
            tasksType = TasksLoader.TasksType.TODAY_TASKS;
        }
        Logger.d(LOG_TAG, "Creating a new TasksLoader");
        return new TasksLoader(getContext(), tasksType);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<TaskModel>> loader, ArrayList<TaskModel> taskData) {
        Logger.d(LOG_TAG, "Loader finished loading tasks");
        adapter.setTasksData(taskData);
        if (getAdapter() == null) {
            setAdapter(adapter);
            if (taskData.size() > 0) fadeInRecyclerView();
            else fadeInEmptyView();
        }

        // Restore selected items
        if (selectedPositions != null) {
            adapter.selectPositions(selectedPositions);
            selectedPositions = null;
            if (adapter.getSelectedItemCount() > 0) startActionMode();
        }
        if (adapter.getItemCount() > 0)
            getView().post(() -> {
                switch (fragmentType) {
                    case MainActivity.FRAGMENT_TODAY:
                        Tutorials.showTodayTaskListItemTutorial(getActivity(), getActivity().findViewById(R.id.task_list_item));
                        break;
                    case MainActivity.FRAGMENT_ROUTINE:
                        Tutorials.showRoutineTasksListItemTutorial(getActivity(), getActivity().findViewById(R.id.task_list_item));
                        break;
                    case MainActivity.FRAGMENT_ONE_TIME:
                        Tutorials.showOneTimeTaskListItemTutorial(getActivity(), getActivity().findViewById(R.id.task_list_item));
                        break;
                }
            });

        Logger.d(LOG_TAG, "onLoadFinished() - RecyclerView Adapter is updated");
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<TaskModel>> loader) {
        adapter.clearTasksData();
    }
}