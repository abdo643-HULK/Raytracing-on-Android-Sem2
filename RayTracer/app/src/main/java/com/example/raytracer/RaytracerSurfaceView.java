package com.example.raytracer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;

import android.util.Log;
import android.util.DisplayMetrics;

import android.view.View;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import Util.CurrentEvent;
import Util.Direction;

//import com.example.raytracer.Renderer;

@SuppressLint("ViewConstructor")
public class RaytracerSurfaceView extends GLSurfaceView {
    private static final String TAG = "Raytracer";
    private static final int INVALID_POINTER_ID = -1;

    private final ScaleGestureDetector scaleDetector;
    private final float mMinScale = 0.5f;
    private final float mMaxScale = 3f;
    private final float mDensity;
    private final float mThreshold;
    private CurrentEvent mMode = CurrentEvent.NONE;
    // multiple coordinates for multitouch
    private float mScaleFactor = 1f;
    private com.example.raytracer.Renderer mRenderer;

    private long mFPS;
    private float mPreviousX;
    private float mPreviousY;
    private float mDeltaX;
    private float mDeltaY;
    private float mPreviousDeltaX;
    private float mPreviousDeltaY;
    private int mActivePointerId;

    RaytracerSurfaceView(Context ctx, DisplayMetrics metrics) {
        super(ctx);
        // Request an OPENGL ES 3.1 compatible context
        setEGLContextClientVersion(3);
        // Fullscreen-Mode
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        // Assign the renderer
        setRenderer(mRenderer = new com.example.raytracer.Renderer(ctx));

        scaleDetector = new ScaleGestureDetector(ctx, new ScaleListener());
        mDensity = metrics.density;
        mThreshold = metrics.density * 5f;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
//        scaleDetector.onTouchEvent(e);

        int pointerCount = e.getPointerCount();
        // int i = e.getActionIndex();
        int p = e.getActionIndex();
        int m = e.getActionMasked();

        if (pointerCount == 1) {
            mMode = CurrentEvent.PAN;
        } else {
            mMode = CurrentEvent.ROTATE;
        }

//        Log.i(TAG, "first: " + pointerCount);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
//                mActivePointerId = e.getPointerId(0);
//                mActivePointerId2 = e.getPointerId(1);
//                Log.i(TAG, "Action down: " + e.getPointerCount());
                mMode = CurrentEvent.PAN;
                mPreviousX = e.getX();
                mPreviousY = e.getY();
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                // Fired when 1 Pointer is on the screen and one gets added or 2 pointer
                mMode = CurrentEvent.ROTATE;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!scaleDetector.isInProgress()) {
                    final float x = e.getX(0);
                    final float y = e.getY(0);
//                    Log.i(TAG, "c: " + x + " " + y);

                    final float deltaX = (x - mPreviousX); //  / mDensity / 2f;
                    final float deltaY = (y - mPreviousY); // / mDensity / 2f;
                    mPreviousX = x;
                    mPreviousY = y;

                    final boolean sideMov = Math.abs(deltaX) > Math.abs(deltaY);
                    if (mMode == CurrentEvent.PAN) {
                        if (sideMov) {
                            // Right, Left
                            final Direction d = deltaX > 0 ? Direction.LEFT : Direction.RIGHT;
                            queueEvent(() -> mRenderer.handleTouchDrag(d));
                        } else {
                            // Up, Down
                            queueEvent(() -> {
                                final Direction d = deltaY > 0 ? Direction.UP : Direction.DOWN;
                                mRenderer.handleTouchDrag(d);
                            });
                        }
                        break;
                    }

                    if (mMode == CurrentEvent.ROTATE) {
                        if (sideMov) {
                            // Right, Left
                            final float angle = deltaX > 0 ? 5f : -5f;
                            queueEvent(() -> mRenderer.handleRotation(angle, 0, 1));
                        } else {
                            // Up, Down
                            queueEvent(() -> {
                                final float angle = deltaY > 0 ? 5f : -5f;
                                mRenderer.handleRotation(angle, 1, 0);
                            });
                        }
                        break;
                    }
                    // view.invalidate();
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                // Fired when 2 Pointer are on the screen and one gets removed
                mMode = CurrentEvent.NONE;
//                Log.i(TAG, "Pointer up: " + e.getPointerCount());
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                mMode = CurrentEvent.NONE;
                break;
            }
        }
        return true;
    }

    public void scale(float scaleFactor) {
        queueEvent(() -> mRenderer.handleScale(scaleFactor));
    }

    @Override
    public void onPause() {
        //super.onPause();
    }

    @Override
    public void onResume() {
        // super.onResume();
    }

    public com.example.raytracer.Renderer getRenderer() {
        return mRenderer;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        float scaleFocusX = 0;
        float scaleFocusY = 0;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector arg0) {
            mMode = CurrentEvent.SCALE;
//            Log.i(TAG, "scale:  ");
            scaleFocusX = arg0.getFocusX();
            scaleFocusY = arg0.getFocusY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(mMinScale, Math.min(mScaleFactor, mMaxScale));
            queueEvent(() -> mRenderer.handleScale(mScaleFactor));
            // requestRender();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector arg0) {
            scaleFocusX = 0;
            scaleFocusY = 0;
        }
    }
}
