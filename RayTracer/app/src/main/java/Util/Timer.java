package Util;

import static Util.Constants.TARGET_FPS;

public class Timer {

    // Times in milli seconds
    private double targetWaitTime;
    private double timeLeft;

    // Flags
    private boolean running;
    private boolean reportedBack;
    private boolean neverBeenStarted;

    public Timer(double timeInMil) {
        this.targetWaitTime = timeInMil;
        running = false;
        reportedBack = false;
        neverBeenStarted = true;
        StateManager.registerTimer(this);
    }

    // Function (re-)starts the timer
    public void start() {
        timeLeft = targetWaitTime;
        running = true;
        reportedBack = false;
        neverBeenStarted = false;
    }

    // Function stops the timer
    public void stop() {
        running = false;
    }

    // Function returns true when targetWaitTime has passed
    public boolean hasFinished() {
        if(!running && !reportedBack && !neverBeenStarted) {
            reportedBack = true;
            return true;
        } else {
            return false;
        }
    }

    // The GameStateManager automatically updates all registered timers every frame
    public void update() {
        if(running) {
            if (timeLeft > 0.0) {
                timeLeft -= 1.0 / TARGET_FPS * 1000.0; // calculates how long one frame should take in milli seconds
            } else {
                stop();
            }
        }
    }

    public boolean hasNeverBeenStarted(){
        return neverBeenStarted;
    }

    public boolean isRunning() {
        return running;
    }
}
