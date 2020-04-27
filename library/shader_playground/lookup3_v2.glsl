varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2;// TODO: This is not used

uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;// lookup texture

uniform lowp float intensity;
uniform lowp float dimension;

vec4 sampleAs3DTexture(sampler2D tex, vec3 texCoord, float size) {

    float x = texCoord.x;
    float y = texCoord.y;
    float z = texCoord.z;

    float sliceSize = 1.0 / size;// space of 1 slice
    float slicePixelSize = sliceSize / size;// space of 1 pixel
    float width = size - 1.0;
    float sliceInnerSize = slicePixelSize * width;// space of size pixels

    float zSlice0 = floor(z * width);
    float zSlice1 = min(zSlice0 + 1.0, width);

    float xOffset = slicePixelSize * 0.5 + x * sliceInnerSize;
    float yRange = (y * width + 0.5) / size;

    float z0 = xOffset + (zSlice0 * sliceSize);
    float z1 = xOffset + (zSlice1 * sliceSize);

    #if defined(USE_NEAREST)
    return texture2D(tex, vec2(z0, yRange));
    #else
    vec4 slice0Color = texture2D(tex, vec2(z0, yRange));
    vec4 slice1Color = texture2D(tex, vec2(z1, yRange));
    float zOffset = mod(z * width, 1.0);
    return mix(slice0Color, slice1Color, zOffset);
    #endif
}


void main()
{
    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 newColor = sampleAs3DTexture(inputImageTexture2, textureColor.rgb, dimension);
    gl_FragColor = mix(textureColor, newColor, intensity);
}