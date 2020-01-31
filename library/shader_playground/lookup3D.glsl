#extension GL_OES_texture_3D : enable

precision highp float;

varying highp vec2 textureCoordinate;
varying highp vec3 textureCoordinate2;
uniform sampler2D inputImageTexture;

uniform lowp float intensity;

uniform sampler3D inputImageTexture2;
void main() {
    vec4 textureColor= texture2D(inputImageTexture, textureCoordinate);
    vec4 newColor = texture3D(inputImageTexture2, textureColor.rgb);
    gl_FragColor = mix(textureColor, newColor, intensity);
}