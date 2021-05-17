package Objects;

import Programs.ShaderProgram;
import Util.Geometry.Vector;

public class Sphere {

    private Vector center;
    private float radius;
    private Vector color;

    public Sphere(Vector center, float radius, Vector color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    public Vector getCenter() {
        return center;
    }

    public void setCenter(Vector center) {
        this.center = center;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Vector getColor() {
        return color;
    }

    public void setColor(Vector color) {
        this.color = color;
    }
}
