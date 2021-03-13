package com.cod3rboy.routinetask;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.logging.Logger;

import java.util.ArrayList;

/**
 * This is a Adapter associated with the RecyclerView in the RecyclerViewFragment.
 * This adapter is responsible for creating item views, their associated viewholders, binding data
 * with widgets in viewholder of item and most importantly hooking(registering) the RecyclerViewFragment with the
 * TaskViewHolder so that RecyclerViewFragment can receive item click and item long click events.
 */
public class TaskListAdapter extends RecyclerView.Adapter<TaskViewHolder> {
    private static final String LOG_TAG = TaskListAdapter.class.getSimpleName();

    // Tasks List holding data for the list items in RecyclerView
    private ArrayList<TaskModel> tasksData;

    private Context context;
    private ArrayList<ItemClickListener> mClickListeners;
    private ArrayList<SelectionModeListener> mSelectionModeListeners;
    private int itemLayoutId;
    private ArrayList<Integer> selectedPositions;
    private boolean selectionModeActive;

    /**
     * Callback Interface that click listeners must register
     */
    public interface ItemClickListener {
        void onItemClick(TaskViewHolder vh, View v, int position, boolean selectionModeActive);
    }

    /**
     * Callback Interface that selection mode change listeners must register
     */
    public interface SelectionModeListener {
        void onModeChanged(boolean isModeActive);
    }

    /**
     * Constructor to create an instance of adapter
     *
     * @param context   context object
     * @param tasksData list holding tasks for the list items in RecyclerView
     */
    public TaskListAdapter(Context context, @LayoutRes int itemLayoutId, ArrayList<TaskModel> tasksData) {
        this.context = context;
        this.itemLayoutId = itemLayoutId;
        this.tasksData = tasksData;
        this.mClickListeners = new ArrayList<>();
        this.mSelectionModeListeners = new ArrayList<>();
        this.selectedPositions = new ArrayList<>();
        this.selectionModeActive = false;
    }

    public void setItemLayoutResourceId(@LayoutRes int itemLayoutId) {
        this.itemLayoutId = itemLayoutId;
    }

    public void addItemClickListener(ItemClickListener listener) {
        if (listener != null) mClickListeners.add(listener);
    }

    public void addSelectionModeListener(SelectionModeListener listener) {
        if (listener != null) mSelectionModeListeners.add(listener);
    }

    public void removeItemClickListener(ItemClickListener listener) {
        if (listener != null) mClickListeners.remove(listener);
    }

    public void removeSelectionModeListener(SelectionModeListener listener) {
        if (listener != null) mSelectionModeListeners.remove(listener);
    }

    private void notifyClickListeners(TaskViewHolder vh, View v, int position, boolean selectionModeActive) {
        for (ItemClickListener listener : mClickListeners)
            listener.onItemClick(vh, v, position, selectionModeActive);
    }

    private void notifySelectionModeListeners(boolean isModeActive) {
        for (SelectionModeListener listener : mSelectionModeListeners) {
            listener.onModeChanged(isModeActive);
        }
    }

    /**
     * This method is called when an List Item is created for first time.
     * Here we inflate list item view and create a TaskViewHolder object tagged with that list item.
     *
     * @param viewGroup parent view
     * @param viewType  use to support multiple views
     * @return TaskViewHolder object tagged with inflated list item
     */
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View root = LayoutInflater.from(context).inflate(itemLayoutId, viewGroup, false);
        TaskViewHolder viewHolder = new TaskViewHolder(context, root);
        // Register listener with Checkbox
        viewHolder.setCheckboxListener((view, isChecked) -> {
            int position = viewHolder.getAdapterPosition();
            TaskModel taskAtPosition = tasksData.get(position);
            if (isChecked) {
                Logger.d(LOG_TAG, String.format("Routine task with id %d is marked as completed", taskAtPosition.getId()));
                TaskModel.markAsComplete(taskAtPosition);
            } else {
                Logger.d(LOG_TAG, String.format("Routine task with id %d is marked as not completed", taskAtPosition.getId()));
                TaskModel.markAsPending(taskAtPosition);
            }
        });

        // Register Click Listener
        root.setOnClickListener(v -> {
            int position = viewHolder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            boolean wasSelectionModeActive = selectionModeActive;
            if (selectionModeActive) {
                Integer clickedPosition = position;
                if (selectedPositions.contains(clickedPosition)) {
                    selectedPositions.remove(clickedPosition);
                    Logger.d(LOG_TAG, String.format("Deselecting long clicked item at position %d ", clickedPosition));
                    selectionModeActive = !selectedPositions.isEmpty();
                    if (!selectionModeActive) notifySelectionModeListeners(false);
                } else {
                    selectedPositions.add(clickedPosition);
                    Logger.d(LOG_TAG, String.format("Selecting long clicked item at position %d ", clickedPosition));
                }
                notifyItemChanged(position);
            }
            // Notify Click listeners that list item view is clicked
            notifyClickListeners(viewHolder, v, position, wasSelectionModeActive);
        });
        // Register LongClick Listener
        root.setOnLongClickListener(v -> {
            int position = viewHolder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (!selectionModeActive) {
                selectionModeActive = true;
                Integer longClickedPosition = position;
                Logger.d(LOG_TAG, String.format("Selecting long clicked item at position %d ", longClickedPosition));
                selectedPositions.add(longClickedPosition);
                notifyItemChanged(position);
                // Notify Selection Mode listeners that list item view is long clicked
                notifySelectionModeListeners(true);
                return true;
            }
            return false;
        });
        return viewHolder;
    }

    /**
     * This method is called to bind the data with a list item view.
     *
     * @param taskViewHolder TaskViewHolder associated with list item view
     * @param position       position of list item view
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder taskViewHolder, int position) {
        TaskModel taskAtPosition = tasksData.get(position);
        taskViewHolder.bindData(context, taskAtPosition, selectedPositions.contains(position));
        if (selectionModeActive) taskViewHolder.hideCheckbox();
        else taskViewHolder.showCheckbox();
    }

    /**
     * This method is called to know about total no of data items so that item view for each data item can be requested from adapter.
     *
     * @return
     */
    @Override
    public int getItemCount() {
        return tasksData.size();
    }


    public TaskModel getTaskAtPosition(int position) {
        return tasksData.get(position);
    }

    public ArrayList<Integer> getSelectedPositions() {
        return new ArrayList<>(selectedPositions);
    }

    public int getSelectedItemCount() {
        return selectedPositions.size();
    }

    /**
     * This method is used to set data for list items with new data.
     *
     * @param tasksData new task data to use
     */
    public void setTasksData(ArrayList<TaskModel> tasksData) {
        this.tasksData.clear();
        this.tasksData.addAll(tasksData);
        this.selectedPositions.clear();
        this.selectionModeActive = false;
        notifyDataSetChanged();
    }

    public void clearTasksData() {
        this.tasksData.clear();
        this.selectedPositions.clear();
        this.selectionModeActive = false;
        notifyDataSetChanged();
    }

    public void selectAllItems() {
        selectedPositions.clear();
        selectionModeActive = true;
        for (int i = 0; i < getItemCount(); i++) selectedPositions.add(i);
        notifyDataSetChanged();
    }

    public void selectPositions(ArrayList<Integer> positions) {
        selectedPositions.clear();
        selectionModeActive = true;
        selectedPositions.addAll(positions);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPositions.clear();
        selectionModeActive = false;
        notifyDataSetChanged();
    }

    public boolean isSelectionModeActive() {
        return selectionModeActive;
    }
}