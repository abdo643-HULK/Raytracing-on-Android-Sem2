#version 310 es

//DEBUG_NAME: compute_shader.glsl

// Defining the number of boxes and spheres in the scene
//for more edit in trace() is needed
#define BOX_COUNT 2
#define SPHERE_COUNT 2

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

// ----- STRUCTS -----
// Axis aligned 3D box defined by it's min corner and max corner coordinates
struct box {
    vec3 min;
    vec3 max;
    vec4 color;
};

// Sphere defined by it's center coordinates and radius
struct sphere {
    vec3 center;
    float radius;
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
    int arrayIndex;
};

// ----- SCENE OBJECTS -----
// Creating an array of boxes
// Const = can't be changer after being initialized (final in java)
const box sceneBoxes[BOX_COUNT] = box[BOX_COUNT](

// This is a ground plate
box(vec3(-5.0, -0.1, -5.0), vec3(5.0, 0.0, 5.0), vec4(0.9, 0.9, 0.9, 1)),

// This is a box standing at the center of the ground plate
box(vec3(-0.5, 0.0, -0.5), vec3(0.5, 1.0, 0.5), vec4(0.88, 0.45, 0.55, 1))
);

// Creating an array of spheres
// Const = can't be changer after being initialized (final in java)
const sphere sceneSpheres[SPHERE_COUNT] = sphere[SPHERE_COUNT](

// This is a sphere on the left side of the box
sphere(vec3(1.0, 0.5, 0.0), 0.5, vec4(0.2, 0.7, 0.1, 1)),

// This is a sphere on the right side of the box
sphere(vec3(-1.0, 0.5, 0.0), 0.5, vec4(0.4, 0.0, 0.4, 1))
);

// ----- FUNCTION DECLERATIONS -----
// Apperantly glsl works like c, so one has to declare functions like so or put them above the main method
vec4 trace(ray cameraRay);
bool intersectSceneBoxes(ray cameraRay, out hitInfo info);
vec2 intersectBox(ray cameraRay, const box b);
bool intersectSceneSpheres(ray cameraRay, out hitInfo info);
float intersectSphere(ray cameraRay, const sphere s);

// ----- MAIN -----
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

    // TODO: everything up to here is not efficient - it is calculated for every texel instead of once on the cpu (java) and then parsed everytime

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
// If an object was hit it returns its color
// If nothing was hit it returns an artificial sky color, depending on the rays direction height
vec4 trace(ray cameraRay) {
    hitInfo boxHitInfo;
    hitInfo sphereHitInfo;

    // TODO: this is horrible, make it better
    // it's only necessary because with the previous system
    // boxes would always be infront of spheres as intersectSceneBoxes()
    // would return when a box was hit, giving the spheres no chance
    // to even calculate whether they were hit or not
    if(intersectSceneBoxes(cameraRay, boxHitInfo)) {
        if(intersectSceneSpheres(cameraRay, sphereHitInfo)) {
            if(boxHitInfo.hitPosition.x < sphereHitInfo.hitPosition.x) {
                if (boxHitInfo.arrayIndex == 0) return sceneBoxes[0].color;
                if (boxHitInfo.arrayIndex == 1) return sceneBoxes[1].color;
                //if(info.boxIndex == x) return sceneBoxes[x].color;
            } else {
                if (sphereHitInfo.arrayIndex == 0) return sceneSpheres[0].color;
                if (sphereHitInfo.arrayIndex == 1) return sceneSpheres[1].color;
                //if(info.arrayIndex == x) return sceneSpheres[x].color;
            }
        } else {
            if (boxHitInfo.arrayIndex == 0) return sceneBoxes[0].color;
            if (boxHitInfo.arrayIndex == 1) return sceneBoxes[1].color;
            //if(info.boxIndex == x) return sceneBoxes[x].color;
        }
    } else {
        if(intersectSceneSpheres(cameraRay, sphereHitInfo)) {
            if (sphereHitInfo.arrayIndex == 0) return sceneSpheres[0].color;
            if (sphereHitInfo.arrayIndex == 1) return sceneSpheres[1].color;
            //if(info.arrayIndex == x) return sceneSpheres[x].color;
        }
    }

    // calculates a sky colour if no object intersections were found
    vec3 unit_direction = normalize(cameraRay.direction);
    return vec4((1.0 - unit_direction.y) * vec3(1,1,1) + unit_direction.y * vec3(0,0,0.6), 1.0); //first vec3 is for bottom, second for top
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
            info.arrayIndex = i;
            closestHitPosition = hitPosition.x;
            intersectionFound = true;
        }
    }
    return intersectionFound;
}

// Algorythm to test intersection with axis aligned 3D boxes
// The parametric form of a ray is used (origin + (dir * t))
// The function returns tNear (x) at which the ray enters the box
// and the tFar (y) at which the ray leaves the box
// with t being the distance
// If the ray does not hit the box: tFar will be less than tNear
// If the box lies behind the ray: tNear will be negative
vec2 intersectBox(ray cameraRay, const box b) {
    vec3 tMin = (b.min - cameraRay.origin) / cameraRay.direction; // distance between ray origin and box min
    vec3 tMax = (b.max - cameraRay.origin) / cameraRay.direction; // distance between ray origin and box max
    vec3 t1 = min(tMin, tMax); // let t1 be the smaller distance
    vec3 t2 = max(tMin, tMax); // let t2 be the bigger distance
    float tNear = max(max(t1.x, t1.y), t1.z); // calculate where the ray enters the box
    float tFar = min(min(t2.x, t2.y), t2.z); // calculate where the ray leaves the box
    return vec2(tNear, tFar);
}

// The function computes the nearest intersection with a sphere
// considering all spheres in the scene
// The hitInfo struct is used to return the information as out parameter (function out, not shader out)
bool intersectSceneSpheres(ray cameraRay, out hitInfo info) {
    float closestHitPosition = MAX_SCENE_BOUNDS; // the value of closestHitPosition will be changed when a more close box is hit (determines the closest box -> every single ray has to use it's closest visible box for it's pixel)
    bool intersectionFound = false;

    for(int i = 0; i < SPHERE_COUNT; i++) {
        float hitDistance = intersectSphere(cameraRay, sceneSpheres[i]);

        if (hitDistance != -1.0 && hitDistance < closestHitPosition) {
            info.hitPosition = vec2(hitDistance, 0.0); // intersectSphere only calculates the fNear (hitDistance) for now
            info.arrayIndex = i;
            closestHitPosition = hitDistance;
            intersectionFound = true;
        }
    }

    return intersectionFound;
}

// Algorythm to test intersection with spheres
// The algorithm makes use of the "mitternachtsformel"
// The function returns the distance from rayOrigin to the hitPoint at which the ray enters the sphere
// If the ray does not hit the sphere: the discriminant will be negative and -1 will be returned
float intersectSphere(ray cameraRay, const sphere s) {
    vec3 toOriginVec = cameraRay.origin - s.center;

    float a = dot(cameraRay.direction, cameraRay.direction);
    float b = 2.0 * dot(toOriginVec, cameraRay.direction);
    float c = dot(toOriginVec,toOriginVec) - s.radius*s.radius;
    float discriminant = (b*b) - (4.0*a*c);

    if(discriminant < 0.0){
        return -1.0;
    }
    else{
        return (-b - sqrt(discriminant)) / (2.0*a);
    }
}

