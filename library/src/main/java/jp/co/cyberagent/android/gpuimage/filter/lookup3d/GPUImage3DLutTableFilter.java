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

import jp.co.cyberagent.android.gpuimage.filter.BaseGPUImageTwoInputFilter;

public class GPUImage3DLutTableFilter extends BaseGPUImageTwoInputFilter {

    public static final String LOOKUP_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec2 textureCoordinate2; // TODO: This is not used\n" +
                    "\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform sampler2D inputImageTexture2;// lookup texture\n" +
                    "uniform int isInputImageTexture2Loaded;\n" +
                    "\n" +
                    "uniform lowp float intensity;\n" +
                    "uniform lowp float dimension;\n" +
                    "\n" +
                    "highp vec4 sampleAs3DTexture(sampler2D tex, highp vec3 texCoord,highp float size) {\n" +
                    "    highp float x = texCoord.x;\n" +
                    "    highp float y = texCoord.z;\n" +
                    "    highp float z = texCoord.y;\n" +
                    "\n" +
                    "    highp float sliceSize = 1.0 / size;                  // space of 1 slice\n" +
                    "    highp float sliceTexelSize = sliceSize / size;       // space of 1 pixel\n" +
                    "    highp float texelsPerSlice = size - 1.0;\n" +
                    "    highp float sliceInnerSize = sliceTexelSize * texelsPerSlice; // space of size pixels\n" +
                    "\n" +
                    "    highp float zSlice0 = floor(z * texelsPerSlice);\n" +
                    "    highp float zSlice1 = min( zSlice0 + 1.0, texelsPerSlice);\n" +
                    "\n" +
                    "    highp float yRange = (y * texelsPerSlice + 0.5) / size;\n" +
                    "\n" +
                    "    highp float xOffset = sliceTexelSize * 0.5 + x * sliceInnerSize;\n" +
                    "\n" +
                    "    highp float z1 = zSlice1 * sliceSize + xOffset;\n" +
                    "\n" +
                    "    #if defined(USE_NEAREST)\n" +
                    "        return texture2D(tex, vec2( z0, yRange)).bgra;\n" +
                    "    #else\n" +
                    "        highp float z0 = zSlice0 * sliceSize + xOffset;\n" +
                    "        highp vec4 slice0Color = texture2D(tex, vec2(z0, yRange));\n" +
                    "        highp vec4 slice1Color = texture2D(tex, vec2(z1, yRange));\n" +
                    "        highp float zOffset = mod(z * texelsPerSlice, 1.0);\n" +
                    "        return mix(slice0Color, slice1Color, zOffset).bgra;\n" +
                    "    #endif\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "    if (isInputImageTexture2Loaded == 0 || textureColor.w == 0.0) {\n" +
                    "        gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);\n" +
                    "    } else {\n" +
                    "        highp vec4 newColor = sampleAs3DTexture(inputImageTexture2, textureColor.rgb, dimension);\n" +
                    "        gl_FragColor = vec4(mix(textureColor, newColor, intensity).rgb, textureColor.w);\n" +
                    "    }\n" +
                    "}";

    private int intensityLocation;
    private int dimensionLocation;

    private float intensity;
    private float dimension = 0;

    private String bitmapId = "";

    public GPUImage3DLutTableFilter() {
        this(1.0f);
    }

    public GPUImage3DLutTableFilter(final float intensity) {
        super(LOOKUP_FRAGMENT_SHADER);
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

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
        setFloat(intensityLocation, this.intensity);
    }

    public float getIntensity() {
        return intensity;
    }

    private void setDimension(float dimension) {
        this.dimension = dimension;
        setFloat(dimensionLocation, this.dimension);
    }
}
