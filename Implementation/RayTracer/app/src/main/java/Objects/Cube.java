package Objects;

import Util.Geometry.Vector;

public class Cube {

    public enum Material {
        DIFFUSE,
        METAL,
        LIGHT
    }

    private Vector min;
    private Vector max;
    private Vector color;
    private Material material;
    private float parameter0;

    public Cube(Vector min, Vector max, Vector color, Material material, float parameter0) {
        this.min = min;
        this.max = max;
        this.color = color;
        this.material = material;
        this.parameter0 = parameter0; // Diffuse: Attenuation - Metal: Reflectivity
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

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public float getParameter0() {
        return parameter0;
    }

    public void setParameter0(float parameter0) {
        this.parameter0 = parameter0;
    }
}
