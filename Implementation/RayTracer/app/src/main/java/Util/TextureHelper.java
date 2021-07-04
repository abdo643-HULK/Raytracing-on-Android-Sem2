package Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import static android.opengl.GLES11Ext.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static android.opengl.GLES11Ext.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_EXTENSIONS;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetFloatv;
import static android.opengl.GLES20.glGetString;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

/**
 * Created by Andreas on 24.04.2020.
 */

public class TextureHelper {

    private static final String TAG = "TextureHelper";

    public static int loadTexture(Context context, int resourceID) {

        final int[] textureObjectIDs = new int[1];
        glGenTextures(1, textureObjectIDs, 0);

        if (textureObjectIDs[0] == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not generate a new OpenGL texture object");
            }
            return 0;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceID, options);

        if (bitmap == null) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Resource ID " + resourceID + "could not be decoded");
            }

            glDeleteTextures(1, textureObjectIDs, 0);
            return 0;
        }

        // Binding the texture so it can be loaded
        glBindTexture(GL_TEXTURE_2D, textureObjectIDs[0]);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        if(glGetString(GL_EXTENSIONS).contains("GL_EXT_texture_filter_anisotropic")) {
            float[] buffer = new float[1];
            glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer, 0);
            float amount = Math.min(4f, buffer[0]);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
        }

        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

        glGenerateMipmap(GL_TEXTURE_2D);

        // Unbinding the texture after loading it
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureObjectIDs[0];
    }

    public static int loadCroppedTexture(Context context, int resourceID, int x, int y, int width, int height) {

        final int[] textureObjectIDs = new int[1];
        glGenTextures(1, textureObjectIDs, 0);

        if (textureObjectIDs[0] == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not generate a new OpenGL texture object");
            }
            return 0;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        final Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), resourceID, options);
        Bitmap bitmap = Bitmap.createBitmap(originalBitmap, x, y, width, height);

        if (bitmap == null) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Resource ID " + resourceID + "could not be decoded");
            }

            glDeleteTextures(1, textureObjectIDs, 0);
            return 0;
        }

        // Binding the texture so it can be loaded
        glBindTexture(GL_TEXTURE_2D, textureObjectIDs[0]);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        if(glGetString(GL_EXTENSIONS).contains("GL_EXT_texture_filter_anisotropic")) {
            float[] buffer = new float[1];
            glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer, 0);
            float amount = Math.min(4f, buffer[0]);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

        glGenerateMipmap(GL_TEXTURE_2D);

        // Unbinding the texture after loading it
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureObjectIDs[0];
    }
}
