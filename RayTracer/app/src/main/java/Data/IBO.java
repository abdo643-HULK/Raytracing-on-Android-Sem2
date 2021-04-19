package Data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static Util.Constants.BYTES_PER_SHORT;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glGenBuffers;

/**
 * Created by Andreas on 13.05.2020.
 */

public class IBO {

    private final int bufferID;

    public IBO(short[] indexData) {
        // Allocate a buffer
        final int buffers[] = new int[1];
        glGenBuffers(buffers.length, buffers, 0);

        if(buffers[0] == 0) {
            throw new RuntimeException("Could not create a new index buffer object.");
        }
        bufferID = buffers[0];

        // Bind to the buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0]);

        // Transfer data to native memory
        ShortBuffer indexArray = ByteBuffer.allocateDirect(indexData.length * BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer().put(indexData);
        indexArray.position(0);

        // Transfer data from native memory to the GPU buffer
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArray.capacity()*BYTES_PER_SHORT, indexArray, GL_STATIC_DRAW);

        // IMPORTANT: Unbind from the buffer when done with it
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        // We let the native buffer go out of scope, but it won't be released
        // until the next time the garbage collector is run.
    }

    public int getBufferID() {
        return bufferID;
    }

}
