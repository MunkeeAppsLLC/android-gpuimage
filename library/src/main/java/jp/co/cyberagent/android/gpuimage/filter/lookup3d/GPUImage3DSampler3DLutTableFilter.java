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

public class GPUImage3DSampler3DLutTableFilter extends GPUImage3DSamplerInputFilter {

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
                    "    vec4 textureColor= texture2D(inputImageTexture, textureCoordinate);\n" +
                    "    vec4 newColor = texture3D(inputImageTexture2, textureColor.rgb);\n" +
                    "    gl_FragColor = mix(textureColor, newColor, intensity);\n" +
                    "}";

    private int intensityLocation;

    private float intensity;

    public GPUImage3DSampler3DLutTableFilter() {
        this(1.0f);
    }

    public GPUImage3DSampler3DLutTableFilter(final float intensity) {
        super(LOOKUP_FRAGMENT_SHADER);
        this.intensity = intensity;
    }

    @Override
    public void onInit() {
        super.onInit();
        intensityLocation = GLES30.glGetUniformLocation(getProgram(), "intensity");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setIntensity(intensity);
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
        setFloat(intensityLocation, this.intensity);
    }
}
