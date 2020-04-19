package jp.co.cyberagent.android.gpuimage.filter;

import android.opengl.GLES20;

/**
 * Performs a Perlin Noise effect
 */
public class GPUImagePerlinNoiseFilter extends BaseGPUImageFilter {
    public static final String PERLIN_NOISE_FRAGMENT_SHADER = "" +
            "precision highp float;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            " uniform float scale;\n" +
            " uniform float noiseLevel;\n" +
            " uniform float opacity;\n" +
            " \n" +
            " uniform vec4 colorStart;\n" +
            " uniform vec4 colorFinish;\n" +
            " \n" +
            " //\n" +
            " // Description : Array and textureless GLSL 2D/3D/4D simplex\n" +
            " // noise functions.\n" +
            " // Author : Ian McEwan, Ashima Arts.\n" +
            " // Maintainer : ijm\n" +
            " // Lastmod : 20110822 (ijm)\n" +
            " // License : Copyright (C) 2011 Ashima Arts. All rights reserved.\n" +
            " // Distributed under the MIT License. See LICENSE file.\n" +
            " // https://github.com/ashima/webgl-noise\n" +
            " //\n" +
            " \n" +
            " vec4 mod289(vec4 x)\n" +
            "{\n" +
            "    return x - floor(x * (1.0 / 289.0)) * 289.0;\n" +
            "}\n" +
            " \n" +
            " vec4 permute(vec4 x)\n" +
            "{\n" +
            "    return mod289(((x*noiseLevel)+1.0)*x);\n" +
            "}\n" +
            " \n" +
            " vec4 taylorInvSqrt(vec4 r)\n" +
            "{\n" +
            "    return 1.79284291400159 - 0.85373472095314 * r;\n" +
            "}\n" +
            " \n" +
            " vec2 fade(vec2 t) {\n" +
            "     return t*t*t*(t*(t*6.0-15.0)+10.0);\n" +
            " }\n" +
            " \n" +
            " // Classic Perlin noise\n" +
            " float cnoise(vec2 P)\n" +
            "{\n" +
            "    vec4 Pi = floor(P.xyxy) + vec4(0.0, 0.0, 1.0, 1.0);\n" +
            "    vec4 Pf = fract(P.xyxy) - vec4(0.0, 0.0, 1.0, 1.0);\n" +
            "    Pi = mod289(Pi); // To avoid truncation effects in permutation\n" +
            "    vec4 ix = Pi.xzxz;\n" +
            "    vec4 iy = Pi.yyww;\n" +
            "    vec4 fx = Pf.xzxz;\n" +
            "    vec4 fy = Pf.yyww;\n" +
            "    \n" +
            "    vec4 i = permute(permute(ix) + iy);\n" +
            "    \n" +
            "    vec4 gx = fract(i * (1.0 / 41.0)) * 2.0 - 1.0 ;\n" +
            "    vec4 gy = abs(gx) - 0.5 ;\n" +
            "    vec4 tx = floor(gx + 0.5);\n" +
            "    gx = gx - tx;\n" +
            "    \n" +
            "    vec2 g00 = vec2(gx.x,gy.x);\n" +
            "    vec2 g10 = vec2(gx.y,gy.y);\n" +
            "    vec2 g01 = vec2(gx.z,gy.z);\n" +
            "    vec2 g11 = vec2(gx.w,gy.w);\n" +
            "    \n" +
            "    vec4 norm = taylorInvSqrt(vec4(dot(g00, g00), dot(g01, g01), dot(g10, g10), dot(g11, g11)));\n" +
            "    g00 *= norm.x;  \n" +
            "    g01 *= norm.y;  \n" +
            "    g10 *= norm.z;  \n" +
            "    g11 *= norm.w;  \n" +
            "    \n" +
            "    float n00 = dot(g00, vec2(fx.x, fy.x));\n" +
            "    float n10 = dot(g10, vec2(fx.y, fy.y));\n" +
            "    float n01 = dot(g01, vec2(fx.z, fy.z));\n" +
            "    float n11 = dot(g11, vec2(fx.w, fy.w));\n" +
            "    \n" +
            "    vec2 fade_xy = fade(Pf.xy);\n" +
            "    vec2 n_x = mix(vec2(n00, n01), vec2(n10, n11), fade_xy.x);\n" +
            "    float n_xy = mix(n_x.x, n_x.y, fade_xy.y);\n" +
            "    return 2.3 * n_xy;\n" +
            "}\n" +
            " \n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     \n" +
            "     float n1 = (cnoise(textureCoordinate * scale) + 1.0) / 2.0;\n" +
            "     \n" +
            "     vec4 colorDiff = colorFinish - colorStart;\n" +
            "     highp vec4 originalColor = texture2D(inputImageTexture, textureCoordinate).rgba;\n" +
            "     vec4 color = colorStart + colorDiff * n1 * opacity;\n" +
            "     \n" +
            "     gl_FragColor = originalColor + color;\n" +
            "}";

    private float scale;
    private int scaleLocation;
    private float noiseLevel;
    private int noiseLevelLocation;
    private float opacity;
    private int opacityLocation;
    private float[] startColor;
    private int startClolorLocation;
    private float[] endColor;
    private int endColorLocation;

    public GPUImagePerlinNoiseFilter() {
        this(256f);
    }

    public GPUImagePerlinNoiseFilter(float scale) {
        this(scale, 34f, 0.5f,
                new float[]{0.0f, 0.0f, 0.0f, 1.0f}, new float[]{1.0f, 1.0f, 1.0f, 1.0f});
    }

    public GPUImagePerlinNoiseFilter(float scale, float noiseLevel, float opacity,
                                     float[] startColor, float[] endColor) {
        super(NO_FILTER_VERTEX_SHADER, PERLIN_NOISE_FRAGMENT_SHADER);
        this.scale = scale;
        this.noiseLevel = noiseLevel;
        this.opacity = opacity;
        this.startColor = startColor;
        this.endColor = endColor;
    }

    @Override
    public void onInit() {
        super.onInit();
        scaleLocation = GLES20.glGetUniformLocation(getProgram(), "scale");
        noiseLevelLocation = GLES20.glGetUniformLocation(getProgram(), "noiseLevel");
        opacityLocation = GLES20.glGetUniformLocation(getProgram(), "opacity");
        startClolorLocation = GLES20.glGetUniformLocation(getProgram(), "colorStart");
        endColorLocation = GLES20.glGetUniformLocation(getProgram(), "colorFinish");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        this.setScale(scale);
        this.setStartColor(startColor);
        this.setEndColor(endColor);
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
        this.setFloat(opacityLocation, opacity);
    }

    public void setNoiseLevel(float noiseLevel) {
        this.noiseLevel = noiseLevel;
        this.setFloat(noiseLevelLocation, noiseLevel);
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.setFloat(scaleLocation, scale);
    }

    public void setStartColor(float[] startColor) {
        this.startColor = startColor;
        this.setFloatVec4(startClolorLocation, startColor);
    }

    public void setEndColor(float[] endColor) {
        this.endColor = endColor;
        this.setFloatVec4(endColorLocation, endColor);
    }

}
