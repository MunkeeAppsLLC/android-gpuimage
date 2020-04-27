varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;// lookup texture

uniform lowp float intensity;

void main()
{
    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);

    highp float blueColor = textureColor.b * 63.0;

    highp vec2 quad1;
    quad1.y = floor(floor(blueColor) / 8.0);
    quad1.x = floor(blueColor) - (quad1.y * 8.0);

    highp vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);

    highp vec2 texPos1;
    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);

    highp vec2 texPos2;
    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);

    lowp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);
    lowp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);

    lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
    gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);
}

void main2()
{
    vec2 tiles      = vec2(17.0, 17.0);// original texture vec2( 8.0, 8.0 )
    vec2 colTexSize = vec2(17.0, 289.0);// original texture vec2( 512.0, 512.0 )

    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);

    highp float blueColor = textureColor.b * ((tiles.x*tiles.y)-1.0);

    highp vec2 quad1;
    quad1.y = floor(floor(blueColor) / tiles.y);
    quad1.x = floor(blueColor) - (quad1.y * tiles.x);

    highp vec2 quad2;
    quad2.y = floor(ceil(blueColor) / tiles.y);
    quad2.x = ceil(blueColor) - (quad2.y * tiles.x);

    highp vec2 texPos1;
    texPos1.x = (quad1.x / tiles.x) + 0.5/colTexSize.x + (1.0/(tiles.x - colTexSize.x) * textureColor.r);
    texPos1.y = (quad1.y / tiles.y) + 0.5/colTexSize.y + (1.0/(tiles.y - colTexSize.y) * textureColor.g);

    highp vec2 texPos2;
    texPos2.x = (quad2.x / tiles.x) + 0.5/colTexSize.x + (1.0/(tiles.x - colTexSize.x) * textureColor.r);
    texPos2.y = (quad2.y / tiles.y) + 0.5/colTexSize.y + (1.0/(tiles.y - colTexSize.y) * textureColor.g);

    lowp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);
    lowp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);

    lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
    gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);
}