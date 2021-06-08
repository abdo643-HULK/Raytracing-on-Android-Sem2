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

// Defining how many rays to send out on hit of an object (before being averaged)
#define RAY_SCATTER_COUNT 1

// Defining how often the scene will be rendered (before being averaged)
#define MULTI_SAMPLING_COUNT 1

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

// Intersection hit information defined by hitPosition (the vec2 returned by intersectCube) and cubeIndex (the index of the cube that was hit)
struct hitInfo {
    int arrayIndex;
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
float drand(vec2 co, int sampleIndex);

// ----- MAIN -----
// The main function (shader program entry point)
void main(void) {
    // Camera initialization
    vec3 cameraPosition = (u_InvertedViewMatrix * vec4(0.0, 0.0, 0.0, 1.0)).xyz;// once again assuming w is 1

    // The rays are defined as vec4s so that mat4 multiplication and perspective divide (dividing by w component) is possible (Note: these are device coordinates (screen coordinates) with 1 as w)
    vec4 u_Ray00 = vec4(-1, -1, 0, 1);// left, bottom
    vec4 u_Ray10 = vec4(+1, -1, 0, 1);// right, bottom
    vec4 u_Ray01 = vec4(-1, +1, 0, 1);// left, top
    vec4 u_Ray11 = vec4(+1, +1, 0, 1);// right, top

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

    if (shaderDomainPosition.x >= size.x || shaderDomainPosition.y >= size.y) {
        return;// exit if the current shader invocation is out of bounds of the framebuffer
    }

    // mix performs a linear interpolation between param 1 and 2 using param 3 as weight
    // therefore we "move" from left to right, and top to bottom through all of the available texels
    // and calculate a direction ray for each of them -> the current shader invocation knows where to shoot the current ray
    vec2 position = vec2(shaderDomainPosition) / vec2(size.x, size.y);
    vec3 direction = mix(mix(u_Ray00.xyz, u_Ray01.xyz, position.y), mix(u_Ray10.xyz, u_Ray11.xyz, position.y), position.x);

    ray cameraRay;
    cameraRay.origin = cameraPosition;
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
    hitInfo cubeHitInfo;
    hitInfo sphereHitInfo;

    vec3 color;
    vec3 attenuation = vec3(1.0, 1.0, 1.0);

    ray scatteredRay = cameraRay;// starts with the camera ray but continues with the scattered rays

    float previousParameter0 = -1.0;

    bool hitOnlyMetal = true;

    // GLSL doesn't support recursion hence the following for loop is necessary
    for (int bounce = 0; bounce < MAX_BOUNCES; bounce++) {
        float bufferAttenuation = 0.0;

        for (int scatter = 0; scatter < RAY_SCATTER_COUNT; scatter++) {
            hitInfo sphereHitBuffer;
            hitInfo cubeHitBuffer;

            sphereHitBuffer.t = -1.0;
            cubeHitBuffer.t = -1.0;

            bool hitSomething = false;

            if (intersectSceneSpheres(scatteredRay, sphereHitInfo)){
                hitSomething = true;

                if (sphereHitInfo.t < sphereHitBuffer.t || sphereHitBuffer.t == -1.0){
                    sphereHitBuffer = sphereHitInfo;
                }
            }

            if (intersectSceneCubes(scatteredRay, cubeHitInfo)){
                hitSomething = true;

                if (cubeHitInfo.t < cubeHitBuffer.t || cubeHitBuffer.t == -1.0){
                    cubeHitBuffer = cubeHitInfo;
                }
            }

            if (hitSomething) {
                vec3 scatteredPoint;

                if (cubeHitBuffer.t > 0.0 && sphereHitBuffer.t > 0.0) { // both a cube and a sphere got hit
                    if (cubeHitBuffer.t < sphereHitBuffer.t){
                        //first hit / color
                        if (bounce == 0){
                            if (scatter == 0) {
                                color = cubeColorArray[cubeHitBuffer.arrayIndex];
                                if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 0) {
                                    hitOnlyMetal = false;
                                } else if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 1) { // Metal Material
                                    previousParameter0 = cubeParameter0Array[cubeHitBuffer.arrayIndex];
                                }
                            }
                        } else {
                            if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 0) { // Diffuse Material
                                hitOnlyMetal = false;
                                if (previousParameter0 != -1.0) {
                                    color = (color * (1.0 - previousParameter0) + cubeColorArray[cubeHitBuffer.arrayIndex] * previousParameter0);
                                    bufferAttenuation += 1.0 - cubeParameter0Array[cubeHitBuffer.arrayIndex];
                                    previousParameter0 = -1.0;
                                } else {
                                    bufferAttenuation += cubeParameter0Array[cubeHitBuffer.arrayIndex];
                                }
                            } else if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 1) { // Metal Material
                                if (previousParameter0 != -1.0) {
                                    color = (color * (1.0 - previousParameter0) + cubeColorArray[cubeHitBuffer.arrayIndex] * previousParameter0);
                                    previousParameter0 *= cubeParameter0Array[cubeHitBuffer.arrayIndex];
                                }
                            }
                        }

                        if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 0) {
                            scatteredPoint = cubeHitBuffer.p + cubeHitBuffer.normal + getRandomPoint(cubeHitBuffer.p, sampleIndex + scatter);
                            scatteredRay = ray(cubeHitBuffer.p, scatteredPoint - cubeHitBuffer.p);
                        } else if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 1) {
                            vec3 incomingDirection = (cubeHitBuffer.p - scatteredRay.origin);
                            vec3 reflectionDirection = incomingDirection - 2.0 * dot(incomingDirection, cubeHitBuffer.normal) * cubeHitBuffer.normal;
                            scatteredRay = ray(cubeHitBuffer.p, reflectionDirection);
                        }

                    } else {
                        //first hit / color
                        if (bounce == 0){
                            if (scatter == 0) {
                                color = sphereColorArray[sphereHitBuffer.arrayIndex];
                                if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 0) {
                                    hitOnlyMetal = false;
                                } else if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 1) { // Metal Material
                                    previousParameter0 = sphereParameter0Array[sphereHitBuffer.arrayIndex];
                                }
                            }
                        } else {
                            if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 0) { // Diffuse Material
                                hitOnlyMetal = false;
                                if (previousParameter0 != -1.0) {
                                    color = (color * (1.0 - previousParameter0) + sphereColorArray[sphereHitBuffer.arrayIndex] * previousParameter0);
                                    bufferAttenuation += 1.0 - sphereParameter0Array[sphereHitBuffer.arrayIndex];
                                    previousParameter0 = -1.0;
                                } else {
                                    bufferAttenuation += sphereParameter0Array[sphereHitBuffer.arrayIndex];
                                }
                            } else if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 1) { // Metal Material
                                if (previousParameter0 != -1.0) {
                                    color = (color * (1.0 - previousParameter0) + sphereColorArray[sphereHitBuffer.arrayIndex] * previousParameter0);
                                    previousParameter0 *= sphereParameter0Array[sphereHitBuffer.arrayIndex];
                                }
                            }
                        }

                        if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 0) {
                            scatteredPoint = sphereHitBuffer.p + sphereHitBuffer.normal + getRandomPoint(sphereHitBuffer.p, sampleIndex + scatter);
                            scatteredRay = ray(sphereHitBuffer.p, scatteredPoint - sphereHitBuffer.p);
                        } else if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 1) {
                            vec3 incomingDirection = (sphereHitBuffer.p - scatteredRay.origin);
                            vec3 reflectionDirection = incomingDirection - 2.0 * dot(incomingDirection, sphereHitBuffer.normal) * sphereHitBuffer.normal;
                            scatteredRay = ray(sphereHitBuffer.p, reflectionDirection);
                        }
                    }
                } else if (cubeHitBuffer.t != -1.0){ //only cube hit
                    //first hit / color
                    if (bounce == 0){
                        if (scatter == 0) {
                            color = cubeColorArray[cubeHitBuffer.arrayIndex];
                            if(cubeMaterialArray[cubeHitBuffer.arrayIndex] == 0) {
                                hitOnlyMetal = false;
                            } else if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 1) { // Metal Material
                                previousParameter0 = cubeParameter0Array[cubeHitBuffer.arrayIndex];
                            }
                        }
                    } else {
                        if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 0) { // Diffuse Material
                            hitOnlyMetal = false;
                            if (previousParameter0 != -1.0) {
                                color = (color * (1.0 - previousParameter0) + cubeColorArray[cubeHitBuffer.arrayIndex] * previousParameter0);
                                bufferAttenuation += 1.0 - cubeParameter0Array[cubeHitBuffer.arrayIndex];
                                previousParameter0 = -1.0;
                            } else {
                                bufferAttenuation += cubeParameter0Array[cubeHitBuffer.arrayIndex];
                            }
                        } else if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 1) { // Metal Material
                            if (previousParameter0 != -1.0) {
                                color = (color * (1.0 - previousParameter0) + cubeColorArray[cubeHitBuffer.arrayIndex] * previousParameter0);
                                previousParameter0 *= cubeParameter0Array[cubeHitBuffer.arrayIndex];
                            }
                        }
                    }

                    if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 0) {
                        scatteredPoint = cubeHitBuffer.p + cubeHitBuffer.normal + getRandomPoint(cubeHitBuffer.p, sampleIndex + scatter);
                        scatteredRay = ray(cubeHitBuffer.p, scatteredPoint - cubeHitBuffer.p);
                    } else if (cubeMaterialArray[cubeHitBuffer.arrayIndex] == 1) {
                        vec3 incomingDirection = (cubeHitBuffer.p - scatteredRay.origin);
                        vec3 reflectionDirection = incomingDirection - 2.0 * dot(incomingDirection, cubeHitBuffer.normal) * cubeHitBuffer.normal;
                        scatteredRay = ray(cubeHitBuffer.p, reflectionDirection);
                    }
                } else { //only sphere hit
                    //first hit / color
                    if (bounce == 0){
                        if (scatter == 0) {
                            color = sphereColorArray[sphereHitBuffer.arrayIndex];
                            if(sphereMaterialArray[sphereHitBuffer.arrayIndex] == 0) {
                                hitOnlyMetal = false;
                            } else if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 1) { // Metal Material
                                previousParameter0 = sphereParameter0Array[sphereHitBuffer.arrayIndex];
                            }
                        }
                    } else {
                        if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 0) { // Diffuse Material
                            hitOnlyMetal = false;
                            if (previousParameter0 != -1.0) {
                                color = (color * (1.0 - previousParameter0) + sphereColorArray[sphereHitBuffer.arrayIndex] * previousParameter0);
                                bufferAttenuation += 1.0 - sphereParameter0Array[sphereHitBuffer.arrayIndex];
                                previousParameter0 = -1.0;
                            } else {
                                bufferAttenuation += sphereParameter0Array[sphereHitBuffer.arrayIndex];
                            }
                        } else if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 1) { // Metal Material
                            if (previousParameter0 != -1.0) {
                                color = (color * (1.0 - previousParameter0) + sphereColorArray[sphereHitBuffer.arrayIndex] * previousParameter0);
                                previousParameter0 *= sphereParameter0Array[sphereHitBuffer.arrayIndex];
                            }
                        }
                    }

                    if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 0) {
                        scatteredPoint = sphereHitBuffer.p + sphereHitBuffer.normal + getRandomPoint(sphereHitBuffer.p, sampleIndex + scatter);
                        scatteredRay = ray(sphereHitBuffer.p, scatteredPoint - sphereHitBuffer.p);
                    } else if (sphereMaterialArray[sphereHitBuffer.arrayIndex] == 1) {
                        vec3 incomingDirection = (sphereHitBuffer.p - scatteredRay.origin);
                        vec3 reflectionDirection = incomingDirection - 2.0 * dot(incomingDirection, sphereHitBuffer.normal) * sphereHitBuffer.normal;
                        scatteredRay = ray(sphereHitBuffer.p, reflectionDirection);
                    }
                }
            } else {
                hitOnlyMetal = false;
                vec3 primarySkyColor = vec3(1.0, 1.0, 1.0);
                vec3 secondarySkyColor = vec3(0.1, 0.5, 0.8);

                // Sky (can set the skycolour but will defenitely break as there can not be any more ray bounces)
                if (bounce == 0 && scatter == 0){
                    vec3 unit_direction = normalize(cameraRay.direction);
                    vec3 skyColor = (1.0 - unit_direction.y) * primarySkyColor + unit_direction.y * secondarySkyColor;

                    return skyColor;
                } else if (previousParameter0 != -1.0) {
                    vec3 unit_direction = normalize(scatteredRay.direction);
                    vec3 skyColor = (1.0 - unit_direction.y) * primarySkyColor + unit_direction.y * secondarySkyColor;

                    color =  (color * (1.0 - previousParameter0) + skyColor * previousParameter0);
                }
            }

            break;
        }

        if (bufferAttenuation > 0.0) {
            attenuation *= (bufferAttenuation / float(RAY_SCATTER_COUNT));// calculating the average attenuation of all scattered rays of this bounce
        }
    }

    if(hitOnlyMetal) {
        color = vec3(1.0, 1.0, 1.0);
    }

    return color * attenuation;
}

