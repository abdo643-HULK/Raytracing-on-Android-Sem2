package Objects;

import Util.Geometry;
import Util.Geometry.Point;
import Util.Geometry.Vector;

import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

public class Camera {

    // Camera properties
    protected Point position;
    private Point viewCenterPosition;
    private final float[] viewMatrix;

    public Camera(Point position, Point viewCenterPosition) {
        viewMatrix =  new float[16];
        this.viewCenterPosition = viewCenterPosition;
        this.position = position;
        updateViewMatrix();
    }

    // Sets the view point position
    public void setPosition(Point position) {
        this.position = position;
        updateViewMatrix();
    }

    // Sets the focal point position
    public void setViewCenterPosition(Point viewCenterPosition) {
        this.viewCenterPosition = viewCenterPosition;
        updateViewMatrix();
    }

    public void translate(Vector translationVector) {
        this.viewCenterPosition = viewCenterPosition.translate(translationVector);
        this.position = position.translate(translationVector);
        updateViewMatrix();
    }

    public void translateX(float distance) {
        this.viewCenterPosition = viewCenterPosition.translateX(distance);
        this.position = position.translateX(distance);
        updateViewMatrix();
    }

    public void translateY(float distance) {
        this.viewCenterPosition = viewCenterPosition.translateY(distance);
        this.position = position.translateY(distance);
        updateViewMatrix();
    }

    public void translateZ(float distance) {
        this.viewCenterPosition = viewCenterPosition.translateZ(distance);
        this.position = position.translateZ(distance);
        updateViewMatrix();
    }

    public void rotate(Geometry.Rotation rotation) {
        rotateM(viewMatrix, 0, rotation.angle, rotation.x, rotation.y, rotation.z);
    }

    private void updateViewMatrix() {
        setLookAtM(viewMatrix, 0, position.x, position.y, position.z, viewCenterPosition.x, viewCenterPosition.y, viewCenterPosition.z, 0f, 1f, 0f);
    }

    public Point getViewCenterPosition() {
        return viewCenterPosition;
    }

    public Point getPosition() {
        return position;
    }

    public float[] getViewMatrix() {
        return viewMatrix;
    }
}
