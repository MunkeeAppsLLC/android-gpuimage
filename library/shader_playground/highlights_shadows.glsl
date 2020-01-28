uniform sampler2D inputImageTexture;
varying highp vec2 textureCoordinate;

uniform lowp float shadows;
uniform lowp float highlights;

const mediump vec3 luminanceWeighting = vec3(0.3, 0.3, 0.3);

void main()
{
    lowp vec4 source = texture2D(inputImageTexture, textureCoordinate);
    mediump float luminance = dot(source.rgb, luminanceWeighting);

    //(shadows+1.0) changed to just shadows:
    mediump float shadow = clamp((pow(luminance, 1.0/shadows) + (-0.76)*pow(luminance, 2.0/shadows)) - luminance, 0.0, 1.0);
    mediump float highlight = clamp((1.0 - (pow(1.0-luminance, 1.0/(2.0-highlights)) + (-0.8)*pow(1.0-luminance, 2.0/(2.0-highlights)))) - luminance, -1.0, 0.0);
    lowp vec3 result = vec3(0.0, 0.0, 0.0) + ((luminance + shadow + highlight) - 0.0) * ((source.rgb - vec3(0.0, 0.0, 0.0))/(luminance - 0.0));

    // blend toward white if highlights is more than 1
    mediump float contrastedLuminance = ((luminance - 0.5) * 1.5) + 0.5;
    mediump float whiteInterp = contrastedLuminance*contrastedLuminance*contrastedLuminance;
    mediump float whiteTarget = clamp(highlights, 1.0, 2.0) - 1.0;
    result = mix(result, vec3(1.0), whiteInterp*whiteTarget);

    // blend toward black if shadows is less than 1
    mediump float invContrastedLuminance = 1.0 - contrastedLuminance;
    mediump float blackInterp = invContrastedLuminance*invContrastedLuminance*invContrastedLuminance;
    mediump float blackTarget = 1.0 - clamp(shadows, 0.0, 1.0);
    result = mix(result, vec3(0.0), blackInterp*blackTarget);

    gl_FragColor = vec4(result, source.a);
}
