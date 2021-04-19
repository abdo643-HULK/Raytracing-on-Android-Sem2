package Data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import Util.Constants;

import static Util.Constants.BYTES_PER_FLOAT;
import static Util.Constants.BYTES_PER_SHORT;
import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glVertexAttribPointer;

public class VBOHandler {

    private final int[] VBO;
    private int verticesCount;
    private int indicesCount;

    public VBOHandler(float[] vertexData) {
        // Create as many VBOs as needed
        VBO = new int[1];
        glGenBuffers(VBO.length, VBO, 0);

        // Fills all VBOs and add them to the currently bound VAO
        fillVBO(Constants.POSITION_ATTRIBUTE_LOCATION, vertexData, 2);

        // Save the number of vertices so that the VAO can use it in its draw method (data divided by component count)
        verticesCount = vertexData.length / 2;
    }

    // For vertices, textures, normals, etc.:
    public void fillVBO(int index, float[] data, int componentCount) {
        // Bind the buffer
        glBindBuffer(GL_ARRAY_BUFFER, VBO[index]);

        // Transfer all the data to native memory (android native heap)
        FloatBuffer dataBuffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer().put(data);
        dataBuffer.position(0);

        // Transfer data from native memory to the GPU buffer (bind the buffer and load it)
        int numberItems = data.length / componentCount;
        int stride = componentCount * BYTES_PER_FLOAT;

        glBufferData(GL_ARRAY_BUFFER, stride * numberItems, dataBuffer, GL_STATIC_DRAW);

        // Add VBO to VAO
        glVertexAttribPointer(index, componentCount, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(index);
    }

    // For indices:
    public void fillVBO(int index, short[] data, int componentCount) {
        // Bind the buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[index]);

        // Transfer all the data to native memory (android native heap)
        ShortBuffer dataBuffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer().put(data);
        dataBuffer.position(0);

        // Transfer data from native memory to the GPU buffer (bind the buffer and load it)
        int numberItems = data.length / componentCount;
        int stride = componentCount * BYTES_PER_SHORT;
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, stride * numberItems, dataBuffer, GL_STATIC_DRAW);
    }

    public int getVerticesCount() {
        return verticesCount;
    }

    public int getIndicesCount() {
        return indicesCount;
    }
}
