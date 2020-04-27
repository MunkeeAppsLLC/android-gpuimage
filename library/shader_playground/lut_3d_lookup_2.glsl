#version 100

precision highp float;
varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2;
uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;

uniform lowp float dimension;
uniform lowp float lookupTextureDimension;
uniform lowp float intensity;

float lerp(float a, float b, float f) {
    return a + f * (b - a);
}

vec3 lerp(vec3 a, vec3 b, float f) {
    vec3 result;
    result.r = lerp(a.r, b.r, f);
    result.g = lerp(a.g, b.g, f);
    result.b = lerp(a.b, b.b, f);
    return result;
}

vec3 translate(vec4 color) {
    vec3 result;
    result.z = floor(color.b * dimension);
    result.x = color.r;
    result.y = color.g;
    return result;
}

vec2 lutTexturePosition(vec3 lutPosition) {
    highp vec2 result;
    highp float unit = 1.0 / dimension;
    result.x = (lutPosition.z + lutPosition.x) * unit;
    result.y = lutPosition.y;
    return result;
}

void main()
{

    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);

    vec3 translatedPosition = translate(textureColor);

    vec2 texturePosition1 = lutTexturePosition(translatedPosition);

    highp vec4 lutColor = texture2D(inputImageTexture2, texturePosition1);

    highp vec3 newColor = lerp(textureColor.rgb, lutColor.rgb, intensity);

    gl_FragColor = mix(textureColor, vec4(newColor, 1.0), intensity);
}