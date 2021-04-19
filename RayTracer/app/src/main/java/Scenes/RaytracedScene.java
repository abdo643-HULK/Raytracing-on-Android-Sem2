package Scenes;

import android.content.Context;

import Objects.Camera;
import PostProcessingPipeLine.Processing;
import Programs.ComputeShaderProgram;
import Util.Geometry;

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

    // Shader
    private ComputeShaderProgram computeProgram;

    @Override
    public void onSurfaceCreated(Context context) {
        // Camera
        camera = new Camera(new Geometry.Point(3.0f, 2.0f, 7.0f), new Geometry.Point(0.0f, 0.5f, 0.0f));

        //FrameBuffer (a texture that the scene will be written to)
        frameBuffer = new int[1];
        glGenTextures(1, frameBuffer, 0);

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
        glScissor(0,0, width, height);
        glEnable(GL_SCISSOR_TEST);

        // Don't render color when using textures with transparency
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Set up a projection matrix
        perspectiveM(projectionMatrix, 60, (float)width / (float)height, 1f, 20f);
    }

    @Override
    public void onDrawFrame() {
        // Add all the important matrices together into viewProjectionMatrix
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, camera.getViewMatrix(), 0);

        // Create the inverted view projection matrix
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        // Create the inverted viewMatrix
        invertM(invertedViewMatrix, 0, camera.getViewMatrix(), 0);

        computeProgram.useProgram();

        computeProgram.setUniforms(frameBuffer[0], FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, invertedViewProjectionMatrix, invertedViewMatrix); // width and height must be powers of two

        Processing.postToScreen(frameBuffer[0]);
    }

    @Override
    public void handleTouchPress(float normalizedX, float normalizedY) {

    }

    @Override
    public void handleTouchDrag(float normalizedX, float normalizedY, float normalizedX1, float normalizedY1) {
        camera.rotate(new Geometry.Rotation(1f, 0, 1, 0));
    }

    @Override
    public void handleTouchRelease(float normalizedX, float normalizedY) {

    }

    @Override
    public void onReload() {

    }
}
