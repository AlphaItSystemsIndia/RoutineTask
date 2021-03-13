package com.cod3rboy.routinetask;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

/**
 * This class defines the callback methods used in the action mode
 * which is started on the TaskListFragment.
 * An instance of this class is passed in the startActionMode method (in fragment/activity) which starts the
 * action mode and invoke the callback methods defined in this class.
 * This class is used to implement the edit and delete actions for the selected adapter list item
 * in TaskListFragment using Context Action Bar (overlay action bar on existing one).
 */
public class ActionbarModeCallback implements ActionMode.Callback {
    private static final String LOG_TAG = ActionbarModeCallback.class.getSimpleName();

    public interface ActionModeStateListener {
        void onCreateActionMode();

        void onDestroyActionMode();
    }

    private TaskListAdapter adapter;
    private ActionMode actionMode;
    private MenuInflater menuInflater;
    private Context context;
    private ActionModeStateListener listener;

    /**
     * Constructor
     *
     * @param context      Context Object
     * @param menuInflater MenuInflater attached to host activity
     * @param adapter      TaskListAdapter instance
     */
    public ActionbarModeCallback(Context context, MenuInflater menuInflater, TaskListAdapter adapter) {
        this.context = context;
        this.menuInflater = menuInflater;
        this.adapter = adapter;
    }

    /**
     * This method is called to create context action bar.
     * Menu supplied is used to populate action buttons in action mode.
     * Here we inflate and return the options menu to display in context action bar and set flag to active.
     *
     * @param mode Action Mode
     * @param menu menu used to populate action buttons
     * @return true if the action mode should be created, false if entering this
     * mode should be aborted.
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        menuInflater.inflate(R.menu.options_edit, menu);
        this.actionMode = mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionMode.setType(ActionMode.TYPE_FLOATING);
        }
        updateTitle();
        if (listener != null) listener.onCreateActionMode();
        return true;
    }

    /**
     * Called to refresh an action mode's action menu whenever it is invalidated.
     * Here it is defined to complete formality of interface.
     *
     * @param mode ActionMode being prepared
     * @param menu Menu used to populate action buttons
     * @return true if the menu or action mode was updated, false otherwise.
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean updated = false;
        // Update context action menu here
        // Remove select all action if all items are selected
        if (adapter.getSelectedItemCount() < adapter.getItemCount()) {
            if (menu.findItem(R.id.action_select_all) == null) {
                menu.clear();
                menuInflater.inflate(R.menu.options_edit, menu);
                updated = true;
            }
        } else {
            menu.removeItem(R.id.action_select_all);
            updated = true;
        }
        return updated;
    }


    private void updateTitle() {
        int selectedItems = adapter.getSelectedItemCount();
        String format = (selectedItems > 1) ? context.getString(R.string.cab_multiple_select)
                : context.getString(R.string.cab_single_select);
        this.actionMode.setTitle(context.getString(R.string.cab_title));
        this.actionMode.setSubtitle(String.format(format, selectedItems));
        actionMode.invalidate();
    }

    public void refreshState() {
        updateTitle();
        if (adapter.getSelectedItemCount() == 0) finishActionMode();
    }

    public void finishActionMode() {
        if (actionMode != null) actionMode.finish();
    }

    public void setActionModeStateListener(ActionModeStateListener listener) {
        this.listener = listener;
    }

    /**
     * Called to report a user click on an action button and take
     * appropriate actions accordingly.
     *
     * @param mode The current ActionMode
     * @param item The item that was clicked
     * @return true if this callback handled the event, false if the standard MenuItem
     * invocation should continue.
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int selectedItemCount = adapter.getSelectedItemCount();
        switch (item.getItemId()) {
            case R.id.action_delete: // If edit action was clicked
                Logger.d(LOG_TAG, "onActionItemClicked() - Selected delete action from action menu");
                // Show delete dialog
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.del_sel_dialog_title)
                        .setMessage(String.format(
                                context.getResources().getString(R.string.del_sel_dialog_message),
                                selectedItemCount, (selectedItemCount > 1) ? "s" : ""))
                        .setNeutralButton(R.string.del_sel_dialog_action_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.del_sel_dialog_action_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (selectedItemCount == 1) {
                                    int taskPosition = adapter.getSelectedPositions().get(0);
                                    TaskModel taskToDelete = adapter.getTaskAtPosition(taskPosition);
                                    taskToDelete.delete(true);
                                } else {
                                    // Bulk delete all selected adapter items
                                    ArrayList<Integer> selectedPositions = adapter.getSelectedPositions();
                                    ArrayList<TaskModel> tasksToDelete = new ArrayList<>();
                                    for (int position : selectedPositions) {
                                        tasksToDelete.add(adapter.getTaskAtPosition(position));
                                    }
                                    TaskModel.deleteAsync(tasksToDelete);
                                }
                                // Dismiss the action mode
                                finishActionMode();
                                dialog.dismiss();
                            }
                        }).show();
                break;
            case R.id.action_select_all:
                Logger.d(LOG_TAG, "onActionItemClicked() - Selected Select All action from action menu");
                adapter.selectAllItems();
                updateTitle();
                break;
            case R.id.action_duplicate:
                Logger.d(LOG_TAG, "onActionItemClicked() - Selected duplicate action from action menu");
                // Show duplicate dialog
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.duplicate_dialog_title)
                        .setMessage(String.format(
                                context.getResources().getString(R.string.duplicate_dialog_message),
                                selectedItemCount, (selectedItemCount > 1) ? "s" : ""))
                        .setNeutralButton(R.string.duplicate_dialog_action_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.duplicate_dialog_action_duplicate, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Make duplicate of all selected tasks
                                ArrayList<Integer> selectedPositions = adapter.getSelectedPositions();
                                ArrayList<TaskModel> tasksToInsert = new ArrayList<>();
                                for (int position : selectedPositions)
                                    tasksToInsert.add(new TaskModel(adapter.getTaskAtPosition(position)));
                                TaskModel.insertAsync(tasksToInsert, true);
                                // Dismiss the action mode
                                finishActionMode();
                                dialog.dismiss();
                            }
                        }).show();
                break;
        }
        return true;
    }

    /**
     * Called when an action mode is about to be exited/dismissed and destroyed.
     *
     * @param mode The current ActionMode being destroyed
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (listener != null) listener.onDestroyActionMode();
    }

}
