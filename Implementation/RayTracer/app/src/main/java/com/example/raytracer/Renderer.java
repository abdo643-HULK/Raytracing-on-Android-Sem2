package com.example.raytracer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import Util.Direction;
import Util.StateManager;

import static Util.Constants.NANOS_PER_SEC;
import static Util.Constants.TARGET_FPS;

/**
 * Created by Andreas on 22.04.2020.
 */

public class Renderer implements GLSurfaceView.Renderer {

    private static final String TAG = "Raytracer";

    private Context context;

    private long overTimeInNano;
    private int mFPS = 0;
    private long mLastTime;

    public Renderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        StateManager.getActiveScene().onSurfaceCreated(context);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        StateManager.loadDimensions(width, height);

        StateManager.getActiveScene().onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        long startTimeInNano = System.nanoTime();

        StateManager.updateAllTimers();
        StateManager.getActiveScene().onDrawFrame();

        long endTimeInNano = System.nanoTime();
        long timeElapsedInNano = endTimeInNano-startTimeInNano;

        mFPS++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastTime >= 1000) {
            mFPS = 0;
            mLastTime = currentTime;
        }


        if((1.0/TARGET_FPS)>timeElapsedInNano/NANOS_PER_SEC) {
            long waitTimeInNano = (long)((1.0/TARGET_FPS*NANOS_PER_SEC)-(timeElapsedInNano));

            try {
                if(waitTimeInNano<NANOS_PER_SEC && overTimeInNano<NANOS_PER_SEC && waitTimeInNano>overTimeInNano) {
                    Thread.sleep((waitTimeInNano - overTimeInNano)/1000000, (int)((waitTimeInNano - overTimeInNano)%1000000));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            overTimeInNano = 0;
        } else {
            overTimeInNano = (long)(((timeElapsedInNano) - (1.0/TARGET_FPS)*NANOS_PER_SEC));
        }
    }

    public int getFPS() {
        return mFPS;
    }

    public void handleScale(float scaleFactor){
        StateManager.getActiveScene().handleScale(scaleFactor);
    }

    public void handleRotation(float angle, int x, int y){
        StateManager.getActiveScene().handleRotation(angle, x, y);
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        StateManager.getActiveScene().handleTouchPress(normalizedX, normalizedY);
    }

    public void handleTouchDrag(Direction direction) {
        StateManager.getActiveScene().handleTouchDrag(direction);
    }
//    public void handleTouchDrag(float normalizedX, float normalizedY, float normalizedX1, float normalizedY1) {
//        StateManager.getActiveScene().handleTouchDrag(normalizedX, normalizedY, normalizedX1, normalizedY1);
//    }

    public void handleTouchRelease(float normalizedX, float normalizedY) {
        StateManager.getActiveScene().handleTouchRelease(normalizedX, normalizedY);
    }
}
