#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BtdConfig {
    float RewindTint;
};

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 src = texture(InSampler, texCoord);
    float luma = dot(src.rgb, vec3(0.299, 0.587, 0.114));
    vec3 duo = mix(vec3(0.32, 0.10, 0.45), vec3(1.0, 0.86, 0.42), clamp(luma * 1.2, 0.0, 1.0));
    vec3 outCol = mix(src.rgb, duo, RewindTint);
    fragColor = vec4(clamp(outCol, 0.0, 1.0), 1.0);
}
