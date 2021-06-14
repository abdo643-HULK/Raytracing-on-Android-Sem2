package Programs;

import android.content.Context;
import android.opengl.Matrix;

import com.example.raytracer.R;

import java.util.ArrayList;

import Objects.Cube;
import Objects.Sphere;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES30.GL_RGBA32F;
import static android.opengl.GLES30.glTexStorage2D;
import static android.opengl.GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static android.opengl.GLES31.GL_WRITE_ONLY;
import static android.opengl.GLES31.glBindImageTexture;
import static android.opengl.GLES31.glDispatchCompute;
import static android.opengl.GLES31.glMemoryBarrier;

/**
 * Created by Andreas on 24.04.2020.
 */

public class ComputeShaderProgram extends ShaderProgram {

    // The following constants have to have the same value in the shader
    private static final int CUBE_COUNT = 3;
    private static final int SPHERE_COUNT = 3;

    // Uniform locations
    private final int uTextureUnitLocation;
    private final int uCameraPositionLocation;
    private final int uRay00Location;
    private final int uRay10Location;
    private final int uRay01Location;
    private final int uRay11Location;
    private final int[] uCubeMinArrayLocation;
    private final int[] uCubeMaxArrayLocation;
    private final int[] uCubeColorArrayLocation;
    private final int[] uCubeMaterialArrayLocation;
    private final int[] uCubeParameter0ArrayLocation;
    private final int[] uSphereCenterArrayLocation;
    private final int[] uSphereRadiusArrayLocation;
    private final int[] uSphereColorArrayLocation;
    private final int[] uSphereMaterialArrayLocation;
    private final int[] uSphereParameter0ArrayLocation;

    public ComputeShaderProgram(Context context) {
        super(context, R.raw.compute_shader);

        // Retrieve uniform locations for the shader program
        uTextureUnitLocation = glGetUniformLocation(program, U_FRAME_BUFFER);
        uCameraPositionLocation = glGetUniformLocation(program, "u_CameraPosition");
        uRay00Location = glGetUniformLocation(program, "u_Ray00");
        uRay10Location = glGetUniformLocation(program, "u_Ray10");
        uRay01Location = glGetUniformLocation(program, "u_Ray01");
        uRay11Location = glGetUniformLocation(program, "u_Ray11");

        uCubeMinArrayLocation = new int[CUBE_COUNT];
        uCubeMaxArrayLocation = new int[CUBE_COUNT];
        uCubeColorArrayLocation = new int[CUBE_COUNT];
        uCubeMaterialArrayLocation = new int[CUBE_COUNT];
        uCubeParameter0ArrayLocation = new int[CUBE_COUNT];
        uSphereCenterArrayLocation = new int[SPHERE_COUNT];
        uSphereRadiusArrayLocation = new int[SPHERE_COUNT];
        uSphereColorArrayLocation = new int[SPHERE_COUNT];
        uSphereMaterialArrayLocation = new int[SPHERE_COUNT];
        uSphereParameter0ArrayLocation = new int[SPHERE_COUNT];

        for(int i=0;i<CUBE_COUNT;i++) {
            uCubeMinArrayLocation[i] = glGetUniformLocation(program, "cubeMinArray"+"["+i+"]");
            uCubeMaxArrayLocation[i] = glGetUniformLocation(program, "cubeMaxArray"+"["+i+"]");
            uCubeColorArrayLocation[i] = glGetUniformLocation(program, "cubeColorArray"+"["+i+"]");
            uCubeMaterialArrayLocation[i] = glGetUniformLocation(program, "cubeMaterialArray"+"["+i+"]");
            uCubeParameter0ArrayLocation[i] = glGetUniformLocation(program, "cubeParameter0Array"+"["+i+"]");
        }

        for(int i=0;i<SPHERE_COUNT;i++) {
            uSphereCenterArrayLocation[i] = glGetUniformLocation(program, "sphereCenterArray"+"["+i+"]");
            uSphereRadiusArrayLocation[i] = glGetUniformLocation(program, "sphereRadiusArray"+"["+i+"]");
            uSphereColorArrayLocation[i] = glGetUniformLocation(program, "sphereColorArray"+"["+i+"]");
            uSphereMaterialArrayLocation[i] = glGetUniformLocation(program, "sphereMaterialArray"+"["+i+"]");
            uSphereParameter0ArrayLocation[i] = glGetUniformLocation(program, "sphereParameter0Array"+"["+i+"]");
        }
    }

