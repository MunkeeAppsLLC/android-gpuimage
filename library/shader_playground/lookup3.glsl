varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2; // TODO: This is not used

uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;// lookup texture

uniform lowp float intensity;
uniform lowp float dimension;

vec4 sampleAs3DTexture(sampler2D tex, vec3 texCoord, float size) {
    highp float x = texCoord.z;
    highp float y = texCoord.y;
    highp float z = texCoord.x;
    highp float sliceSize = 1.0 / size;                  // space of 1 slice
    highp float slicePixelSize = sliceSize / size;       // space of 1 pixel
    highp float width = size - 1.0;
    highp float sliceInnerSize = slicePixelSize * width; // space of size pixels
    highp float zSlice0 = floor(y * width);
    highp float zSlice1 = min( zSlice0 + 1.0, width);
    highp float xOffset = slicePixelSize * 0.5 + z * sliceInnerSize;
    highp float yRange = (x * width + 0.5) / size;
    highp float s0 = xOffset + (zSlice0 * sliceSize);
    #if defined(USE_NEAREST)
        return texture2D(tex, vec2( s0, yRange)).bgra;
    #else
        highp float s1 = xOffset + (zSlice1 * sliceSize);
        highp vec4 slice0Color = texture2D(tex, vec2(s0, yRange));
        highp vec4 slice1Color = texture2D(tex, vec2(s1, yRange));
        highp float zOffset = mod(y * width, 1.0);
        return mix(slice0Color, slice1Color, zOffset).bgra;
    #endif
}


void main()
{
    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 newColor = sampleAs3DTexture(inputImageTexture2, textureColor.rgb, dimension);
    gl_FragColor = mix(textureColor, newColor, intensity);
}