package Util;

import android.graphics.Rect;
import android.opengl.Matrix;
import android.util.Log;

import static android.opengl.Matrix.multiplyMV;

/**
 * Created by Andreas on 25.04.2020.
 */

public class Geometry {

    public static class Point {
        public final float x, y, z;

        public Point(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Point translateY(float distance) {
            return new Point(x, y + distance, z);
        }

        public Point translateX(float distance) {
            return new Point(x + distance, y, z);
        }

        public Point translateZ(float distance) {
            return new Point(x, y, z + distance);
        }

        public Point translate(Vector vector) {
            return new Point(x + vector.x, y + vector.y, z + vector.z);
        }

        public boolean equals(Point point) {
            if(point.x == x && point.y == y && point.z == z){
                return true;
            } else {
                return false;
            }
        }
    }

    public static class Rectangle {
        public final Point center;
        public final float width;
        public final float height;

        public Rectangle(Point center, float width, float height) {
            this.center = center;
            this.width = width;
            this.height = height;
        }

        public float getRightSide() {
            return this.center.x + (this.width / 2);
        }

        public float getLeftSide() {
            return this.center.x - (this.width / 2);
        }

        public float getBottomSide() {
            return this.center.z + (this.height / 2);
        }

        public float getTopSide() {
            return this.center.z - (this.height / 2);
        }

        // Function determines whether or not this (a rectangle) intersects another rectangle
        public boolean intersects(Rectangle rectangle) {
            if(((this.center.x - (this.width/2)) < (rectangle.center.x + (rectangle.width/2)))
                    && ((rectangle.center.x - (rectangle.width/2)) < (this.center.x + (this.width/2)))
                    && ((this.center.z - (this.height/2)) < (rectangle.center.z + (rectangle.height/2)))
                    && ((rectangle.center.z - (rectangle.height/2)) < (this.center.z + (this.height/2)))) {
                return true;
            } else {
                return false;
            }
        }

        // Function determines whether or not this (a horizontal rectangle) is inside of another horizontal rectangle
        public boolean isInsideOf(Rectangle rectangle) {
            if (((this.center.x - (this.width/2)) > (rectangle.center.x - (rectangle.width/2)))
                    && (this.center.x + (this.width/2)) < ((rectangle.center.x + (rectangle.width/2)))
                    && (this.center.z - (this.height/2)) > (rectangle.center.z - (rectangle.height/2))
                    && (this.center.z + (this.height/2)) < ((rectangle.center.z + (rectangle.height/2)))) {
                return true;
            } else {
                return false;
            }
        }

        // Function determines whether or not this (a horizontal rectangle) is inside of another horizontal rectangle (only checks x axis)
        public boolean isInsideOfX(Rectangle rectangle) {
            if (((this.center.x - (this.width/2)) > (rectangle.center.x - (rectangle.width/2)))
                    && (this.center.x + (this.width/2)) < ((rectangle.center.x + (rectangle.width/2)))) {
                return true;
            } else {
                return false;
            }
        }

        // Function determines whether or not this (a horizontal rectangle) is inside of another horizontal rectangle (only checks z axis)
        public boolean isInsideOfZ(Rectangle rectangle) {
            if (((this.center.z - (this.height/2)) > (rectangle.center.z - (rectangle.height/2)))
                    && (this.center.z + (this.height/2)) < ((rectangle.center.z + (rectangle.height/2)))) {
                return true;
            } else {
                return false;
            }
        }

        // Function determines whether or not this (a rectangle) is intersected by a point (checks x and z axis)
        public boolean intersectedBy(Point point) {
            if ((point.x >= (center.x - (width / 2))) && (point.x <= (center.x + (width / 2))) && (point.z >= (center.z - (height / 2))) && (point.z <= (center.z + (height / 2)))) {
                return true;
            } else {
                return false;
            }
        }

