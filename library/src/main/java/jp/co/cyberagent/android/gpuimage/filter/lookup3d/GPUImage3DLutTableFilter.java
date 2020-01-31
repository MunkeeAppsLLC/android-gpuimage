/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.filter.lookup3d;

import android.opengl.GLES30;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageTwoInputFilter;

public class GPUImage3DLutTableFilter extends GPUImageTwoInputFilter {

    public static final String LOOKUP_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec2 textureCoordinate2; // TODO: This is not used\n" +
                    "\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform sampler2D inputImageTexture2;// lookup texture\n" +
                    "\n" +
                    "uniform lowp float intensity;\n" +
                    "uniform lowp float dimension;\n" +
                    "\n" +
                    "vec4 sampleAs3DTexture(sampler2D tex, vec3 texCoord, float size) {\n" +
                    "    highp float x = texCoord.z;\n" +
                    "    highp float y = texCoord.y;\n" +
                    "    highp float z = texCoord.x;\n" +
                    "    highp float sliceSize = 1.0 / size;                  // space of 1 slice\n" +
                    "    highp float slicePixelSize = sliceSize / size;       // space of 1 pixel\n" +
                    "    highp float width = size - 1.0;\n" +
                    "    highp float sliceInnerSize = slicePixelSize * width; // space of size pixels\n" +
                    "    highp float zSlice0 = floor(y * width);\n" +
                    "    highp float zSlice1 = min( zSlice0 + 1.0, width);\n" +
                    "    highp float xOffset = slicePixelSize * 0.5 + z * sliceInnerSize;\n" +
                    "    highp float yRange = (x * width + 0.5) / size;\n" +
                    "    highp float s0 = xOffset + (zSlice0 * sliceSize);\n" +
                    "    #if defined(USE_NEAREST)\n" +
                    "        return texture2D(tex, vec2( s0, yRange)).bgra;\n" +
                    "    #else\n" +
                    "        highp float s1 = xOffset + (zSlice1 * sliceSize);\n" +
                    "        highp vec4 slice0Color = texture2D(tex, vec2(s0, yRange));\n" +
                    "        highp vec4 slice1Color = texture2D(tex, vec2(s1, yRange));\n" +
                    "        highp float zOffset = mod(y * width, 1.0);\n" +
                    "        return mix(slice0Color, slice1Color, zOffset).bgra;\n" +
                    "    #endif\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "    highp vec4 newColor = sampleAs3DTexture(inputImageTexture2, textureColor.rgb, dimension);\n" +
                    "    gl_FragColor = mix(textureColor, newColor, intensity);\n" +
                    "}";

    private int intensityLocation;
    private int dimensionLocation;

    private float intensity;
    private float dimension;

    public GPUImage3DLutTableFilter(int dimension) {
        this(1.0f, dimension);
    }

    public GPUImage3DLutTableFilter(final float intensity, final int dimension) {
        super(LOOKUP_FRAGMENT_SHADER);
        this.intensity = intensity;
        this.dimension = dimension;
    }

    @Override
    public void onInit() {
        super.onInit();
        intensityLocation = GLES30.glGetUniformLocation(getProgram(), "intensity");
        dimensionLocation = GLES30.glGetUniformLocation(getProgram(), "dimension");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setIntensity(intensity);
        setDimension((int)dimension);
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
        setFloat(intensityLocation, this.intensity);
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
        setFloat(dimensionLocation, this.dimension);
    }
}
