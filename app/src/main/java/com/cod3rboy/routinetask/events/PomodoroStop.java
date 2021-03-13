package com.cod3rboy.routinetask.events;

public class PomodoroStop {
    public enum Status{FAILED, SUCCESS}
    private Status status;
    public PomodoroStop(Status status){
        this.status = status;
    }
    public Status getStatus(){
        return this.status;
    }
}
