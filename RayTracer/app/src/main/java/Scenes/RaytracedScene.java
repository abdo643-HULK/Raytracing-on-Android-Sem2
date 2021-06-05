package Scenes;

import android.content.Context;
import android.util.Log;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.util.ArrayList;

import Objects.Camera;
import Objects.Cube;
import Objects.Sphere;
import PostProcessingPipeLine.Processing;
import Programs.ComputeShaderProgram;
import Util.Direction;
import Util.Geometry.Vector;
import Util.Geometry.Point;
import Util.Geometry.Rotation;
import Util.Geometry.Scale;

import Util.Timer;

import static Util.MatrixHelper.perspectiveM;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SCISSOR_TEST;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glScissor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;

/**
 * Created by Andreas on 11.05.2020.
 */

public class RaytracedScene implements Scene {

    private static final int FRAMEBUFFER_WIDTH = 1024;
    private static final int FRAMEBUFFER_HEIGHT = 1024;

    // Matrix to create 3D effect and fit to screen
    private final float[] projectionMatrix = new float[16];
    // Inverted view matrix = camera position
    private final float[] invertedViewMatrix = new float[16];
    // Matrices to hold multiplication results
    private final float[] viewProjectionMatrix = new float[16];
    // Matrix that is inverted and can therefor be used to map 2D touch points to a line in 3D
    private final float[] invertedViewProjectionMatrix = new float[16];

    // Camera
    private Camera camera;

    // FrameBuffer
    private int[] frameBuffer;

    // Cubes
    private Cube cube1;
    private Cube cube2;
    private ArrayList<Cube> cubeList;

    // Spheres
    private Sphere sphere1;
    private Sphere sphere2;
    private ArrayList<Sphere> sphereList;

    // Timer & Speed
    private Timer sphere2Timer;
    private Vector sphere2Speed;

    // Shader
    private ComputeShaderProgram computeProgram;

    @Override
    public void onSurfaceCreated(Context context) {
        // Camera
        camera = new Camera(new Point(3.0f, 2.0f, 7.0f), new Point(0.0f, 0.5f, 0.0f));

        // FrameBuffer (a texture that the scene will be written to)
        frameBuffer = new int[1];
        glGenTextures(1, frameBuffer, 0);

        // Cubes
        cube1 = new Cube(new Vector(-5.0f, -0.1f, -5.0f), new Vector(5.0f, 0.0f, 5.0f), new Vector(0.9f, 0.9f, 0.9f));
        cube2 = new Cube(new Vector(-0.5f, 0.0f, -0.5f), new Vector(0.5f, 1.0f, 0.5f), new Vector(0.88f, 0.45f, 0.55f));
        cubeList = new ArrayList<>();
        cubeList.add(cube1);
        cubeList.add(cube2);

        // Spheres
        sphere1 = new Sphere(new Vector(1.0f, 0.5f, 0.0f), 0.5f, new Vector(0.2f, 0.7f, 0.1f));
        sphere2 = new Sphere(new Vector(-1.0f, 0.5f, 0.0f), 0.5f, new Vector(0.4f, 0.0f, 0.4f));
        sphereList = new ArrayList<>();
        sphereList.add(sphere1);
        sphereList.add(sphere2);

        // Timer & Speed
        sphere2Timer = new Timer(1000);
        sphere2Speed = new Vector(0f, 0.03f, 0f);

        // Shader
        computeProgram = new ComputeShaderProgram(context);


        // PostProcessing
        // There is no need for an actual FBO, just the init() has to be called so that the postToScreen method can be used
        // The postToScreen method creates it's own FBO in the imageRenderer that is used for rendering the scene texture to the screen
        Processing.init();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        // Set the OpenGL viewport to fill the entire surface
        glViewport(0, 0, width, height);

        // Don't render anything outside the visible window
        glScissor(0, 0, width, height);
        glEnable(GL_SCISSOR_TEST);

        // Don't render color when using textures with transparency
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Set up a projection matrix
        perspectiveM(projectionMatrix, 60, (float) width / (float) height, 1f, 20f);
    }

    @Override
    public void onDrawFrame() {
        // Add all the important matrices together into viewProjectionMatrix
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, camera.getViewMatrix(), 0);

        // Create the inverted view projection matrix
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        // Create the inverted viewMatrix
        invertM(invertedViewMatrix, 0, camera.getViewMatrix(), 0);

        // Move sphere2
        sphere2.setCenter(Vector.add(sphere2.getCenter(), sphere2Speed));
        if (sphere2Timer.hasNeverBeenStarted() || sphere2Timer.hasFinished()) {
            if (!sphere2Timer.hasNeverBeenStarted()) {
                sphere2Speed = sphere2Speed.scale(-1f);
            }
            sphere2Timer.start();
        }

        computeProgram.useProgram();

        computeProgram.setUniforms(frameBuffer[0], FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, invertedViewProjectionMatrix, invertedViewMatrix, cubeList, sphereList); // width and height must be powers of two

        Processing.postToScreen(frameBuffer[0]);
    }

    @Override
    public void handleTouchPress(float normalizedX, float normalizedY) {

    }

    @Override
    public void handleScale(float scaleFactor) {
        camera.scale(scaleFactor);
    }

    @Override
    public void handleRotation(float angle, int x, int y) {
        camera.rotate(new Rotation(angle, x, y, 0));
    }

    @Override
    public void handleTouchDrag(Direction direction) {
        switch (direction) {
            case UP: {
                camera.translate(0, -0.08f);
                break;
            }
            case DOWN: {
                camera.translate(0, 0.08f);
                break;
            }
            case LEFT: {
                camera.translate(0.08f, 0f);
                break;
            }
            case RIGHT: {
                camera.translate(-0.08f, 0f);
                break;
            }
        }
    }

    @Override
    public void handleTouchRelease(float normalizedX, float normalizedY) {

    }

    @Override
    public void onReload() {

    }
}
