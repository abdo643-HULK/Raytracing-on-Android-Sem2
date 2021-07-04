#version 310 es

//DEBUG_NAME: compute_shader.glsl

// Defining the number of cubes and spheres in the scene
// Must match the count numbers in ComputeShaderProgram.java
#define CUBE_COUNT 3
#define SPHERE_COUNT 3

// Defining the maximum bounds of the scene
#define MAX_SCENE_BOUNDS 1000.0

// Defining the maximum amount of bounces of the ray
#define MAX_BOUNCES 20

// Defining how far the scattered rays can randomly spray (with 0 being no random behaviour at all)
#define SPRAY_CONTROL 1.0

// Defining how often the scene will be rendered (before being averaged)
#define MULTI_SAMPLING_COUNT 5

// Defining the local work group size of the compute shader (must be a power of two)
layout (local_size_x = 8, local_size_y = 8) in;

// Getting the uniform location of the framebuffer and setting its uniform value to 0
// rgba32f sets the image format qualifier for the image2D to rgba 32bit floating point
// This image2D represents the framebuffer that this shader will right to
layout(rgba32f, binding = 0) uniform highp writeonly image2D u_FrameBuffer;

// The camera position
uniform vec3 u_CameraPosition;

// The four corner rays
uniform vec3 u_Ray00;
uniform vec3 u_Ray10;
uniform vec3 u_Ray01;
uniform vec3 u_Ray11;

// The cube properties
uniform vec3 cubeMinArray[CUBE_COUNT];
uniform vec3 cubeMaxArray[CUBE_COUNT];
uniform vec3 cubeColorArray[CUBE_COUNT];
uniform int cubeMaterialArray[CUBE_COUNT];
uniform float cubeParameter0Array[CUBE_COUNT];

// The sphere properties
uniform vec3 sphereCenterArray[SPHERE_COUNT];
uniform float sphereRadiusArray[SPHERE_COUNT];
uniform vec3 sphereColorArray[SPHERE_COUNT];
uniform int sphereMaterialArray[SPHERE_COUNT];
uniform float sphereParameter0Array[SPHERE_COUNT];

// ----- STRUCTS -----
// Ray defined by it's origin and it's direction
struct ray {
    vec3 origin;
    vec3 direction;
};

// Intersection hit information of hit objects
struct hitInfo {
    int arrayIndex;// index of the hit object (can be used to access the uniform arrays)
    float t;// distance from origin to the point where we enter the object
    vec3 p;// point where the object is first hit (enter, not leave)
    vec3 normal;// normal vector on the object
};

// ----- FUNCTION DECLERATIONS -----
// Glsl works similar to c, so one has to declare functions like so or put them above the main method
vec3 trace(ray cameraRay, int sampleIndex);
bool intersectSceneCubes(ray cameraRay, out hitInfo info);
vec2 intersectCube(ray cameraRay, int i);
bool intersectSceneSpheres(ray cameraRay, out hitInfo info);
float intersectSphere(ray cameraRay, int i);
vec3 getPointFromRay(ray cameraRay, float t);
vec3 getRandomPoint(vec3 p, int sampleIndex);
float squaredLength(vec3 p);
float drand(vec2 co);

// ----- MAIN -----
// The main function (shader program entry point)
void main(void) {
    ivec2 shaderDomainPosition = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(u_FrameBuffer);

    // mix performs a linear interpolation between param 1 and 2 using param 3 as weight
    // therefore we "move" from left to right, and top to bottom through all of the available texels
    // and calculate a direction ray for each of them -> the current shader invocation knows where to shoot the current ray
    vec2 position = vec2(shaderDomainPosition) / vec2(size.x, size.y);
    vec3 direction = mix(mix(u_Ray00, u_Ray01, position.y), mix(u_Ray10, u_Ray11, position.y), position.x);

    ray cameraRay;
    cameraRay.origin = u_CameraPosition;
    cameraRay.direction = direction;

    vec3 color;

    for (int i = 0; i < MULTI_SAMPLING_COUNT; i++) {
        color += trace(cameraRay, i);
    }
    color /= float(MULTI_SAMPLING_COUNT);

    imageStore(u_FrameBuffer, shaderDomainPosition, vec4(color, 1));
}

