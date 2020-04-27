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

import android.opengl.GLES20;

import org.jetbrains.annotations.NotNull;

import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.R;

/**
 * Sharpens the picture. <br>
 * <br>
 * sharpness: from -4.0 to 4.0, with 0.0 as the normal level
 */
public class GPUImageSharpenFilter extends BaseGPUImageFilter {

    private int sharpnessLocation;
    private float sharpness;
    private int imageWidthFactorLocation;
    private int imageHeightFactorLocation;

    public GPUImageSharpenFilter() {
        this(0.0f);
    }

    public GPUImageSharpenFilter(final float sharpness) {
        super(R.raw.shader_sharpen_vert, R.raw.shader_sharpen_frag);
        this.sharpness = sharpness;
    }

    @Override
    public void onInit() {
        super.onInit();
        sharpnessLocation = GLES20.glGetUniformLocation(getProgram(), "sharpness");
        imageWidthFactorLocation = GLES20.glGetUniformLocation(getProgram(), "imageWidthFactor");
        imageHeightFactorLocation = GLES20.glGetUniformLocation(getProgram(), "imageHeightFactor");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setSharpness(sharpness);
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        setFloat(imageWidthFactorLocation, 1.0f / width);
        setFloat(imageHeightFactorLocation, 1.0f / height);
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);
    }

    public float getSharpness() {
        return sharpness;
    }

    public void setSharpness(final float sharpness) {
        this.sharpness = sharpness;
        setFloat(sharpnessLocation, this.sharpness);
    }

    @NotNull
    @Override
    public GPUImageFilter copy() {
        return new GPUImageSharpenFilter(sharpness);
    }
}
