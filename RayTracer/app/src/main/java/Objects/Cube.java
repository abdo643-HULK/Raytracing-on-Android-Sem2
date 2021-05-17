package Objects;

import Util.Geometry.Vector;

public class Cube {
    private Vector min;
    private Vector max;
    private Vector color;

    public Cube(Vector min, Vector max, Vector color) {
        this.min = min;
        this.max = max;
        this.color = color;
    }

    public Vector getMin() {
        return min;
    }

    public void setMin(Vector min) {
        this.min = min;
    }

    public Vector getMax() {
        return max;
    }

    public void setMax(Vector max) {
        this.max = max;
    }

    public Vector getColor() {
        return color;
    }

    public void setColor(Vector color) {
        this.color = color;
    }
}
