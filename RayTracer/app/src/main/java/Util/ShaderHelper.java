package Util;

import android.util.Log;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;
import static android.opengl.GLES31.GL_COMPUTE_SHADER;

/**
 * Created by Andreas on 23.04.2020.
 */

public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    public static int compileVertexShader(String shaderCode) {
        return compileShader(GL_VERTEX_SHADER, shaderCode);
    }

    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GL_FRAGMENT_SHADER, shaderCode);
    }

    public static int compileComputeShader(String shaderCode) {
        return compileShader(GL_COMPUTE_SHADER, shaderCode);
    }

    private static int compileShader(int type, String shaderCode) {
        final int shaderObjectID = glCreateShader(type);

        if (shaderObjectID == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not create new Shader.");
            }
            return 0;
        }

        glShaderSource(shaderObjectID, shaderCode);
        glCompileShader(shaderObjectID);

        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectID, GL_COMPILE_STATUS, compileStatus, 0);

        if (LoggerConfig.ON) {
            // Print the shader info log to the Android log output
            Log.v(TAG, "Results of compiling source:" + "\n" + shaderCode + "\n" + glGetShaderInfoLog(shaderObjectID));
        }

        if (compileStatus[0] == 0) {
            // If it failed, delete the shader object
            glDeleteShader(shaderObjectID);

            if (LoggerConfig.ON) {
                Log.w(TAG, "Compilation of shader failed");
            }
            return 0;
        }
        return shaderObjectID;
    }

    public static int linkProgram(int vertexShaderID, int fragmentShaderID) {
        final int programObjectID = glCreateProgram();

        if (programObjectID == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not create new program");
            }
            return 0;
        }

        glAttachShader(programObjectID, vertexShaderID);
        glAttachShader(programObjectID, fragmentShaderID);

        glLinkProgram(programObjectID);

        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectID, GL_LINK_STATUS, linkStatus, 0);

        if (LoggerConfig.ON) {
            // Print the program info log to the Android log output
            Log.v(TAG, "Results of linking program:" + "\n" + glGetProgramInfoLog(programObjectID));
        }

        if (linkStatus[0] == 0) {
            // If it failed, delete the program object
            glDeleteProgram(programObjectID);
            if (LoggerConfig.ON) {
                Log.w(TAG, "Linking of program failed");
            }
            return 0;
        }
        return programObjectID;
    }

    public static int linkProgram(int computeShaderID) {
        final int programObjectID = glCreateProgram();

        if (programObjectID == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not create new program");
            }
            return 0;
        }

        glAttachShader(programObjectID, computeShaderID);

        glLinkProgram(programObjectID);

        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectID, GL_LINK_STATUS, linkStatus, 0);

        if (LoggerConfig.ON) {
            // Print the program info log to the Android log output
            Log.v(TAG, "Results of linking program:" + "\n" + glGetProgramInfoLog(programObjectID));
        }

        if (linkStatus[0] == 0) {
            // If it failed, delete the program object
            glDeleteProgram(programObjectID);
            if (LoggerConfig.ON) {
                Log.w(TAG, "Linking of program failed");
            }
            return 0;
        }
        return programObjectID;
    }

    public static boolean validateProgram(int programObjectID) {
        glValidateProgram(programObjectID);

        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectID, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.v(TAG, "Results of validating program: " + validateStatus[0] + "\n" + "Log:" + glGetProgramInfoLog(programObjectID));

        return validateStatus[0] != 0;
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program;

        // Compile the shaders
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compileFragmentShader(fragmentShaderSource);

        // Link them into a shader program
        program = linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            validateProgram(program);
        }

        return program;
    }

    public static int buildProgram(String computeShaderSource) {
        int program;

        // Compile the shader
        int computeShader = compileComputeShader(computeShaderSource);

        // Link them into a shader program
        program = linkProgram(computeShader);

        if (LoggerConfig.ON) {
            validateProgram(program);
        }

        return program;
    }
}
