package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.ReminderModel;

public class ReminderCreated extends ReminderEvent {
    private ReminderModel model;

    public ReminderCreated(ReminderModel model) {
        this.model = model;
    }

    public ReminderModel getModel() {
        return model;
    }
}
