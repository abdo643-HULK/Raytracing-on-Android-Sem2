package Objects;

import android.util.Log;

import Util.Geometry;
import Util.Geometry.Point;
import Util.Geometry.Vector;

import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

public class Camera {

    private static final String TAG = "Raytracer";
    private final float[] mOriginalMatrix;
    private final float[] viewMatrix;
    // Camera properties
    protected Point position;
    private Point viewCenterPosition;

    public Camera(Point position, Point viewCenterPosition) {
        viewMatrix = new float[16];
        mOriginalMatrix = new float[16];
        this.viewCenterPosition = viewCenterPosition;
        this.position = position;
        updateViewMatrix();
    }

//    public void translate(Vector translationVector) {
//        this.viewCenterPosition = viewCenterPosition.translate(translationVector);
//        this.position = position.translate(translationVector);
//        updateViewMatrix();
//    }

    public void translateX(float distance) {
        this.viewCenterPosition = viewCenterPosition.translateX(distance);
        this.position = position.translateX(distance);
        //updateViewMatrix();
    }

    public void translateY(float distance) {
        this.viewCenterPosition = viewCenterPosition.translateY(distance);
        this.position = position.translateY(distance);
        //updateViewMatrix();
    }

    public void translateZ(float distance) {
        this.viewCenterPosition = viewCenterPosition.translateZ(distance);
        this.position = position.translateZ(distance);
        updateViewMatrix();
    }

    public void translate(float x,float y){
        translateM(viewMatrix,0,x,y,0);
        translateM(mOriginalMatrix,0,x,y,0);
//        this.viewCenterPosition = viewCenterPosition.translateX(x);
//        this.viewCenterPosition = viewCenterPosition.translateY(y);
//        this.position = position.translateX(x);
//        this.position = position.translateY(y);
//        updateViewMatrix();
    }

    public void rotate(Geometry.Rotation rotation) {
        rotateM(viewMatrix, 0, rotation.angle, rotation.x, rotation.y, rotation.z);
        rotateM(mOriginalMatrix, 0, rotation.angle, rotation.x, rotation.y, rotation.z);
//        updateViewMatrix();
    }

    private void updateViewMatrix() {
        setLookAtM(viewMatrix, 0, position.x, position.y, position.z, viewCenterPosition.x, viewCenterPosition.y, viewCenterPosition.z, 0f, 1f, 0f);
        setLookAtM(mOriginalMatrix, 0, position.x, position.y, position.z, viewCenterPosition.x, viewCenterPosition.y, viewCenterPosition.z, 0f, 1f, 0f);
    }

    public void scale(float scaleFactor) {
        scaleM(viewMatrix,
                0,
                mOriginalMatrix,
                0,
                scaleFactor,
                scaleFactor,
                scaleFactor);
    }

    public Point getViewCenterPosition() {
        return viewCenterPosition;
    }

    // Sets the focal point position
    public void setViewCenterPosition(Point viewCenterPosition) {
        this.viewCenterPosition = viewCenterPosition;
        updateViewMatrix();
    }

    public Point getPosition() {
        return position;
    }

    // Sets the view point position
    public void setPosition(Point position) {
        this.position = position;
        updateViewMatrix();
    }

    public float[] getViewMatrix() {
        return viewMatrix;
    }
}
