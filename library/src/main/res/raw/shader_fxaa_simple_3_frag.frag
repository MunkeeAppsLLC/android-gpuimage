/*
		FXAA fragment shader by Timothy Lottes
		http://timothylottes.blogspot.com/
		GLSL version by Geeks3D
		http://www.geeks3d.com/
		modified and adapted to BGE by Martins Upitis
		http://devlog-martinsh.blogspot.com/
		modified by Simone Cingano (to make an outline)
		http://akifox.com
		You need to provide
			vTexCoord: Fragment Coordinates
			uImage0: the Texture
			uImage0Width: the Texture Width
			uImage0Height: the Texture Height
		*/
precision mediump float;

varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;//redered scene texture
uniform vec2 resolution;//texture width and height

float FXAA_SUBPIX_SHIFT = 1.0/4.0;
vec3 FxaaPixelShader(vec4 posPos, sampler2D tex, vec2 rcpFrame)
{
    float width = resolution.x;
    float height = resolution.y;
    vec2 rcpFrame = vec2(1.0/width, 1.0/height);
    vec4 posPos = vec4(textureCoordinate.st, textureCoordinate.st -(rcpFrame * (0.5 + FXAA_SUBPIX_SHIFT)));
    //posPos   // Output of FxaaVertexShader interpolated across screen
    //tex      // Input texture.
    //rcpFrame // Constant {1.0/frameWidth, 1.0/frameHeight}
    /*---------------------------------------------------------*/
    #define FXAA_REDUCE_MIN   (1.0/128.0)
    #define FXAA_REDUCE_MUL   (1.0/8.0)
    #define FXAA_SPAN_MAX     8.0
    /*---------------------------------------------------------*/
    vec3 rgbNW = texture2D(tex, posPos.zw).xyz;
    vec3 rgbNE = texture2D(tex, posPos.zw + vec2(1.0, 0.0)*rcpFrame.xy).xyz;
    vec3 rgbSW = texture2D(tex, posPos.zw + vec2(0.0, 1.0)*rcpFrame.xy).xyz;
    vec3 rgbSE = texture2D(tex, posPos.zw + vec2(1.0, 1.0)*rcpFrame.xy).xyz;
    vec3 rgbM  = texture2D(tex, posPos.xy).xyz;
    /*---------------------------------------------------------*/
    vec3 luma = vec3(0.299, 0.587, 0.114);
    float lumaNW = dot(rgbNW, luma);
    float lumaNE = dot(rgbNE, luma);
    float lumaSW = dot(rgbSW, luma);
    float lumaSE = dot(rgbSE, luma);
    float lumaM  = dot(rgbM, luma);
    /*---------------------------------------------------------*/
    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));
    /*---------------------------------------------------------*/
    vec2 dir;
    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));
    /*---------------------------------------------------------*/
    float dirReduce = max(
    (lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),
    FXAA_REDUCE_MIN);
    float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);
    dir = min(vec2(FXAA_SPAN_MAX, FXAA_SPAN_MAX),
    max(vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX),
    dir * rcpDirMin)) * rcpFrame.xy;
    /*--------------------------------------------------------*/
    vec3 rgbA = (1.0/2.0) * (
    texture2D(tex, posPos.xy + dir * (1.0/3.0 - 0.5)).xyz +
    texture2D(tex, posPos.xy + dir * (2.0/3.0 - 0.5)).xyz);
    vec3 rgbB = rgbA * (1.0/2.0) + (1.0/4.0) * (
    texture2D(tex, posPos.xy + dir * (0.0/3.0 - 0.5)).xyz +
    texture2D(tex, posPos.xy + dir * (3.0/3.0 - 0.5)).xyz);
    float lumaB = dot(rgbB, luma);
    if ((lumaB < lumaMin) || (lumaB > lumaMax)) return rgbA;
    return rgbB;
}
vec4 PostFX(sampler2D tex, vec2 uv, float outline)
{
    vec4 c = texture2D(tex, uv.xy);
    vec2 rcpFrame = vec2(1.0/width, 1.0/height);
    vec3 fxaa = FxaaPixelShader(posPos, tex, rcpFrame);
    float alpha = c.a*outline;
    alpha += texture2D(tex, posPos.xy + vec2(1.0, 1.0)*rcpFrame.xy).a;
    alpha += texture2D(tex, posPos.xy + vec2(-1.0, 1.0)*rcpFrame.xy).a;
    alpha += texture2D(tex, posPos.xy + vec2(-1.0, -1.0)*rcpFrame.xy).a;
    alpha += texture2D(tex, posPos.xy + vec2(1.0, -1.0)*rcpFrame.xy).a;
    // alpha += texture2D(tex, posPos.xy + vec2(2.0,2.0)*rcpFrame.xy).a;
    // alpha += texture2D(tex, posPos.xy + vec2(-2.0,2.0)*rcpFrame.xy).a;
    // alpha += texture2D(tex, posPos.xy + vec2(-2.0,-2.0)*rcpFrame.xy).a;
    // alpha += texture2D(tex, posPos.xy + vec2(2.0,-2.0)*rcpFrame.xy).a;
    // alpha /=(outline+8.0);
    alpha /=(outline+4.0);
    return vec4(fxaa.rgb, alpha);
}
void main()
{
    gl_FragColor = PostFX(uImage0, vTexCoord, uOutline);
}