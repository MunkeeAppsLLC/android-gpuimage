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

import jp.co.cyberagent.android.gpuimage.filter.GPUImage3DSamplerInputFilter;

public class GPUImage3DLutTableFilter extends GPUImage3DSamplerInputFilter {

    public static final String LOOKUP_FRAGMENT_SHADER =
            "#extension GL_OES_texture_3D : enable\n" +
                    "\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec3 textureCoordinate2;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "\n" +
                    "uniform lowp float intensity;\n" +
                    "\n" +
                    "uniform sampler3D inputImageTexture2;\n" +
                    "void main() {\n" +
                    "    vec2 texcoord0 = textureCoordinate.xy;\n" +
                    "    vec4 rawColor = texture2D(inputImageTexture, texcoord0);\n" +
                    "    vec4 outColor = texture3D(inputImageTexture2, rawColor.rgb);\n" +
                    "    gl_FragColor = mix(rawColor, outColor, intensity);\n" +
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
