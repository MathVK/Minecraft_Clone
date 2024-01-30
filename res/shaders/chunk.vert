#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexture;
layout (location = 2) in float aBlockID;
layout (location = 3) in float aBlockFace;
layout (location = 4) in float aOcclusion;

out vec2 textureCoord;
out float blockID;
out float blockFace;
out float brightnessFace;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

float brigthness[6] = float[6](0.85f, 0.6f, 1.0f, 0.6f, 0.85f, 0.6f); /*[px, nx, py, ny, pz, nz]*/

float occlusion[4] = float[4](0.1f, 0.25f, 0.5f, 1f);

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0);
    textureCoord = aTexture;
    blockID=aBlockID;
    blockFace=aBlockFace;
    brightnessFace = brigthness[int(blockFace)] * occlusion[int(aOcclusion)];
}