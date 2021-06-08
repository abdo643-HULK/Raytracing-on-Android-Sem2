package com.example.raytracer;

import android.annotation.SuppressLint;
import android.content.Context;

import android.view.DragEvent;
import android.view.View;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View.OnTouchListener;
import android.view.View.OnDragListener;

import Util.CurrentEvent;


public class ActivityHandler implements OnTouchListener, OnDragListener {

    private static final int INVALID_POINTER_ID = -1;

    private final ScaleGestureDetector scaleDetector;
    private final float mMinScale = 0.5f;
    private final float mMaxScale = 3f;
    private final float mDensity;

    private CurrentEvent mMode = CurrentEvent.NONE;

    private float mScaleFactor = 1f;
    private float mPreviousX;
    private float mPreviousY;
    private int mActivePointerId;

    public ActivityHandler(Context ctx, float density) {
        // gestureDetector = new GestureDetector(ctx, new GestureListener());
        scaleDetector = new ScaleGestureDetector(ctx, new ScaleListener());
        mDensity = density;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public float getMaxScale() {
        return mMaxScale;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        // gestureDetector.onTouchEvent(e);
        int pointerCount = e.getPointerCount();
        int index = e.getActionIndex();

        if (pointerCount == 1) {
            mMode = CurrentEvent.PAN;
        }
        else {
            mMode = CurrentEvent.ROTATE;
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // mActivePointerId = e.getPointerId(0);
                // mActivePointerId2 = e.getPointerId(1);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!scaleDetector.isInProgress()) {
                    if (mMode != CurrentEvent.SCALE) {
                        final float x = e.getX();
                        final float y = e.getY();
                        final float deltaX = (x - mPreviousX) / mDensity / 2f;
                        final float deltaY = (y - mPreviousY) / mDensity / 2f;
                        mPreviousX = x;
                        mPreviousY = y;

                        if (pointerCount == 1) {
                            mMode = CurrentEvent.PAN;
                            // TODO: Translate the field
                            break;
                        }

                        mMode = CurrentEvent.ROTATE;

                        // TODO: Else Rotate the field

                        // view.invalidate();
                        break;
                    }
                }
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                mMode = CurrentEvent.NONE;

            }
            case MotionEvent.ACTION_POINTER_UP: {
                mMode = CurrentEvent.NONE;

            }
            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
        }
        return true;
    }

    public boolean onDrag(View view,DragEvent event) {
        // mRenderer.handleTouchDrag(normalizedX1, normalizedY1, normalizedX2, normalizedY2);
        return false;
    }

    // private final GestureDetector gestureDetector;
    // Scale event detection
    private class ScaleListener extends SimpleOnScaleGestureListener {

        float scaleFocusX = 0;
        float scaleFocusY = 0;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector arg0) {
            mMode = CurrentEvent.SCALE;
            // invalidate();
            scaleFocusX = arg0.getFocusX();
            scaleFocusY = arg0.getFocusY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(mMinScale, Math.min(mScaleFactor, mMaxScale));

            // requestRender();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector arg0) {
            mMode = CurrentEvent.NONE;
            scaleFocusX = 0;
            scaleFocusY = 0;
        }
    }
}
