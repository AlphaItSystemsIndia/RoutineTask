package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.TaskModel;

public class TaskDeleted extends TaskEvent {
    private TaskModel model;

    public TaskDeleted(TaskModel model) {
        this.model = model;
    }

    public TaskModel getModel() {
        return model;
    }
}
