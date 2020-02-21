#extension GL_OES_texture_3D : enable

precision highp float;

varying highp vec2 textureCoordinate;
varying highp vec3 textureCoordinate2;
uniform sampler2D inputImageTexture;

uniform lowp float intensity;

uniform sampler3D inputImageTexture2;
void main() {
    vec2 texcoord0 = textureCoordinate.xy;
    vec4 rawColor = texture2D(inputImageTexture, texcoord0);
    vec4 outColor = texture3D(inputImageTexture2, rawColor.rgb);
    gl_FragColor = mix(rawColor, outColor, intensity);
}