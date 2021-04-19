package Util;

import Util.Geometry.Point;
import Util.Geometry.Vector;
import Util.Geometry.Rotation;

import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by Andreas on 24.04.2020.
 */

public class MatrixHelper {

    // This function sets up a perspective projection matrix which is able to project a 3D scene onto a 2D screen
    public static void perspectiveM(float[] m, float yFovInDegrees, float aspect, float near, float far) {
        // Convert degrees to radians
        final float angleInRadians = (float) (yFovInDegrees * Math.PI / 180);
        // Calculate the focal length
        final float a = (float) (1.0 / Math.tan(angleInRadians / 2.0));

        // Creating the Matrix in a float array
        m[0] = a / aspect;
        m[1] = 0f;
        m[2] = 0f;
        m[3] = 0f;

        m[4] = 0f;
        m[5] = a;
        m[6] = 0f;
        m[7] = 0f;

        m[8] = 0f;
        m[9] = 0f;
        m[10] = -((far + near) / (far - near));
        m[11] = -1f;

        m[12] = 0f;
        m[13] = 0f;
        m[14] = -((2f * far * near) / (far - near));
        m[15] = 0f;
    }

    public static float[] createTransformationMatrix(Vector translation, Vector scale) {
        float[] matrix = new float[16];
        setIdentityM(matrix, 0);
        translateM(matrix, 0, translation.x, translation.y, translation.z);
        scaleM(matrix, 0, scale.x, scale.y, scale.z);
        return matrix;
    }

    public static float[] createTransformationMatrix(Point translation, float scale, Rotation rotation) {
        float[] matrix = new float[16];
        setIdentityM(matrix, 0);
        translateM(matrix, 0, translation.x, translation.y, translation.z);
        if(rotation.angle!=0) {
            rotateM(matrix, 0, rotation.angle, rotation.x, rotation.y, rotation.z);
        }
        scaleM(matrix, 0, scale, scale, scale);
        return matrix;
    }
}
