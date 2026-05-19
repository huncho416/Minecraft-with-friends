#version 150
#define VSH
#define RENDERTYPE_TEXT

// Import globals first to get GameTime and ScreenSize
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Custom uniforms for spheya_packs compatibility
uniform float FogStart;
uniform float FogEnd;

// Input attributes
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

// Samplers
uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

// Outputs required by 1.21.6 pipeline
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// Additional outputs for compatibility with spheya_packs_impl.glsl
out float vertexDistance;
out vec4 baseColor;
out vec4 lightColor;

// Import spheya packs after all uniforms are declared
#moj_import <spheya_packs_impl.glsl>

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Calculate distances for fog - using Position directly like vanilla 1.21.6
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    
    // Set up color data - match vanilla exactly
    vec4 light = texelFetch(Sampler2, UV2 / 16, 0);
    baseColor = Color;
    lightColor = light;
    vertexColor = Color * light;  // Direct multiplication like vanilla
    texCoord0 = UV0;

    // Apply spheya packs (which includes text effects with animations)
    if(applySpheyaPacks()) return;
}