// The function computes the amount of light that a given ray contributes when perceived by the eye
// So, any ray that will be used as input originates in the eye and goes through the framebuffer texel
// that is being computed in the current shader invocation
// If an object was hit it returns its color
// If nothing was hit it returns an artificial sky color, depending on the rays direction height
vec3 trace(ray cameraRay, int sampleIndex) {
    vec3 color;
    vec3 attenuation = vec3(1.0, 1.0, 1.0);

    ray scatteredRay = cameraRay;// starts with the camera ray but continues with the scattered rays

    float previousParameter0 = -1.0;

    // GLSL doesn't support recursion hence the following for loop is necessary
    for (int bounce = 0; bounce < MAX_BOUNCES; bounce++) {
        hitInfo cubeHitInfo;
        hitInfo sphereHitInfo;

        cubeHitInfo.t = -1.0;
        sphereHitInfo.t = -1.0;

        bool hitSomething = false;

        if (intersectSceneCubes(scatteredRay, cubeHitInfo)){
            hitSomething = true;
        }

        if (intersectSceneSpheres(scatteredRay, sphereHitInfo)){
            hitSomething = true;
        }

        if (hitSomething) {
            vec3 scatteredPoint;

            if (((cubeHitInfo.t != -1.0 && sphereHitInfo.t != -1.0) && cubeHitInfo.t < sphereHitInfo.t) || (cubeHitInfo.t != -1.0 && sphereHitInfo.t == -1.0)){ //only cube hit or both hit but cube hit is closer
                //first hit / color
                if (bounce == 0){
                    color = cubeColorArray[cubeHitInfo.arrayIndex];
                    if (cubeMaterialArray[cubeHitInfo.arrayIndex] == 0) {
                    } else if (cubeMaterialArray[cubeHitInfo.arrayIndex] == 1) { // Metal Material
                        previousParameter0 = cubeParameter0Array[cubeHitInfo.arrayIndex];
                    }
                } else {
                    if (cubeMaterialArray[cubeHitInfo.arrayIndex] == 0) { // Diffuse Material
                        if (previousParameter0 != -1.0) {
                            color = (color * (1.0 - previousParameter0) + cubeColorArray[cubeHitInfo.arrayIndex] * previousParameter0);
                            attenuation *= 1.0 - cubeParameter0Array[cubeHitInfo.arrayIndex];
                            previousParameter0 = -1.0;
                        } else {
                            attenuation *= cubeParameter0Array[cubeHitInfo.arrayIndex];
                        }
                    } else if (cubeMaterialArray[cubeHitInfo.arrayIndex] == 1) { // Metal Material
                        if (previousParameter0 != -1.0) {
                            color = (color * (1.0 - previousParameter0) + cubeColorArray[cubeHitInfo.arrayIndex] * previousParameter0);
                            previousParameter0 *= cubeParameter0Array[cubeHitInfo.arrayIndex];
                        }
                    }
                }

                if (cubeMaterialArray[cubeHitInfo.arrayIndex] == 0) {
                    scatteredPoint = cubeHitInfo.p + cubeHitInfo.normal + getRandomPoint(cubeHitInfo.p, sampleIndex);
                    scatteredRay = ray(cubeHitInfo.p, scatteredPoint - cubeHitInfo.p);
                } else if (cubeMaterialArray[cubeHitInfo.arrayIndex] == 1) {
                    vec3 incomingDirection = (cubeHitInfo.p - scatteredRay.origin);
                    vec3 reflectionDirection = incomingDirection - 2.0 * dot(incomingDirection, cubeHitInfo.normal) * cubeHitInfo.normal;
                    scatteredRay = ray(cubeHitInfo.p, reflectionDirection);
                }
            } else if (((cubeHitInfo.t != -1.0 && sphereHitInfo.t != -1.0) && sphereHitInfo.t < cubeHitInfo.t) || (sphereHitInfo.t != -1.0 && cubeHitInfo.t == -1.0)){ //only sphere hit or both hit but sphere hit is closer
                //first hit / color
                if (bounce == 0){
                    color = sphereColorArray[sphereHitInfo.arrayIndex];
                    if (sphereMaterialArray[sphereHitInfo.arrayIndex] == 0) {
                    } else if (sphereMaterialArray[sphereHitInfo.arrayIndex] == 1) { // Metal Material
                        previousParameter0 = sphereParameter0Array[sphereHitInfo.arrayIndex];
                    }
                } else {
                    if (sphereMaterialArray[sphereHitInfo.arrayIndex] == 0) { // Diffuse Material
                        if (previousParameter0 != -1.0) {
                            color = (color * (1.0 - previousParameter0) + sphereColorArray[sphereHitInfo.arrayIndex] * previousParameter0);
                            attenuation *= 1.0 - sphereParameter0Array[sphereHitInfo.arrayIndex];
                            previousParameter0 = -1.0;
                        } else {
                            attenuation *= sphereParameter0Array[sphereHitInfo.arrayIndex];
                        }
                    } else if (sphereMaterialArray[sphereHitInfo.arrayIndex] == 1) { // Metal Material
                        if (previousParameter0 != -1.0) {
                            color = (color * (1.0 - previousParameter0) + sphereColorArray[sphereHitInfo.arrayIndex] * previousParameter0);
                            previousParameter0 *= sphereParameter0Array[sphereHitInfo.arrayIndex];
                        }
                    }
                }

                if (sphereMaterialArray[sphereHitInfo.arrayIndex] == 0) {
                    scatteredPoint = sphereHitInfo.p + sphereHitInfo.normal + getRandomPoint(sphereHitInfo.p, sampleIndex);
                    scatteredRay = ray(sphereHitInfo.p, scatteredPoint - sphereHitInfo.p);
                } else if (sphereMaterialArray[sphereHitInfo.arrayIndex] == 1) {
                    vec3 incomingDirection = (sphereHitInfo.p - scatteredRay.origin);
                    vec3 reflectionDirection = incomingDirection - 2.0 * dot(incomingDirection, sphereHitInfo.normal) * sphereHitInfo.normal;
                    scatteredRay = ray(sphereHitInfo.p, reflectionDirection);
                }
            }
        } else {
            vec3 primarySkyColor = vec3(1.0, 1.0, 1.0);
            vec3 secondarySkyColor = vec3(0.1, 0.5, 0.8);

            // Sky was hit
            if (bounce == 0){
                vec3 unit_direction = normalize(cameraRay.direction);
                vec3 skyColor = (1.0 - unit_direction.y) * primarySkyColor + unit_direction.y * secondarySkyColor;

                return skyColor;
            } else if (previousParameter0 != -1.0) { // Metal was hit before this sky hit
                vec3 unit_direction = normalize(scatteredRay.direction);
                vec3 skyColor = (1.0 - unit_direction.y) * primarySkyColor + unit_direction.y * secondarySkyColor;

                color =  (color * (1.0 - previousParameter0) + skyColor * previousParameter0);
            } else {
                break;
            }
        }
    }

    return color * attenuation;
}

