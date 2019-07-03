package jp.co.cyberagent.android.gpuimage.filter;

import android.opengl.GLES20;

/**
 * Performs a film Grain noise effect
 */
public class GPUImageGrainNoiseFilter extends GPUImageFilter {
    public static final String GRAIN_NOISE_FRAGMENT_SHADER = "" +
            "/*\n" +
            "Film Grain post-process shader v1.1\n" +
            "Martins Upitis (martinsh) devlog-martinsh.blogspot.com\n" +
            "2013\n" +
            "\n" +
            "--------------------------\n" +
            "This work is licensed under a Creative Commons Attribution 3.0 Unported License.\n" +
            "So you are free to share, modify and adapt it for your needs, and even use it for commercial use.\n" +
            "I would also love to hear about a project you are using it.\n" +
            "\n" +
            "Have fun,\n" +
            "Martins\n" +
            "--------------------------\n" +
            "\n" +
            "Perlin noise shader by toneburst:\n" +
            "http://machinesdontcare.wordpress.com/2009/06/25/3d-perlin-noise-sphere-vertex-shader-sourcecode/\n" +
            "*/\n" +
            "\n" +
            "precision highp float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform float timer;\n" +
            "uniform float grainAmount;\n" +
            "uniform float grainSize; //grain particle size (1.5 - 2.5)\n" +
            "uniform float scale;\n" +
            "\n" +
            "const float permTexUnit = 1.0/256.0;\t\t// Perm texture texel-size\n" +
            "const float permTexUnitHalf = 0.5/256.0;\t// Half perm texture texel-size\n" +
            "\n" +
            "bool colored = false; //colored noise?\n" +
            "float coloramount = 0.0;\n" +
            "float lumamount = 1.0; //\n" +
            "\n" +
            "//a random texture generator, but you can also use a pre-computed perturbation texture\n" +
            "vec4 rnm(in vec2 tc)\n" +
            "{\n" +
            "    float noise =  sin(dot(tc + vec2(timer,timer),vec2(12.9898,78.233))) * 43758.5453;\n" +
            "\n" +
            "    float noiseR =  fract(noise)*2.0-1.0;\n" +
            "    float noiseG =  fract(noise*1.2154)*2.0-1.0;\n" +
            "    float noiseB =  fract(noise*1.3453)*2.0-1.0;\n" +
            "    float noiseA =  fract(noise*1.3647)*2.0-1.0;\n" +
            "\n" +
            "    return vec4(noiseR,noiseG,noiseB,noiseA);\n" +
            "}\n" +
            "\n" +
            "float fade(in float t) {\n" +
            "    return t*t*t*(t*(t*6.0-15.0)+10.0);\n" +
            "}\n" +
            "\n" +
            "float pnoise3D(in vec3 p)\n" +
            "{\n" +
            "    vec3 pi = permTexUnit*floor(p)+permTexUnitHalf; // Integer part, scaled so +1 moves permTexUnit texel\n" +
            "    // and offset 1/2 texel to sample texel centers\n" +
            "    vec3 pf = fract(p);     // Fractional part for interpolation\n" +
            "\n" +
            "    // Noise contributions from (x=0, y=0), z=0 and z=1\n" +
            "    float perm00 = rnm(pi.xy).a ;\n" +
            "    vec3  grad000 = rnm(vec2(perm00, pi.z)).rgb * 4.0 - 1.0;\n" +
            "    float n000 = dot(grad000, pf);\n" +
            "    vec3  grad001 = rnm(vec2(perm00, pi.z + permTexUnit)).rgb * 4.0 - 1.0;\n" +
            "    float n001 = dot(grad001, pf - vec3(0.0, 0.0, 1.0));\n" +
            "\n" +
            "    // Noise contributions from (x=0, y=1), z=0 and z=1\n" +
            "    float perm01 = rnm(pi.xy + vec2(0.0, permTexUnit)).a ;\n" +
            "    vec3  grad010 = rnm(vec2(perm01, pi.z)).rgb * 4.0 - 1.0;\n" +
            "    float n010 = dot(grad010, pf - vec3(0.0, 1.0, 0.0));\n" +
            "    vec3  grad011 = rnm(vec2(perm01, pi.z + permTexUnit)).rgb * 4.0 - 1.0;\n" +
            "    float n011 = dot(grad011, pf - vec3(0.0, 1.0, 1.0));\n" +
            "\n" +
            "    // Noise contributions from (x=1, y=0), z=0 and z=1\n" +
            "    float perm10 = rnm(pi.xy + vec2(permTexUnit, 0.0)).a ;\n" +
            "    vec3  grad100 = rnm(vec2(perm10, pi.z)).rgb * 4.0 - 1.0;\n" +
            "    float n100 = dot(grad100, pf - vec3(1.0, 0.0, 0.0));\n" +
            "    vec3  grad101 = rnm(vec2(perm10, pi.z + permTexUnit)).rgb * 4.0 - 1.0;\n" +
            "    float n101 = dot(grad101, pf - vec3(1.0, 0.0, 1.0));\n" +
            "\n" +
            "    // Noise contributions from (x=1, y=1), z=0 and z=1\n" +
            "    float perm11 = rnm(pi.xy + vec2(permTexUnit, permTexUnit)).a ;\n" +
            "    vec3  grad110 = rnm(vec2(perm11, pi.z)).rgb * 4.0 - 1.0;\n" +
            "    float n110 = dot(grad110, pf - vec3(1.0, 1.0, 0.0));\n" +
            "    vec3  grad111 = rnm(vec2(perm11, pi.z + permTexUnit)).rgb * 4.0 - 1.0;\n" +
            "    float n111 = dot(grad111, pf - vec3(1.0, 1.0, 1.0));\n" +
            "\n" +
            "    // Blend contributions along x\n" +
            "    vec4 n_x = mix(vec4(n000, n001, n010, n011), vec4(n100, n101, n110, n111), fade(pf.x));\n" +
            "\n" +
            "    // Blend contributions along y\n" +
            "    vec2 n_xy = mix(n_x.xy, n_x.zw, fade(pf.y));\n" +
            "\n" +
            "    // Blend contributions along z\n" +
            "    float n_xyz = mix(n_xy.x, n_xy.y, fade(pf.z));\n" +
            "\n" +
            "    // We're done, return the final noise value.\n" +
            "    return n_xyz;\n" +
            "}\n" +
            "\n" +
            "//2d coordinate orientation thing\n" +
            "vec2 coordRot(in vec2 tc, in float angle, in float scale)\n" +
            "{\n" +
            "    float aspect = scale;\n" +
            "    float rotX = ((tc.x*2.0-1.0)*aspect*cos(angle)) - ((tc.y*2.0-1.0)*sin(angle));\n" +
            "    float rotY = ((tc.y*2.0-1.0)*cos(angle)) + ((tc.x*2.0-1.0)*aspect*sin(angle));\n" +
            "    rotX = ((rotX/aspect)*0.5+0.5);\n" +
            "    rotY = rotY*0.5+0.5;\n" +
            "    return vec2(rotX,rotY);\n" +
            "}\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec3 rotOffset = vec3(1.425,3.892,5.835); //rotation offset values\n" +
            "    vec2 rotCoordsR = coordRot(textureCoordinate, timer + rotOffset.x, scale);\n" +
            "    vec3 noise = vec3(pnoise3D(vec3(rotCoordsR*textureCoordinate*grainSize*scale,0.0)));\n" +
            "\n" +
            "    if (colored)\n" +
            "    {\n" +
            "        vec2 rotCoordsG = coordRot(textureCoordinate, timer + rotOffset.y, scale);\n" +
            "        vec2 rotCoordsB = coordRot(textureCoordinate, timer + rotOffset.z, scale);\n" +
            "        noise.g = mix(noise.r,pnoise3D(vec3(rotCoordsG*textureCoordinate*grainSize*scale,1.0)),coloramount);\n" +
            "        noise.b = mix(noise.r,pnoise3D(vec3(rotCoordsB*textureCoordinate*grainSize*scale,2.0)),coloramount);\n" +
            "    }\n" +
            "\n" +
            "    highp vec3 col = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "\n" +
            "    //noisiness response curve based on scene luminance\n" +
            "    highp vec3 lumcoeff = vec3(0.299,0.587,0.114);\n" +
            "    float luminance = mix(0.0,dot(col, lumcoeff),lumamount);\n" +
            "    float lum = smoothstep(0.2,0.0,luminance);\n" +
            "    lum += luminance;\n" +
            "\n" +
            "\n" +
            "    noise = mix(noise,vec3(0.0),pow(lum,4.0));\n" +
            "    col = col+noise*grainAmount;\n" +
            "\n" +
            "    gl_FragColor =  vec4(col,1.0);\n" +
            "}";

