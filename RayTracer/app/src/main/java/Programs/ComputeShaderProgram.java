package Programs;

import android.content.Context;

import com.example.raytracer.R;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
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

    // Uniform locations
    private final int uTextureUnitLocation;
    private final int uInvertedViewProjectionMatrixLocation;
    private final int uInvertedViewMatrix;

    public ComputeShaderProgram(Context context) {
        super(context, R.raw.compute_shader);

        // Retrieve uniform locations for the shader program
        uTextureUnitLocation = glGetUniformLocation(program, U_FRAME_BUFFER);
        uInvertedViewProjectionMatrixLocation = glGetUniformLocation(program, U_INVERTED_VIEW_PROJECTION_MATRIX);
        uInvertedViewMatrix = glGetUniformLocation(program, U_INVERTED_VIEW_MATRIX);
    }

    public void setUniforms(int textureID, int width, int height, float[] invertedViewProjectionMatrix, float[] invertedViewMatrix) {
        // Pass the camera position into the shader program
        glUniformMatrix4fv(uInvertedViewMatrix, 1, false, invertedViewMatrix, 0);

        // Pass the inverted view projection matrix into the shader program
        glUniformMatrix4fv(uInvertedViewProjectionMatrixLocation, 1, false, invertedViewProjectionMatrix, 0);

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