// The function computes the nearest intersection with a cube
// considering all cubes in the scene
// The hitInfo struct is used to return the information as out parameter (function out, not shader out)
bool intersectSceneCubes(ray cameraRay, out hitInfo info) {
    bool intersectionFound = false;
    info.t = MAX_SCENE_BOUNDS;// the value of closestHitPosition will be changed when a more close cube is hit (determines the closest cube -> every single ray has to use it's closest visible cube for it's pixel)

    for (int i = 0; i < CUBE_COUNT; i++) {
        vec2 hitPosition = intersectCube(cameraRay, i);

        if (hitPosition.x > 0.0 && hitPosition.x < hitPosition.y && hitPosition.x < info.t) { // see the last two lines of explanation comment of intersectCube
            info.arrayIndex = i;
            info.t = hitPosition.x;

            intersectionFound = true;
        }
    }

    if (intersectionFound) {
        // Normal-Calculation
        info.p = getPointFromRay(cameraRay, info.t);

        vec3 cubeCenter = (cubeMinArray[info.arrayIndex] + cubeMaxArray[info.arrayIndex]) / 2.0;
        vec3 posHitPoint = getPointFromRay(cameraRay, info.t) - cubeCenter;

        float delta = 0.0001;// to make up for floating point precission errors

        float posXPlane = (cubeMaxArray[info.arrayIndex].x - cubeMinArray[info.arrayIndex].x) / 2.0;
        if (posHitPoint.x >= posXPlane + delta || posHitPoint.x >= posXPlane - delta) {
            info.normal = vec3(1.0, 0.0, 0.0);
        }
        if (posHitPoint.x <= -posXPlane + delta || posHitPoint.x <= -posXPlane - delta) {
            info.normal = vec3(-1.0, 0.0, 0.0);
        }

        float posYPlane = (cubeMaxArray[info.arrayIndex].y - cubeMinArray[info.arrayIndex].y) / 2.0;
        if (posHitPoint.y >= posYPlane + delta || posHitPoint.y >= posYPlane - delta) {
            info.normal = vec3(0.0, 1.0, 0.0);
        }
        if (posHitPoint.y <= -posYPlane + delta || posHitPoint.y <= -posYPlane - delta) {
            info.normal = vec3(0.0, -1.0, 0.0);
        }

        float posZPlane = (cubeMaxArray[info.arrayIndex].z - cubeMinArray[info.arrayIndex].z) / 2.0;
        if (posHitPoint.z >= posZPlane + delta || posHitPoint.z >= posZPlane - delta) {
            info.normal = vec3(0.0, 0.0, 1.0);
        }
        if (posHitPoint.z <= -posZPlane + delta || posHitPoint.z <= -posZPlane - delta) {
            info.normal = vec3(0.0, 0.0, -1.0);
        }
    }

    return intersectionFound;
}

