attribute vec4 position;
attribute vec4 inputTextureCoordinate;

uniform float imageWidthFactor;
uniform float imageHeightFactor;
uniform float sharpness;

varying vec2 textureCoordinate;
varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;
varying vec2 topTextureCoordinate;
varying vec2 bottomTextureCoordinate;

varying float centerMultiplier;
varying float edgeMultiplier;


void main()
{
    gl_Position = position;

    mediump vec2 widthStep = vec2(imageWidthFactor, 0.0);
    mediump vec2 heightStep = vec2(0.0, imageHeightFactor);

    textureCoordinate = inputTextureCoordinate.xy;

    rightTextureCoordinate = clamp(inputTextureCoordinate.xy + widthStep, 0.0, 1.0);
    topTextureCoordinate = clamp(inputTextureCoordinate.xy + heightStep, 0.0, 1.0);

    leftTextureCoordinate = clamp(inputTextureCoordinate.xy - widthStep, 0.0, 1.0);
    bottomTextureCoordinate = clamp(inputTextureCoordinate.xy - heightStep, 0.0, 1.0);

    centerMultiplier = 1.0 + 4.0 * sharpness;
    edgeMultiplier = sharpness;
}