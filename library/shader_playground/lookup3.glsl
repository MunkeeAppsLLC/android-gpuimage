varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2; // TODO: This is not used

uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;// lookup texture

uniform lowp float intensity;

void main()
{
    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);

    highp float unit = 1.0 / 17.0;
    lowp float index = floor((textureColor.r * 17.0) / 255.0);

    highp vec2 lutPos = vec2(index * unit + ((textureColor.g * unit)/255.0), 1.0 - (textureColor.b / 255.0));

    lowp vec4 newColor = texture2D(inputImageTexture2, lutPos);

    gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.a), intensity);
}