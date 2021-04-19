package Programs;

import android.content.Context;

import com.example.raytracer.R;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_SCISSOR_TEST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;

/**
 * Created by Andreas on 24.04.2020.
 */

public class ToScreenShaderProgram extends ShaderProgram {

    // Uniform Locations
    private final int uTextureUnitLocation;

    public ToScreenShaderProgram(Context context) {
        super(context, R.raw.to_screen_vertex_shader, R.raw.to_screen_fragment_shader);

        // Retrieve uniform locations for the shader program
        uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
    }

    public void setUniforms(int textureID) {
        // Set the active texture unit to texture unit 0
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0
        glUniform1i(uTextureUnitLocation, 0);
    }

    public void useProgram() {
        glEnable(GL_DEPTH_TEST); // CAREFUL: disable while rendering gui (so that two gui objects with transparency can be placed on top of each other)
        glEnable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND); // CAREFUL: disable when not rendering gui

        // Set the current OpenGL shader program to this program
        glUseProgram(program);
    }
}
