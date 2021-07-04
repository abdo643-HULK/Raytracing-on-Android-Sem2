package Objects;

import Programs.ShaderProgram;
import Util.Geometry.Vector;

public class Sphere {

    public enum Material {
        DIFFUSE,
        METAL,
        LIGHT
    }

    private Vector center;
    private float radius;
    private Vector color;
    private Material material;
    private float parameter0;

    public Sphere(Vector center, float radius, Vector color, Material material, float parameter0) {
        this.center = center;
        this.radius = radius;
        this.color = color;
        this.material = material;
        this.parameter0 = parameter0; // Diffuse: Attenuation - Metal: Reflectivity
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
