#version 150

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float vertexDistance;
flat out vec4 vertexColor;

void main() {
    //Fix by DartCat25
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);

    vertexColor = Color * ColorModulator * texelFetch(Sampler2, UV2 / 16, 0) * float((gl_VertexID) % 100 > 1);
}