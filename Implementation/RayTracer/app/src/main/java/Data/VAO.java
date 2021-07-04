package Data;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glGenVertexArrays;

public class VAO {

    private int[] bufferID;
    private VBOHandler vboHandler;

    public VAO(float[] vertexData) {
        // Create the VAO by asking for an ID and binding it
        bufferID = new int[1];
        glGenVertexArrays(1, bufferID, 0);
        glBindVertexArray(bufferID[0]);

        // Create VBOs and add them to the VAO
        vboHandler = new VBOHandler(vertexData);
    }

    public void drawVertices() {
        glBindVertexArray(bufferID[0]);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, vboHandler.getVerticesCount());
        glBindVertexArray(0);
    }
}
