package Util;

/**
 * Created by Andreas on 24.04.2020.
 */

public class Constants {
    public static final int BYTES_PER_FLOAT = 4;
    public static final int BYTES_PER_INT = 4;
    public static final int BYTES_PER_SHORT = 2;
    public static final double NANOS_PER_SEC = 1000000000.0;
    public static final int TARGET_FPS = 60;

    // Shader Attribute Locations (have to have a globally used order for VAOs to work)
    public static final int POSITION_ATTRIBUTE_LOCATION = 0;
    public static final int TEXTURE_ATTRIBUTE_LOCATION = 1;
    public static final int NORMAL_ATTRIBUTE_LOCATION = 2;
}
