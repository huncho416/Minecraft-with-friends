#version 150

#moj_import <minecraft:fog.glsl>

// Custom uniforms for compatibility with linear_fog
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
flat in vec4 vertexColor;

out vec4 fragColor;

// Compatibility function for old linear_fog
vec4 linear_fog(vec4 color, float distance, float start, float end, vec4 fogColor) {
    if (start >= end) return color;
    float fogFactor = (end - distance) / (end - start);
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    return mix(fogColor, color, fogFactor);
}

void main() {
    //Fix by DartCat25

    if (vertexColor.a == 0.0)
        discard;

    fragColor = linear_fog(vertexColor, vertexDistance, FogStart, FogEnd, FogColor);
}