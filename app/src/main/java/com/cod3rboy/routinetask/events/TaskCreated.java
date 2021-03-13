package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.TaskModel;

public class TaskCreated extends TaskEvent {
    private TaskModel model;

    public TaskCreated(TaskModel model) {
        this.model = model;
    }

    public TaskModel getModel() {
        return model;
    }
}
