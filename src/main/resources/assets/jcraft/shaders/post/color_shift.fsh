#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ColorShiftConfig {
    float ShiftIntensity;
};

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 src = texture(InSampler, texCoord);
    // Araki stylized magenta-cyan-gold hue rotation + contrast punch
    vec3 araki = vec3(
        dot(src.rgb, vec3(0.45, 0.15, 0.55)),
        dot(src.rgb, vec3(0.15, 0.72, 0.25)),
        dot(src.rgb, vec3(0.50, 0.18, 0.48))
    );
    float luma = dot(araki, vec3(0.3, 0.59, 0.11));
    araki = clamp((araki - luma) * 1.65 + luma * 1.06, 0.0, 1.0);
    vec3 outCol = mix(src.rgb, araki, ShiftIntensity);
    fragColor = vec4(outCol, 1.0);
}
