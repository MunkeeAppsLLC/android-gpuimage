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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import jp.co.cyberagent.android.gpuimage.R;
import jp.co.cyberagent.android.gpuimage.filter.BaseGPUImageTwoInputFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

import java.nio.FloatBuffer;

public class GPUImage3DLutTableFilter extends BaseGPUImageTwoInputFilter {

    private int intensityLocation;
    private int dimensionLocation;

    private float intensity;
    private float dimension = 0;

    private String bitmapId = "";

    public GPUImage3DLutTableFilter() {
        this(1.0f);
    }

    public GPUImage3DLutTableFilter(final float intensity) {
        super(R.raw.shader_3d_lut_input_2d);
        this.intensity = intensity;
    }

    @Override
    public void onInit() {
        super.onInit();
        intensityLocation = GLES20.glGetUniformLocation(getProgram(), "intensity");
        dimensionLocation = GLES20.glGetUniformLocation(getProgram(), "dimension");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setIntensity(intensity);
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        super.setBitmap(bitmap);
        computeDimension();
        setDimension(dimension);
    }

    public String getBitmapId() {
        return bitmapId;
    }

    public void setBitmapId(String bitmapId) {
        this.bitmapId = bitmapId;
    }

    @Override
    public void reset(boolean recycleBitmap) {
        super.reset(recycleBitmap);
        bitmapId = "";
    }

    @NonNull
    @Override
    public GPUImageFilter copy() {
        GPUImage3DLutTableFilter result = new GPUImage3DLutTableFilter(intensity);
        result.setBitmap(this.getBitmap());
        result.setIntensity(intensity);
        return result;
    }


    public void computeDimension() {
        if (getBitmap() == null || getBitmap().isRecycled()) {
            this.dimension = 0;
        } else {
            this.dimension = Math.min(getBitmap().getWidth(), getBitmap().getHeight());
            if (getBitmap().getWidth() == getBitmap().getHeight()) {
                this.dimension = ((int) Math.cbrt(getBitmap().getWidth() * getBitmap().getHeight()));
            }
        }
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
        setFloat(intensityLocation, this.intensity);
    }

    private void setDimension(float dimension) {
        this.dimension = dimension;
        setFloat(dimensionLocation, this.dimension);
    }
}
