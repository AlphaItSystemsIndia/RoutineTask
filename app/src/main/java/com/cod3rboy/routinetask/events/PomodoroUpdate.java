package com.cod3rboy.routinetask.events;

public class PomodoroUpdate {
    private int secsLeft;
    public PomodoroUpdate(int secsLeft){
        this.secsLeft = secsLeft;
    }
    public int getSecsLeft(){
        return secsLeft;
    }
}
