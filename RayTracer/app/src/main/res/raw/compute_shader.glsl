#version 310 es

//DEBUG_NAME: compute_shader.glsl

// Defining the number of boxes in the scene //if more than two boxes - edit in trace() needed
#define BOX_COUNT 2

// Defining the maximum bounds of the scene
#define MAX_SCENE_BOUNDS 1000.0

// Defining the local work group size of the compute shader (must be a power of two)
layout (local_size_x = 8, local_size_y = 8) in;

// Getting the uniform location of the framebuffer and setting its uniform value to 0
// rgba32f sets the image format qualifier for the image2D to rgba 32bit floating point
// This image2D represents the framebuffer that this shader will right to
layout(rgba32f, binding = 0) uniform highp writeonly image2D u_FrameBuffer;

// With raytracing there is no need to apply projection or view transformation matrices on every object
// Instead, the four corner rays of the camera's viewing frustum get defined
// Those can be tested for intersection against scene geometry
// They however have to use the inverted view projection matrix
uniform mat4 u_InvertedViewProjectionMatrix;

// The inverted view matrix can be used to calculate the proper camera position
uniform mat4 u_InvertedViewMatrix;

// Axis aligned 3D box defined by it's min corner and max corner coordinates
struct box {
    vec3 min;
    vec3 max;
    vec4 color;
};

// Ray defined by it's origin and it's direction
struct ray {
    vec3 origin;
    vec3 direction;
};

// Intersection hit information defined by hitPosition (the vec2 returned by intersectBox) and boxIndex (the index of the box that was hit)
struct hitInfo {
    vec2 hitPosition;
    int boxIndex;
};

// Creating an array of boxes
// Const = can't be changer after being initialized (final in java)
const box sceneBoxes[BOX_COUNT] = box[BOX_COUNT](

// This will be a ground plate
box(vec3(-5.0, -0.1, -5.0), vec3(5.0, 0.0, 5.0), vec4( 1, 1, 1,1)),

// This will be a box standing at the center of the ground plate
box(vec3(-0.5, 0.0, -0.5), vec3(0.5, 1.0, 0.5), vec4(0.88, 0.45, 0.55, 1))
);

// Apperantly glsl works like c, so one has to declare functions like so or put them above the main method
vec4 trace(ray cameraRay);
bool intersectSceneBoxes(ray cameraRay, out hitInfo info);
vec2 intersectBox(ray cameraRay, const box b);

// The main function (shader program entry point)
void main(void) {
    vec3 cameraPosition = (u_InvertedViewMatrix * vec4(0.0, 0.0, 0.0, 1.0)).xyz; // once again assuming w is 1

    // The rays are defined as vec4s so that mat4 multiplication and perspective divide (dividing by w component) is possible (Note: these are device coordinates (screen coordinates) with 1 as w)
    vec4 u_Ray00 = vec4(-1, -1, 0, 1); // left, bottom
    vec4 u_Ray10 = vec4(+1, -1, 0, 1); // right, bottom
    vec4 u_Ray01 = vec4(-1, +1, 0, 1); // left, top
    vec4 u_Ray11 = vec4(+1, +1, 0, 1); // right, top

    // From clipping (device/screen) space to world space
    u_Ray00 = u_InvertedViewProjectionMatrix * u_Ray00;
    u_Ray00 = vec4(((u_Ray00.xyz / u_Ray00.w) - cameraPosition), 1);
    u_Ray10 = u_InvertedViewProjectionMatrix * u_Ray10;
    u_Ray10 = vec4(((u_Ray10.xyz / u_Ray10.w) - cameraPosition), 1);
    u_Ray01 = u_InvertedViewProjectionMatrix * u_Ray01;
    u_Ray01 = vec4(((u_Ray01.xyz / u_Ray01.w) - cameraPosition), 1);
    u_Ray11 = u_InvertedViewProjectionMatrix * u_Ray11;
    u_Ray11 = vec4(((u_Ray11.xyz / u_Ray11.w) - cameraPosition), 1);

    // in the main: everything up to here is not efficient - it is calculated for every texel instead of once on the cpu and then parsed everytime

    ivec2 shaderDomainPosition = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(u_FrameBuffer);

    if(shaderDomainPosition.x >= size.x || shaderDomainPosition.y >= size.y) {
        return; // exit if the current shader invocation is out of bounds of the framebuffer
    }

    // mix performs a linear interpolation between param 1 and 2 using param 3 as weight
    // therefore we "move" from left to right, and top to bottom through all of the available texels
    // and calculate a direction ray for each of them -> the current shader invocation knows where to shoot the current ray
    vec2 position = vec2(shaderDomainPosition) / vec2(size.x, size.y);
    vec3 direction = mix(mix(u_Ray00.xyz, u_Ray01.xyz, position.y), mix(u_Ray10.xyz, u_Ray11.xyz, position.y), position.x);

    ray cameraRay;
    cameraRay.origin = cameraPosition;
    cameraRay.direction = direction;
    vec4 color = trace(cameraRay);

    imageStore(u_FrameBuffer, shaderDomainPosition, color);
}

