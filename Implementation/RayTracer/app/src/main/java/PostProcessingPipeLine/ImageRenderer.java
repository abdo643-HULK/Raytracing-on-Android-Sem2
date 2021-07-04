package PostProcessingPipeLine;

import android.util.Log;

import Data.FBO;
import Data.VAO;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDrawArrays;

public class ImageRenderer {

    private FBO fbo;

    protected ImageRenderer(int width, int height) {
        this.fbo = new FBO(width, height, FBO.NONE);
    }

    protected ImageRenderer() {}

    protected void renderQuad(VAO vao) {
        if (fbo != null) {
            fbo.bindFrameBuffer();
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        vao.drawVertices();
        if (fbo != null) {
            fbo.unbindFrameBuffer();
        }
    }
}