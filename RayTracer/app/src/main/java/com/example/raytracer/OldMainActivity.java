package com.example.raytracer;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import android.app.Activity;
import android.app.ActivityManager;

// Zum Abfragen der GL Version hinzugefügt
import android.content.Context;
import android.content.pm.ConfigurationInfo;

// Für Logcat
import android.util.Log;
// Um die DPI des Displays zu bekommen
import android.util.DisplayMetrics;

import android.view.Display;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import Util.StateManager;

// Auf Top level import geändert
public class OldMainActivity extends Activity {

    final Renderer renderer = new Renderer(this);
    private static final String TAG = "MyActivity";

    // private GLSurfaceView glSurfaceView;
    private RaytracerSurfaceView glSurfaceView;

    private boolean rendererSet = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Display display = getWindowManager().getDefaultDisplay();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        // glSurfaceView = new GLSurfaceView(this);

        if (!detectOpenGLES31()) {
            Log.e("Raytracer", "OpenGL ES 3.1 not supported on device.  Exiting...");
            finish();
        }

        glSurfaceView = new RaytracerSurfaceView(this, displayMetrics);
        rendererSet = true;

        // Request an OPENGL ES 3.1 compatible context
        //glSurfaceView.setEGLContextClientVersion(3);

        // Fullscreen-Mode
        //glSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if(StateManager.isLoaded != true) {
            StateManager.load(0, this);
        }

        // Assign the renderer
        // glSurfaceView.setRenderer(renderer);
        // rendererSet = true;
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            // multiple coordinates for multitouch
            float normalizedX1;
            float normalizedY1;
            float normalizedX2;
            float normalizedY2;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent != null) {
                    // Convert touch coordinates into normalized device
                    // coordinates, keeping in mind that Android's
                    // Y coordinates are inverted
                    float pointerCount = motionEvent.getPointerCount();
                    int p = motionEvent.getActionIndex();
                    int m = motionEvent.getActionMasked();

                    Log.i("Motion",p + " " + pointerCount + " " + m);

                    if(pointerCount == 1) {
                        normalizedX1 = (motionEvent.getX() / (float) view.getWidth()) * 2 - 1;
                        normalizedY1 = -((motionEvent.getY() / (float) view.getHeight()) * 2 - 1);
                        normalizedX2 = -2;
                        normalizedY2 = -2;
                    } else if (pointerCount == 2) {
                        Log.i(TAG, "" + pointerCount + " " + motionEvent.getX());
                        normalizedX1 = (motionEvent.getX() / (float) view.getWidth()) * 2 - 1;
                        normalizedY1 = -((motionEvent.getY() / (float) view.getHeight()) * 2 - 1);
                        normalizedX2 = (motionEvent.getX(1) / (float) view.getWidth()) * 2 - 1;
                        normalizedY2 = -((motionEvent.getY(1) / (float) view.getHeight()) * 2 - 1);
                    }

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        glSurfaceView.queueEvent(() -> renderer.handleTouchPress(normalizedX1, normalizedY1));
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        glSurfaceView.queueEvent(() -> {
//                              Log.i(TAG, "" + normalizedX1 + " " + normalizedX2);
//                              Log.i(TAG, "" + normalizedY1 + " " + normalizedY2);
//                            renderer.handleTouchDrag(normalizedX1, normalizedY1, normalizedX2, normalizedY2);
                        });
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        glSurfaceView.queueEvent(() -> renderer.handleTouchRelease(normalizedX1, normalizedY1));
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        setContentView(glSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean detectOpenGLES31()
    {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= 0x00030001);
    }
}
