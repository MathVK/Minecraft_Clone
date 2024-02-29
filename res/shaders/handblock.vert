#version 330

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexture;
layout (location = 2) in float aIndex;

out vec2 textureCoords;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform vec2 tex0, tex1, tex2, tex3;

bool equals(float a, float b) {
    return abs(a - b) < 1e-5;
}

void main() {

    vec2 texturePosition;
    if (equals(aIndex, 0.0f)) {
        texturePosition = tex0;
    } else if (equals(aIndex, 1.0f)) {
        texturePosition = tex1;
    } else if (equals(aIndex, 2.0f)) {
        texturePosition = tex2;
    } else if (equals(aIndex, 3.0f)) {
        texturePosition = tex3;
    }

    gl_Position = projection * view * model * vec4(aPosition, 1.0);
    textureCoords = texturePosition;

}