        // Function determines whether or not this (a rectangle) is intersected by a point (checks x and y axis) (useful for horizontal objects like gui)
        public boolean intersectedByXY(Point point) {
            if ((point.x >= (center.x - (width / 2))) && (point.x <= (center.x + (width / 2))) && (point.y >= (center.y - (height / 2))) && (point.y <= (center.y + (height / 2)))) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static class Circle {
        public final Point center;
        public final float radius;

        public Circle(Point center, float radius) {
            this.center = center;
            this.radius = radius;
        }

        public Circle scale(float scale) {
            return new Circle(center, radius * scale);
        }
    }

    public static class Cylinder {
        public final Point center;
        public final float radius;
        public final float height;

        public Cylinder(Point center, float radius, float height) {
            this.center = center;
            this.radius = radius;
            this.height = height;
        }
    }

    public static class Vector {
        public final float x, y, z;

        public Vector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector scale(float f) {
            return new Vector(x * f, y * f, z * f);
        }

        public float absoluteValue() {
            return (float) Math.sqrt(x * x + y * y + z * z);
        }

        public Vector normalize() {
            return this.scale(1/this.absoluteValue());
        }

        public Vector crossProduct(Vector other) {
            return new Vector(
                    (y * other.z) - (z * other.y),
                    (z * other.x) - (x * other.z),
                    (x * other.y) - (y * other.x));
        }

        public float dotProduct(Vector other) {
            return x * other.x + y * other.y + z * other.z;
        }

        public static Vector add(Vector vector1, Vector vector2) {
            return new Vector(vector1.x + vector2.x, vector1.y + vector2.y, vector1.z + vector2.z);
        }

        public Vector setX(float x) {
            return new Vector(x, this.y, this.z);
        }

        public Vector setY(float y) {
            return new Vector(this.x, y, this.z);
        }

        public Vector setZ(float z) {
            return new Vector(this.x, this.y, z);
        }

        public Vector rotate(float angle, float x, float y, float z) {
            float[] resultVector = new float[4];
            float[] rotationMatrix = new float[16];

            Matrix.setRotateM(rotationMatrix, 0, angle, x, y, z);
            Matrix.multiplyMV(resultVector, 0, rotationMatrix, 0, new float[]{this.x, this.y, this.z, 1}, 0);

            return new Vector(resultVector[0], resultVector[1], resultVector[2]);
        }
    }

    public static class Ray {
        public final Point point;
        public final Vector vector;

        public Ray(Point startingPoint, Vector direction) {
            this.point = startingPoint;
            this.vector = direction;
        }
    }

    public static Vector vectorBetween(Point from, Point to) {
        return new Vector(to.x - from.x, to.y - from.y, to.z - from.z);
    }

    public static float angleBetween(Vector vec1, Vector vec2) {
        float numerator = vec1.dotProduct(vec2);
        float denominator = vec1.absoluteValue() * vec2.absoluteValue();

        return (float) Math.acos((numerator / denominator));
    }

    public static class Sphere {
        public final Point center;
        public final float radius;

        public Sphere(Point center, float radius) {
            this.center = center;
            this.radius = radius;
        }
    }

    public static boolean intersects(Sphere sphere, Ray ray) {
        return distanceBetween(sphere.center, ray) < sphere.radius;
    }

    public static float distanceBetween(Point point, Ray ray) {
        // Start of the ray to the point
        Vector p1ToPoint = vectorBetween(ray.point, point);
        // End of the ray to the point
        Vector p2ToPoint = vectorBetween(ray.point.translate(ray.vector), point);

        // The length of the cross product gives the area of an imaginary
        // parallelogram having the two vectors as sides. A parallelogram can be
        // thought of as consisting of two triangles, so this is the same as
        // twice the are of the triangle defined by the two vectors.
        float areaOfTriangleTimesTwo = p1ToPoint.crossProduct(p2ToPoint).absoluteValue();
        float lengthOfBase = ray.vector.absoluteValue();

        // The area of a triangle is also equal to (base * height) / 2.
        // In other words, the height is equal to (area * 2) / base.
        // The height of this triangle is the distance from the point to the ray.
        float distanceFromPointToRay = areaOfTriangleTimesTwo / lengthOfBase;
        return distanceFromPointToRay;
    }

    public static class Plane {
        public final Point point;
        public final Vector normal;

        public Plane(Point point, Vector normal) {
            this.point = point;
            this.normal = normal;
        }
    }

    public static Point intersectionPoint(Ray ray, Plane plane) {
        Vector rayToPlaneVector = vectorBetween(ray.point, plane.point);

        float scaleFactor = rayToPlaneVector.dotProduct(plane.normal) / ray.vector.dotProduct(plane.normal);

        Point intersectionPoint = ray.point.translate(ray.vector.scale(scaleFactor));
        return intersectionPoint;
    }

    public static Ray convertNormalized2DPointToRay(float[] invertedViewProjectionMatrix, float normalizedX, float normalizedY) {
        // We'll convert the normalized device coordinates into world-space
        // coordinates. We'll pick a point on the near and far planes, and draw a
        // line between them. To do this transform, we need to first multiply by
        // the inverse matrix, and then we need to undo the perspective divide.
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};

        final float[] nearWorldPoint = new float[4];
        final float[] farWorldPoint = new float[4];

        multiplyMV(nearWorldPoint, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(farWorldPoint, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

        // Undo the perspective divide
        divideByW(nearWorldPoint);
        divideByW(farWorldPoint);

        Point nearPointRay = new Point(nearWorldPoint[0], nearWorldPoint[1], nearWorldPoint[2]);
        Point farPointRay = new Point(farWorldPoint[0], farWorldPoint[1], farWorldPoint[2]);

        return new Ray(nearPointRay, vectorBetween(nearPointRay, farPointRay));
    }

    public static void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    public static class Rotation {
        public final float angle;
        public final int x;
        public final int y;
        public final int z;

        public Rotation(float angle, int x, int y, int z) {
            this.angle = angle;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
