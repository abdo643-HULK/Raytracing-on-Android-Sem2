#version 310 es

//DEBUG_NAME: to_screen_vertex_shader

layout(location=0) in vec2 a_Position;

out vec2 v_TextureCoordinates;

void main(){

    gl_Position = vec4(a_Position, 0.0, 1.0);
    v_TextureCoordinates = vec2((a_Position.x+1.0)/2.0, (a_Position.y+1.0)/2.0);
}