    private float time;
    private int timeLocation;
    private float scale;
    private int scaleLocation;
    private float grainAmount;
    private int grainAmmountLocation;
    private float grainSize;
    private int grainSizeLocation;

    public GPUImageGrainNoiseFilter() {
        this(0, 30f, 0.1f, 1.5f);
    }

    public GPUImageGrainNoiseFilter(float time, float scale, float grainAmont, float grainSize) {
        super(NO_FILTER_VERTEX_SHADER, GRAIN_NOISE_FRAGMENT_SHADER);
        this.time = time;
        this.scale = scale;
        this.grainAmount = grainAmont;
        this.grainSize = grainSize;
    }

    @Override
    public void onInit() {
        super.onInit();
        timeLocation = GLES20.glGetUniformLocation(getProgram(), "timer");
        scaleLocation = GLES20.glGetUniformLocation(getProgram(), "scale");
        grainAmmountLocation = GLES20.glGetUniformLocation(getProgram(), "grainAmount");
        grainSizeLocation = GLES20.glGetUniformLocation(getProgram(), "grainSize");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        this.setTime(time);
        this.setScale(scale);
        this.setGrainAmount(grainAmount);
        this.setGrainSize(grainSize);
    }

    public void setTime(float time) {
        this.time = time;
        this.setFloat(timeLocation, time);
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.setFloat(scaleLocation, scale);
    }

    public void setGrainAmount(float grainAmount) {
        this.grainAmount = grainAmount;
        this.setFloat(grainAmmountLocation, grainAmount);
    }

    public void setGrainSize(float grainSize) {
        this.grainSize = grainSize;
        this.setFloat(grainSizeLocation, grainSize);
    }
}
