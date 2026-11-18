#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform TimeEraseConfig {
    float CrimsonIntensity;
    float ScanlineStrength;
};

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 delta = texCoord - center;
    float dist = length(delta);

    float r = texture(InSampler, clamp(texCoord + delta * 0.015, 0.001, 0.999)).r;
    float g = texture(InSampler, texCoord).g;
    float b = texture(InSampler, clamp(texCoord - delta * 0.015, 0.001, 0.999)).b;
    vec3 col = vec3(r, g, b);

    float luma = dot(col, vec3(0.3, 0.59, 0.11));
    vec3 crimson = vec3(pow(luma, 0.85) * 1.25, luma * 0.22, luma * 0.34);
    float scan = 1.0 - ScanlineStrength * (0.5 + 0.5 * sin(texCoord.y * OutSize.y * 0.65));
    vec3 outCol = mix(col, crimson * scan, CrimsonIntensity);

    float vig = smoothstep(0.95, 0.2, dist);
    outCol *= mix(0.45, 1.05, vig);

    fragColor = vec4(clamp(outCol, 0.0, 1.0), 1.0);
}
