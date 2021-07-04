package Data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import Util.StateManager;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT16;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteRenderbuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferRenderbuffer;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenRenderbuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glRenderbufferStorage;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glViewport;

public class FBO {

    private static final String TAG = "FBO";

    public static final int NONE = 0;
    public static final int DEPTH_TEXTURE = 1;
    public static final int DEPTH_RENDER_BUFFER = 2;

    private final int width;
    private final int height;

    private int frameBuffer;

    private int colourTexture;
    private int depthTexture;

    private int depthBuffer;
    private int colourBuffer;

    public FBO(int width, int height, int depthBufferType) {
        this.width = width;
        this.height = height;
        initialiseFrameBuffer(depthBufferType);
    }

    public void cleanUp() {
        glDeleteFramebuffers(1, IntBuffer.allocate(1).put(frameBuffer));
        glDeleteTextures(1, IntBuffer.allocate(1).put(colourTexture));
        glDeleteTextures(1, IntBuffer.allocate(1).put(depthTexture));
        glDeleteRenderbuffers(1, IntBuffer.allocate(1).put(depthBuffer));
        glDeleteRenderbuffers(1, IntBuffer.allocate(1).put(colourBuffer));
    }

    public void bindFrameBuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glViewport(0, 0, width, height);
    }

    public void unbindFrameBuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, StateManager.getWidth(), StateManager.getHeight());
    }

    public void bindToRead() {
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        //glReadBuffer(GL_COLOR_ATTACHMENT0);
    }

    public int getColourTexture() {
        return colourTexture;
    }

    public int getDepthTexture() {
        return depthTexture;
    }

    private void initialiseFrameBuffer(int type) {
        createFrameBuffer();
        createTextureAttachment();
        if (type == DEPTH_RENDER_BUFFER) {
            createDepthBufferAttachment();
        } else if (type == DEPTH_TEXTURE) {
            createDepthTextureAttachment();
        }
        unbindFrameBuffer();
    }

    private void createFrameBuffer() {
        IntBuffer frameIntBuffer = IntBuffer.allocate(1);
        glGenFramebuffers(1, frameIntBuffer);
        frameBuffer = frameIntBuffer.get(0);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        //glDrawBuffers(1, IntBuffer.allocate(1).put(GL_COLOR_ATTACHMENT0));
    }

    private void createTextureAttachment() {
        IntBuffer colourTextureBuffer = IntBuffer.allocate(1);
        glGenTextures(1, colourTextureBuffer);
        colourTexture = colourTextureBuffer.get(0);

        glBindTexture(GL_TEXTURE_2D, colourTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colourTexture,
                0);
    }

    private void createDepthTextureAttachment() {
        IntBuffer depthTextureBuffer = IntBuffer.allocate(1);
        glGenTextures(1, depthTextureBuffer);
        depthTexture = depthTextureBuffer.get(0);

        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT,
                GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
    }

    private void createDepthBufferAttachment() {
        IntBuffer depthBufferBuffer = IntBuffer.allocate(1);
        glGenRenderbuffers(1, depthBufferBuffer);
        depthBuffer = depthBufferBuffer.get(0);

        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER,
                depthBuffer);
    }
}