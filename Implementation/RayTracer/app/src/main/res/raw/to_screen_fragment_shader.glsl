#version 310 es

//DEBUG_NAME: to_screen_fragment_shader

precision highp float;

in vec2 v_TextureCoordinates;
out vec4 fragmentColor;

uniform sampler2D u_TextureUnit;

void main(void){
    fragmentColor = texture(u_TextureUnit, v_TextureCoordinates);
}