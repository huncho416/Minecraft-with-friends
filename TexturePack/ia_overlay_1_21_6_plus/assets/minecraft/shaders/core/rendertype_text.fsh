#version 150
#define FSH
#define RENDERTYPE_TEXT

// Import globals first to get GameTime and ScreenSize
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

// Custom uniforms for spheya_packs compatibility
uniform float FogStart;
uniform float FogEnd;

// Samplers
uniform sampler2D Sampler0;

// Inputs from vertex shader
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in float vertexDistance;
in vec4 baseColor;
in vec4 lightColor;

out vec4 fragColor;

// Compatibility function for old linear_fog using vanilla fog functions
vec4 linear_fog(vec4 color, float distance, float start, float end, vec4 fogColor) {
    float fogValue = linear_fog_value(distance, start, end);
    return vec4(mix(color.rgb, fogColor.rgb, fogValue * fogColor.a), color.a);
}

// Import spheya packs after all uniforms are declared
#moj_import <spheya_packs_impl.glsl>

void main() {
    // Apply spheya packs (which includes text effects with animations)
    if(applySpheyaPacks()) return;

    // Vanilla rendering code - exactly matching vanilla behavior
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    
    if (color.a < 0.1) {
        discard;
    }
    
    // Match vanilla 1.21.6 fog application exactly
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, 
                         FogEnvironmentalStart, FogEnvironmentalEnd, 
                         FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}