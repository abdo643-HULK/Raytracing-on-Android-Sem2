package Scenes;

import android.content.Context;

import Util.Direction;

/**
 * Created by Andreas on 11.05.2020.
 */

public interface Scene {
    void onSurfaceCreated(Context context);

    void onSurfaceChanged(int width, int height);

    void onDrawFrame();

    void handleTouchPress(float normalizedX, float normalizedY);

    //    void handleTouchDrag(float normalizedX, float normalizedY, float normalizedX1, float normalizedY1);
//    void handleTouchDrag(float normalizedX, int normalizedY, int normalizedX1);
    void handleTouchDrag(Direction direction);

    void handleTouchRelease(float normalizedX, float normalizedY);

    void handleScale(float scaleFactor);
    void handleRotation(float angle, int x, int y);

    void onReload();
}