// The function computes the nearest intersection with a cube
// considering all cubes in the scene
// The hitInfo struct is used to return the information as out parameter (function out, not shader out)
bool intersectSceneCubes(ray cameraRay, out hitInfo info) {
    float closestHitPosition = MAX_SCENE_BOUNDS;// the value of closestHitPosition will be changed when a more close cube is hit (determines the closest cube -> every single ray has to use it's closest visible cube for it's pixel)
    bool intersectionFound = false;

    for (int i = 0; i < CUBE_COUNT; i++) {
        vec2 hitPosition = intersectCube(cameraRay, i);
        if (hitPosition.x > 0.0 && hitPosition.x < hitPosition.y && hitPosition.x < closestHitPosition) { // see the last two lines of explanation comment of intersectCube
            closestHitPosition = hitPosition.x;
            info.arrayIndex = i;

            info.t = hitPosition.x;
            info.p = getPointFromRay(cameraRay, float(info.t));

            // Normal-Calculation
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

            intersectionFound = true;
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
    float closestHitPosition = MAX_SCENE_BOUNDS;// the value of closestHitPosition will be changed when a more close cube is hit (determines the closest cube -> every single ray has to use it's closest visible cube for it's pixel)
    bool intersectionFound = false;

    for (int i = 0; i < SPHERE_COUNT; i++) {
        float hitDistance = intersectSphere(cameraRay, i);

        if (hitDistance > 0.0 && hitDistance < closestHitPosition) {
            info.arrayIndex = i;

            info.t = hitDistance;
            info.p = getPointFromRay(cameraRay, float(info.t));
            info.normal = ((info.p - sphereCenterArray[info.arrayIndex]) / sphereRadiusArray[info.arrayIndex]);

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
float drand(vec2 co, int sampleIndex) {
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
//Additionally checks for a lenght^2 smaller than 0.01 to avoid Shadow Acne
vec3 getRandomPoint(vec3 p, int sampleIndex) {
    do {
        p = 2.0 * vec3(drand(p.xz, sampleIndex), drand(p.xy, sampleIndex), drand(p.zy, sampleIndex)) - vec3(1, 1, 1);//min -3 max 1
    } while (squaredLength(p) >= 1.0);

    float perFrameAndSampleRandomizer;

    if (sampleIndex > 0) {
        perFrameAndSampleRandomizer = ((1.0 - (1.0/float(sampleIndex))));// max value = 1.0 || min value = 0.0
        perFrameAndSampleRandomizer -= 0.5;// max value = 0.5 || min value = -0.5
    }

    p += vec3(perFrameAndSampleRandomizer, perFrameAndSampleRandomizer, perFrameAndSampleRandomizer);

    p *= SPRAY_CONTROL;
    return p;
}