varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2; // TODO: This is not used

uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;// lookup texture

uniform lowp float intensity;
uniform lowp float dimension;

vec4 sampleAs3DTexture(sampler2D tex, vec3 texCoord, float size, float isSquareTexture) {
    highp float x = texCoord.x;
    highp float y = texCoord.z;
    highp float z = texCoord.y;

    highp float sliceSize = 1.0 / size;                  // space of 1 slice
    highp float sliceTexelSize = sliceSize / size;       // space of 1 pixel
    highp float texelsPerSlice = size - 1.0;
    highp float sliceInnerSize = sliceTexelSize * texelsPerSlice; // space of size pixels

    highp float zSlice0 = floor(z * texelsPerSlice);
    highp float zSlice1 = min( zSlice0 + 1.0, texelsPerSlice);

    highp float yRange = (y * texelsPerSlice + 0.5) / size;

    highp float xOffset = sliceTexelSize * 0.5 + x * sliceInnerSize;

    highp float z0 = zSlice0 * sliceSize + xOffset;
    highp float z1 = zSlice1 * sliceSize + xOffset;

    #if defined(USE_NEAREST)
        return texture2D(tex, vec2( z0, yRange)).bgra;
    #else
        highp vec4 slice0Color = texture2D(tex, vec2(z0, yRange));
        highp vec4 slice1Color = texture2D(tex, vec2(z1, yRange));
        highp float zOffset = mod(z * texelsPerSlice, 1.0);
        return mix(slice0Color, slice1Color, zOffset).bgra;
    #endif
}


void main()
{
    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 newColor = sampleAs3DTexture(inputImageTexture2, textureColor.rgb, dimension, 1.0);
    gl_FragColor = mix(textureColor, newColor, intensity);
}