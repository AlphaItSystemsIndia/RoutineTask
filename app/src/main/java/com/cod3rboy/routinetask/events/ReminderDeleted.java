package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.ReminderModel;

public class ReminderDeleted extends ReminderEvent {
    private ReminderModel model;

    public ReminderDeleted(ReminderModel model) {
        this.model = model;
    }

    public ReminderModel getModel() {
        return model;
    }
}
