package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.TaskModel;

public class TaskStatusChanged extends TaskEvent {
    private boolean completed;
    private TaskModel model;

    public TaskStatusChanged(TaskModel model, boolean completed) {
        this.model = model;
        this.completed = completed;
    }

    public TaskModel getModel() {
        return model;
    }

    public boolean isCompleted() {
        return completed;
    }
}
