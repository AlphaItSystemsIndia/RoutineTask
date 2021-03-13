package com.cod3rboy.routinetask.events;

import com.cod3rboy.routinetask.database.models.ReminderModel;

public class ReminderLoaded extends ReminderEvent {
    private ReminderModel model;

    public ReminderLoaded(ReminderModel model) {
        this.model = model;
    }

    public ReminderModel getModel() {
        return model;
    }
}