    public void setUniforms(int textureID, int width, int height, float[] invertedViewProjectionMatrix, float[] invertedViewMatrix, ArrayList<Cube> cubeList, ArrayList<Sphere> sphereList) {
        // The following camera and ray calculations are done once per frame
        // If they were in the shader, the would be calculated once per ray (!)
        // Also having them in the java code means that the cpu can do the calculations
        // allowing the gpu to free up a bit of performance
        // (Ray tracing is way more gpu than cpu demanding so this will increase the performance)

        // Initializing the camera
        float[] cameraPosition = new float[4];
        Matrix.multiplyMV(cameraPosition, 0, invertedViewMatrix, 0, new float[]{0f, 0f, 0f, 1f}, 0);

        // The four corner rays are defined as vec4s so that mat4 multiplication and perspective divide (dividing by w component) is possible (Note: these are device coordinates (screen coordinates) with 1 as w)
        float[] ray00 = new float[]{-1, -1, 0, 1};// left, bottom
        float[] ray10 = new float[]{+1, -1, 0, 1};// right, bottom
        float[] ray01 = new float[]{-1, +1, 0, 1};// left, top
        float[] ray11 = new float[]{+1, +1, 0, 1};// right, top

        float[][] rayArray = new float[][]{ray00, ray10, ray01, ray11};

        // From clipping (device/screen) space to world space
        for(float[] ray : rayArray) {
            Matrix.multiplyMV(ray, 0, invertedViewProjectionMatrix, 0, ray, 0);
            ray[0] = (ray[0] / ray[3]) - cameraPosition[0];
            ray[1] = (ray[1] / ray[3]) - cameraPosition[1];
            ray[2] = (ray[2] / ray[3]) - cameraPosition[2];
            ray[3] = 1f;
        }

        // Pass the camera position into the shader program
        glUniform3f(uCameraPositionLocation, cameraPosition[0], cameraPosition[1], cameraPosition[2]);

        // Pass the four corner rays into the shader program
        glUniform3f(uRay00Location, ray00[0], ray00[1], ray00[2]);
        glUniform3f(uRay10Location, ray10[0], ray10[1], ray10[2]);
        glUniform3f(uRay01Location, ray01[0], ray01[1], ray01[2]);
        glUniform3f(uRay11Location, ray11[0], ray11[1], ray11[2]);

        // Pass the cubes into the shader program
        for(int i=0;i<CUBE_COUNT;i++) {
            if(i<cubeList.size()){
                glUniform3f(uCubeMinArrayLocation[i], cubeList.get(i).getMin().x, cubeList.get(i).getMin().y, cubeList.get(i).getMin().z);
                glUniform3f(uCubeMaxArrayLocation[i], cubeList.get(i).getMax().x, cubeList.get(i).getMax().y, cubeList.get(i).getMax().z);
                glUniform3f(uCubeColorArrayLocation[i], cubeList.get(i).getColor().x, cubeList.get(i).getColor().y, cubeList.get(i).getColor().z);
                if(cubeList.get(i).getMaterial() == Cube.Material.DIFFUSE) {
                    glUniform1i(uCubeMaterialArrayLocation[i], 0);
                } else if(cubeList.get(i).getMaterial() == Cube.Material.METAL) {
                    glUniform1i(uCubeMaterialArrayLocation[i], 1);
                }
                glUniform1f(uCubeParameter0ArrayLocation[i], cubeList.get(i).getParameter0());
            } else {
                glUniform3f(uCubeMinArrayLocation[i], 0, 0, 0);
                glUniform3f(uCubeMaxArrayLocation[i], 0, 0, 0);
                glUniform3f(uCubeColorArrayLocation[i], 0, 0, 0);
                glUniform1i(uCubeMaterialArrayLocation[i], 0);
                glUniform1f(uCubeParameter0ArrayLocation[i], 0f);
            }
        }

        // Pass the spheres into the shader program
        for(int i=0;i<SPHERE_COUNT;i++) {
            if(i<sphereList.size()){
                glUniform3f(uSphereCenterArrayLocation[i], sphereList.get(i).getCenter().x, sphereList.get(i).getCenter().y, sphereList.get(i).getCenter().z);
                glUniform1f(uSphereRadiusArrayLocation[i], sphereList.get(i).getRadius());
                glUniform3f(uSphereColorArrayLocation[i], sphereList.get(i).getColor().x, sphereList.get(i).getColor().y, sphereList.get(i).getColor().z);
                if(sphereList.get(i).getMaterial() == Sphere.Material.DIFFUSE) {
                    glUniform1i(uSphereMaterialArrayLocation[i], 0);
                } else if(sphereList.get(i).getMaterial() == Sphere.Material.METAL) {
                    glUniform1i(uSphereMaterialArrayLocation[i], 1);
                }
                glUniform1f(uSphereParameter0ArrayLocation[i], sphereList.get(i).getParameter0());
            } else {
                glUniform3f(uSphereCenterArrayLocation[i], 0, 0, 0);
                glUniform1f(uSphereRadiusArrayLocation[i], 0);
                glUniform3f(uSphereColorArrayLocation[i], 0, 0, 0);
                glUniform1i(uSphereMaterialArrayLocation[i], 0);
                glUniform1f(uSphereParameter0ArrayLocation[i], 0f);
            }
        }

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0
        glUniform1i(uTextureUnitLocation, 0);

        // Set the active texture unit to texture unit 0
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to the target GL_TEXTURE_2D
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Defining a texture images properties so that the shader can read it properly
        glTexStorage2D(GL_TEXTURE_2D,1, GL_RGBA32F, width, height);

        // Bind level 0 of the texture to image unit 0
        glBindImageTexture(0, textureID, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

        // Starts/Invokes the compute shader, the number of work groups HAS TO BE divided by the work group size specified in the shader layout declaration
        glDispatchCompute(width/8, height/8, 1);

        // Update textures that have been written on and will be read soon (the frameBuffer - textureID)
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }

    public void useProgram() {
        glDisable(GL_DEPTH_TEST); // CAREFUL: disable while rendering gui (so that two gui objects with transparency can be placed on top of each other)
        glDisable(GL_BLEND); // CAREFUL: disable when not rendering gui

        // Set the current OpenGL shader program to this program
        glUseProgram(program);
    }
}
