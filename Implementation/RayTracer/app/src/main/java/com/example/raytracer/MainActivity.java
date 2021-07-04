package com.example.raytracer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import android.app.Activity;
import android.app.ActivityManager;

// Zum Abfragen der GL Version hinzugefügt
import android.content.Context;
import android.content.pm.ConfigurationInfo;

// Für Logcat
import android.util.Log;
import android.util.TypedValue;
// Um die DPI des Displays zu bekommen
import android.util.DisplayMetrics;

import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
//import android.view.ViewGroup.LayoutParams;

import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams;

import Util.StateManager;

// Auf Top level import geändert
public class MainActivity extends Activity {

    private static final String TAG = "Raytracer";

    private final float mMinScale = 0.5f;
    private final float mMaxScale = 2.5f;

    private RaytracerSurfaceView glSurfaceView;
    private boolean rendererSet = false;
    private float mCurrentScale = 1f;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!detectOpenGLES31()) {
            Log.e(TAG, "OpenGL ES 3.1 not supported on device.  Exiting...");
            finish();
        }

        final Display display = getWindowManager().getDefaultDisplay();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        glSurfaceView = new RaytracerSurfaceView(this, displayMetrics);
        rendererSet = true;

        if (!StateManager.isLoaded) {
            StateManager.load(3, this); //TODO CHANGE BACK
        }

        final ConstraintSet set = new ConstraintSet();
        final ConstraintLayout cl = new ConstraintLayout(this);
        cl.setId(View.generateViewId());

        final LayoutParams clLP = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        cl.setLayoutParams(clLP);

        final LayoutParams zoomInLP = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final Button zoomIn = new Button(this);
        zoomIn.setText("+");
        zoomIn.setId(View.generateViewId());
        zoomIn.setLayoutParams(zoomInLP);
        zoomIn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50F);
        zoomIn.setPadding(5, 0, 0, 5);
        zoomIn.setOnClickListener(v -> {
            mCurrentScale += 0.5f;
            mCurrentScale = Math.max(mMinScale, Math.min(mCurrentScale, mMaxScale));
            glSurfaceView.scale(mCurrentScale);
        });

        final LayoutParams zoomOutLP = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final Button zoomOut = new Button(this);
        zoomOut.setText("-");
        zoomOut.setId(View.generateViewId());
        zoomOut.setLayoutParams(zoomOutLP);
        zoomOut.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80F);
        zoomOut.setPadding(0, -80, 0, -40);
        zoomOut.setOnClickListener(v -> {
            mCurrentScale -= 0.5f;
            mCurrentScale = Math.max(mMinScale, Math.min(mCurrentScale, mMaxScale));
            glSurfaceView.scale(mCurrentScale);
        });

        cl.addView(zoomIn);
        cl.addView(zoomOut);

        set.clone(cl);
        set.connect(zoomIn.getId(), ConstraintSet.END, zoomOut.getId(), ConstraintSet.END, 0);
        set.connect(zoomIn.getId(), ConstraintSet.BOTTOM, zoomOut.getId(), ConstraintSet.TOP, 0);

        set.connect(zoomOut.getId(), ConstraintSet.BOTTOM, cl.getId(), ConstraintSet.BOTTOM, 50);
        set.connect(zoomOut.getId(), ConstraintSet.END, cl.getId(), ConstraintSet.END, 40);

        set.applyTo(cl);
        setContentView(glSurfaceView);
        addContentView(cl, clLP);
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

    private boolean detectOpenGLES31() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= 0x00030001);
    }
}
