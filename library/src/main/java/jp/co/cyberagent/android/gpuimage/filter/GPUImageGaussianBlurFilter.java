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

package jp.co.cyberagent.android.gpuimage.filter;

import jp.co.cyberagent.android.gpuimage.R;

/**
 * A more generalized 9x9 Gaussian blur filter
 * blurSize value ranging from 0.0 on up, with a default of 1.0
 */
public class GPUImageGaussianBlurFilter extends GPUImageTwoPassTextureSamplingFilter {

    protected float blurSize;

    public GPUImageGaussianBlurFilter() {
        this(1f);
    }

    public GPUImageGaussianBlurFilter(float blurSize) {
        super(R.raw.shader_blur_gausian_vert, R.raw.shader_blur_gausian_frag,
                R.raw.shader_blur_gausian_vert, R.raw.shader_blur_gausian_frag);
        this.blurSize = blurSize;
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setBlurSize(blurSize);
    }

    @Override
    public float getVerticalTexelOffsetRatio() {
        return blurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return blurSize;
    }

    /**
     * A multiplier for the blur size, ranging from 0.0 on up, with a default of 1.0
     *
     * @param blurSize from 0.0 on up, default 1.0
     */
    public void setBlurSize(float blurSize) {
        this.blurSize = blurSize;
        initTexelOffsets();
    }
}
