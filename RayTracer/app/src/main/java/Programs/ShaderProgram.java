package Programs;

import android.content.Context;

import Util.ShaderHelper;
import Util.TextResourceReader;

/**
 * Created by Andreas on 24.04.2020.
 */

public class ShaderProgram {
    // Uniform constants
    protected static final String U_VIEW_PROJECTION_MATRIX = "u_ViewProjectionMatrix";
    protected static final String U_INVERTED_VIEW_MATRIX = "u_InvertedViewMatrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    protected static final String U_COLOR = "u_Color";
    protected static final String U_TRANSFORMATION_MATRIX = "u_TransformationMatrix";
    protected static final String U_LIGHT_POSITION = "u_LightPosition";
    protected static final String U_LIGHT_COLOUR = "u_LightColour";
    protected static final String U_LIGHT_ATTENUATION = "u_LightAttenuation";
    protected static final String U_REFLECTIVITY = "u_Reflectivity";
    protected static final String U_SHINE_DAMPER = "u_ShineDamper";
    protected static final String U_LIGHT_EMITTING = "u_LightEmitting";

    // Uniform constants for ray tracing
    protected static final String U_FRAME_BUFFER = "u_FrameBuffer";
    protected static final String U_INVERTED_VIEW_PROJECTION_MATRIX = "u_InvertedViewProjectionMatrix";
    protected static final String U_CAMERA_POSITION = "u_CameraPosition";

    // Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    protected static final String A_NORMAL = "a_Normal";

    // Shader program
    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceID, int fragmentShaderResourceID) {
        // Compile the shaders and link the program
        program = ShaderHelper.buildProgram(TextResourceReader.readTextFileFromResource(context, vertexShaderResourceID), TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceID));
    }

    protected ShaderProgram(Context context, int computeShaderResourceID) {
        // Compile the shader and link the program
        program = ShaderHelper.buildProgram(TextResourceReader.readTextFileFromResource(context, computeShaderResourceID));
    }
}
