uniform sampler2D inputImageTexture;
varying highp vec2 textureCoordinate;

uniform lowp vec2 vignetteCenter;
uniform lowp vec3 vignetteColor;
uniform highp float vignetteStart;
uniform highp float vignetteEnd;

void main()
{
    /*
   lowp vec3 rgb = texture2D(inputImageTexture, textureCoordinate).rgb;
   lowp float d = distance(textureCoordinate, vec2(0.5,0.5));
   rgb *= (1.0 - smoothstep(vignetteStart, vignetteEnd, d));
   gl_FragColor = vec4(vec3(rgb),1.0);
    */

    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec3 rgb = textureColor.rgb;
    highp float d = distance(textureCoordinate, vec2(vignetteCenter.x, vignetteCenter.y));
    highp float percent = smoothstep(vignetteStart, vignetteEnd, d);
    gl_FragColor = vec4(mix(rgb.x, vignetteColor.x, percent), mix(rgb.y, vignetteColor.y, percent), mix(rgb.z, vignetteColor.z, percent), textureColor.w);
}