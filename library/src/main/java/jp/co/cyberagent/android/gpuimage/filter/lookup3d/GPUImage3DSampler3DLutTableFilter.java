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

import android.opengl.GLES20;
import jp.co.cyberagent.android.gpuimage.R;
import jp.co.cyberagent.android.gpuimage.filter.BaseGPUImage3DSamplerInputFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import org.jetbrains.annotations.NotNull;

public class GPUImage3DSampler3DLutTableFilter extends BaseGPUImage3DSamplerInputFilter {

    private int intensityLocation;

    private float intensity;

    public GPUImage3DSampler3DLutTableFilter() {
        this(1.0f);
    }

    public GPUImage3DSampler3DLutTableFilter(final float intensity) {
        super(R.raw.shader_3d_lut_input_3d);
        this.intensity = intensity;
    }

    @Override
    public void onInit() {
        super.onInit();
        intensityLocation = GLES20.glGetUniformLocation(getProgram(), "intensity");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setIntensity(intensity);
    }

    @NotNull
    @Override
    public GPUImageFilter copy() {
        GPUImage3DSampler3DLutTableFilter result = new GPUImage3DSampler3DLutTableFilter();
        result.setTexture(getTexture());
        result.setIntensity(intensity);
        return result;
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
        setFloat(intensityLocation, this.intensity);
    }
}