// Algorythm to test intersection with axis aligned 3D cubes
// The parametric form of a ray is used (origin + (dir * t))
// The function returns tNear (x) at which the ray enters the cube
// and the tFar (y) at which the ray leaves the cube
// with t being the distance
// If the ray does not hit the cube: tFar will be less than tNear
// If the cube lies behind the ray: tNear will be negative
vec2 intersectCube(ray cameraRay, int i) {
    vec3 tMin = (cubeMinArray[i] - cameraRay.origin) / cameraRay.direction;// distance between ray origin and cube min
    vec3 tMax = (cubeMaxArray[i] - cameraRay.origin) / cameraRay.direction;// distance between ray origin and cube max
    vec3 t1 = min(tMin, tMax);// let t1 be the smaller distance
    vec3 t2 = max(tMin, tMax);// let t2 be the bigger distance
    float tNear = max(max(t1.x, t1.y), t1.z);// calculate where the ray enters the cube
    float tFar = min(min(t2.x, t2.y), t2.z);// calculate where the ray leaves the cube

    return vec2(tNear, tFar);
}

// The function computes the nearest intersection with a sphere
// considering all spheres in the scene
// The hitInfo struct is used to return the information as out parameter (function out, not shader out)
bool intersectSceneSpheres(ray cameraRay, out hitInfo info) {
    info.t = MAX_SCENE_BOUNDS;// the value of closestHitPosition will be changed when a more close cube is hit (determines the closest cube -> every single ray has to use it's closest visible cube for it's pixel)
    bool intersectionFound = false;

    for (int i = 0; i < SPHERE_COUNT; i++) {
        float hitDistance = intersectSphere(cameraRay, i);

        if (hitDistance > 0.0 && hitDistance < info.t) {
            info.arrayIndex = i;
            info.t = hitDistance;
            intersectionFound = true;
        }
    }

    if (intersectionFound) {
        info.p = getPointFromRay(cameraRay, float(info.t));
        info.normal = ((info.p - sphereCenterArray[info.arrayIndex]) / sphereRadiusArray[info.arrayIndex]);
    }

    return intersectionFound;
}

// Algorythm to test intersection with spheres
// The algorithm makes use of the "mitternachtsformel"
// The function returns the distance from rayOrigin to the hitPoint at which the ray enters the sphere
// If the ray does not hit the sphere: the discriminant will be negative and -1 will be returned
// Attention: the function can return negative values other than -1.0
float intersectSphere(ray cameraRay, int i) {
    vec3 toOriginVec = cameraRay.origin - sphereCenterArray[i];

    float a = dot(cameraRay.direction, cameraRay.direction);
    float b = 2.0 * dot(toOriginVec, cameraRay.direction);
    float c = dot(toOriginVec, toOriginVec) - sphereRadiusArray[i]*sphereRadiusArray[i];
    float discriminant = (b*b) - (4.0*a*c);

    if (discriminant < 0.0){
        return -1.0;
    }
    else {
        return (-b - sqrt(discriminant)) / (2.0*a);
    }
}

//returns Point from Ray at certain distance t
vec3 getPointFromRay(ray r, float t) {
    return r.origin + t * r.direction;
}

//to use instead of drand48 - returns pseudo random
//returns a float from -1.0 to 1.0
//relies on the sample index to be truly random from sample to sample
float drand(vec2 co) {
    float a = 2.0 * fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453) - 1.0;
    float s = a *(6.18278114200511 + a*a *(-38.026124606 + a * a * 53.392573080032137));
    return fract(s * 43758.5453);
}

//to use instead of .squared_length
float squaredLength(vec3 p) {
    return p.x * p.x + p.y * p.y + p.z * p.z;
}

//return random vector with a length < 1; the vector is used as point
//input p has no direct influence on the output - it is only used in drand()
vec3 getRandomPoint(vec3 p, int sampleIndex) {
    do {
        p = 2.0 * vec3(drand(p.xz), drand(p.xy), drand(p.zy)) - vec3(1, 1, 1);//min -3 max 1
    } while (squaredLength(p) >= 1.0);

    float perSampleRandomizer = (drand(p.xz) + drand(p.xy) + drand(p.zy)) / 3.0;
    float x;
    float y;
    float z;

    if (sampleIndex > 0) {
        for (int i = 0; i<sampleIndex; i++) {
            perSampleRandomizer = drand(vec2(perSampleRandomizer*float(i/sampleIndex), perSampleRandomizer*float(sampleIndex/i)));

            if (i <= sampleIndex / 3 * 1) {
                x = perSampleRandomizer;
            } else if (i <= sampleIndex / 3 * 2) {
                y = perSampleRandomizer;
            } else if (i <= sampleIndex / 3 * 3) {
                z = perSampleRandomizer;
            }
        }

        p *= vec3(x-0.5, y-0.5, z-0.5);// all values should be random and between -0.5 and +0.5
    }

    p *= SPRAY_CONTROL;
    return p;
}