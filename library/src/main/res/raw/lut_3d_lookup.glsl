#version 100

precision highp float;
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
const int MATRIX_DIMENSION = 17*17*17;

uniform lowp float dimension;
uniform lowp float intensity;
uniform lowp float lutMatrix[MATRIX_DIMENSION];

int modi(int x, int y) {
    return x - y * (x / y);
}

int and(int a, int b, int bitCount) {
    int result = 0;
    int n = 1;

    for(int i = 0; i < bitCount; i++) {
        if ((modi(a, 2) == 1) && (modi(b, 2) == 1)) {
            result += n;
        }

        a = a / 2;
        b = b / 2;
        n = n * 2;

        if(!(a > 0 && b > 0)) {
            break;
        }
    }
    return result;
}

float lerp(float a, float b, float f) {
    return a + f * (b - a);
}

void main()
{

    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);

    highp vec3 identityColor;
    identityColor.r = textureColor.r * 255.0 / (dimension - 1.0);
    identityColor.g = textureColor.g * 255.0 / (dimension - 1.0);
    identityColor.b = textureColor.b * 255.0 / (dimension - 1.0);

    int lutIndex = int(textureColor.b * dimension * dimension + textureColor.g * dimension + textureColor.r);

    int lut = int(lutMatrix[lutIndex]);

    float rFull = float(and(lut, 0xFF, 32));
    float gFull = float(and(lut, 0xFF00, 64) / 4096); // >> 8
    float bFull = float(and(lut, 0xFF0000, 96) / 65536); // >> 16;

    float r = lerp(identityColor.r, rFull, intensity);
    float g = lerp(identityColor.g, gFull, intensity);
    float b = lerp(identityColor.b, gFull, intensity);

    highp vec3 lutColor = vec3(r, g, b);

    highp vec3 newColor = mix(identityColor, lutColor, fract(bFull));

    gl_FragColor = mix(textureColor, vec4(newColor, textureColor.w), intensity);
}