// The function computes the amount of light that a given ray contributes when perceived by the eye
// So, any ray that will be used as input originates in the eye and goes through the framebuffer texel
// that is being computed in the current shader invocation
// If the Box was hit it returns its color
// If nothing was hit it returns an artificial sky color, depending on the rays directions height
vec4 trace(ray cameraRay) {
    hitInfo info;

    //calculates boxes
    if(intersectSceneBoxes(cameraRay, info)) {
        if(info.boxIndex == 0) return sceneBoxes[0].color;
        if(info.boxIndex == 1) return sceneBoxes[1].color;
        //if(info.boxIndex == x) return sceneBoxes[x].color;
        return vec4(0,0,0,1); //returns black if not enough returns are specified above
    }

    //calculates sky
    vec3 unit_direction = normalize(cameraRay.direction);
    float t = (unit_direction.y + 1.0);
    return vec4((1.0 - t) * vec3(1,1,1) + t * vec3(0,0,0.6), 1.0); //first variable is for bottom, second for top
}


// The function computes the nearest intersection with a box
// considering all boxes in the scene
// The hitInfo struct is used to return the information as out parameter (function out, not shader out)
bool intersectSceneBoxes(ray cameraRay, out hitInfo info) {
    float closestHitPosition = MAX_SCENE_BOUNDS; // the value of closestHitPosition will be changed when a more close box is hit (determines the closest box -> every single ray has to use it's closest visible box for it's pixel)
    bool intersectionFound = false;

    for(int i = 0; i < BOX_COUNT; i++) {
        vec2 hitPosition = intersectBox(cameraRay, sceneBoxes[i]);
        if(hitPosition.x > 0.0 && hitPosition.x < hitPosition.y && hitPosition.x < closestHitPosition) { // see the last two lines of explanation comment of intersectBox
            info.hitPosition = hitPosition;
            info.boxIndex = i;
            closestHitPosition = hitPosition.x;
            intersectionFound = true;
        }
    }
    return intersectionFound;
}

// Algorythm to test intersection with axis aligned 3D boxes
// The parametric form of a ray is used (origin + t * dir)
// The function returns tNear (x) at which the ray enters the box
// and the tFar (y) at which the ray leaves the box
// with t being the distance
// If the ray does not hit the box: tFar will be less than tNear
// If the box lies behind the ray: tNear will be negative
vec2 intersectBox(ray cameraRay, const box b) {
    vec3 tMin = (b.min - cameraRay.origin) / cameraRay.direction;
    vec3 tMax = (b.max - cameraRay.origin) / cameraRay.direction;
    vec3 t1 = min(tMin, tMax);
    vec3 t2 = max(tMin, tMax);
    float tNear = max(max(t1.x, t1.y), t1.z);
    float tFar = min(min(t2.x, t2.y), t2.z);
    return vec2(tNear, tFar);
}