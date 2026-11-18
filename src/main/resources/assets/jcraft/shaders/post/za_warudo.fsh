#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ZaWarudoConfig {
    float DesaturateAmount;
    float ChromaticShift;
    float VignetteStrength;
};

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 delta = texCoord - center;
    float dist = length(delta);
    vec2 dir = dist > 0.0001 ? delta / dist : vec2(0.0);

    float shift = ChromaticShift * (0.35 + dist * 1.4);
    float r = texture(InSampler, clamp(texCoord + dir * shift, 0.001, 0.999)).r;
    float g = texture(InSampler, texCoord).g;
    float b = texture(InSampler, clamp(texCoord - dir * shift, 0.001, 0.999)).b;
    vec3 col = vec3(r, g, b);

    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 desat = vec3(luma) * vec3(0.88, 0.91, 1.06);
    vec3 mixed = mix(col, desat, DesaturateAmount);

    float vig = smoothstep(0.95, 0.25, dist * (1.0 + VignetteStrength * 0.35));
    mixed *= mix(0.76, 1.04, vig);

    fragColor = vec4(clamp(mixed, 0.0, 1.0), 1.0);
}
