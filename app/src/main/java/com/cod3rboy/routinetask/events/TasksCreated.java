package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.TaskModel;

import java.util.ArrayList;

public class TasksCreated extends TaskEvent {
    private ArrayList<TaskModel> models;

    public TasksCreated(ArrayList<TaskModel> models) {
        this.models = models;
    }

    public ArrayList<TaskModel> getModels() {
        return models;
    }
}
