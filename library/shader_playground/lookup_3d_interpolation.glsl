#extension GL_OES_texture_3D : enable

precision highp float;

varying highp vec2 textureCoordinate;
varying highp vec3 textureCoordinate2;
uniform sampler2D inputImageTexture;

uniform lowp float intensity;
uniform lowp float dimension;

uniform sampler3D inputImageTexture2;
void main() {
    highp float unit = 255.0 / dimension;

    vec2 texcoord0 = textureCoordinate.xy;
    vec4 rawColor = texture2D(inputImageTexture, texcoord0);

    highp float rawRedFloor = floor(rawColor.r * dimension / 255.0) * unit;
    highp float rawGreenFloor = floor(rawColor.g * dimension / 255.0) * unit;
    highp float rawBlueFloor = floor(rawColor.b * dimension / 255.0) * unit;

    highp float rawRedCeil = ceil(rawColor.r * dimension / 255.0) * unit;
    highp float rawGreenCeil = ceil(rawColor.g * dimension / 255.0) * unit;
    highp float rawBlueCeil = ceil(rawColor.b * dimension / 255.0) * unit;

    vec3 rawColorFloor = vec3(rawRedFloor, rawGreenFloor, rawBlueFloor);
    vec3 rawColorCeil = vec3(rawRedCeil, rawGreenCeil, rawBlueCeil);

    vec4 outColorFloor = texture3D(inputImageTexture2, rawColorFloor.rgb);
    vec4 outColorCeil = texture3D(inputImageTexture2, rawColorCeil.rgb);
    vec4 lutColor = texture3D(inputImageTexture2, rawColor.rgb);

    vec4 newColorCeil = mix(lutColor, outColorCeil, intensity);
    vec4 newColorFloor = mix(lutColor, outColorFloor, intensity);

    gl_FragColor = mix(newColorCeil, newColorFloor, intensity);
}