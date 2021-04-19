package com.example.raytracer;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import Util.StateManager;

public class MainActivity extends android.app.Activity {

    final Renderer renderer = new Renderer(this);

    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);

        // Request an OPENGL ES 3.1 compatible context
        glSurfaceView.setEGLContextClientVersion(3);

        // Fullscreen-Mode
        glSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if(StateManager.isLoaded != true) {
            StateManager.load(0, this);
        }

        // Assign the renderer
        glSurfaceView.setRenderer(renderer);
        rendererSet = true;

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

                    if(pointerCount == 1) {
                        normalizedX1 = (motionEvent.getX() / (float) view.getWidth()) * 2 - 1;
                        normalizedY1 = -((motionEvent.getY() / (float) view.getHeight()) * 2 - 1);
                        normalizedX2 = -2;
                        normalizedY2 = -2;
                    } else if (pointerCount == 2) {
                        normalizedX1 = (motionEvent.getX() / (float) view.getWidth()) * 2 - 1;
                        normalizedY1 = -((motionEvent.getY() / (float) view.getHeight()) * 2 - 1);
                        normalizedX2 = (motionEvent.getX(1) / (float) view.getWidth()) * 2 - 1;
                        normalizedY2 = -((motionEvent.getY(1) / (float) view.getHeight()) * 2 - 1);
                    }

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                renderer.handleTouchPress(normalizedX1, normalizedY1);
                            }
                        });
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                renderer.handleTouchDrag(normalizedX1, normalizedY1, normalizedX2, normalizedY2);
                            }
                        });
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                renderer.handleTouchRelease(normalizedX1, normalizedY1);
                            }
                        });
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
}
