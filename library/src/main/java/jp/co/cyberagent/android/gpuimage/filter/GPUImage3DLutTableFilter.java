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

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.io.InputStream;

public class GPUImage3DLutTableFilter extends GPUImageTwoInputFilter {

    private static String TAG = GPUImage3DLutTableFilter.class.getSimpleName();

    private static final int BUFFER_READ_SIZE = 2048;
    private static final int BUFFER_SIZE = 4;

    public static final String LOOKUP_FRAGMENT_SHADER =
            "#version 100\n" +
                    "\n" +
                    "precision highp float;\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec2 textureCoordinate2;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform sampler2D inputImageTexture2;\n" +
                    "\n" +
                    "uniform lowp float dimension;\n" +
                    "uniform lowp float lookupTextureDimension;\n" +
                    "uniform lowp float intensity;\n" +
                    "\n" +
                    "float lerp(float a, float b, float f) {\n" +
                    "    return a + f * (b - a);\n" +
                    "}\n" +
                    "\n" +
                    "vec3 lerp(vec3 a,  vec3 b, float f) {\n" +
                    "    vec3 result;\n" +
                    "    result.r = lerp(a.r, b.r, f);\n" +
                    "    result.g = lerp(a.g, b.g, f);\n" +
                    "    result.b = lerp(a.b, b.b, f);\n" +
                    "    return result;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 translate(vec4 color) {\n" +
                    "    vec3 result;\n" +
                    "    result.z = floor(color.b * dimension);\n" +
                    "    result.x = color.r;\n" +
                    "    result.y = color.g;\n" +
                    "    return result;\n" +
                    "}\n" +
                    "\n" +
                    "vec2 lutTexturePosition(vec3 lutPosition) {\n" +
                    "    highp vec2 result;\n" +
                    "    highp float unit = 1.0 / dimension;\n" +
                    "    result.x = (lutPosition.z + lutPosition.x) * unit;\n" +
                    "    result.y = lutPosition.y;\n" +
                    "    return result;\n" +
                    "}\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "\n" +
                    "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "\n" +
                    "    vec3 translatedPosition = translate(textureColor);\n" +
                    "\n" +
                    "    vec2 texturePosition1 = lutTexturePosition(translatedPosition);\n" +
                    "\n" +
                    "    highp vec4 lutColor = texture2D(inputImageTexture2, texturePosition1);\n" +
                    "\n" +
                    "    highp vec3 newColor = lerp(textureColor.rgb, lutColor.rgb, intensity);\n" +
                    "\n" +
                    "    gl_FragColor = mix(textureColor, vec4(newColor, 1.0), intensity);\n" +
                    "}";

    private float intensity;
    private int intensityLocation;
    private int dimension;
    private int dimensionLocation;
    private int lookupTextureDimension;
    private int lookupTextureDimensionLocation;

    private int readBufferIndex = 0;
    private int readBufferLength = 0;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private byte[] readBuffer = new byte[BUFFER_READ_SIZE];

    public GPUImage3DLutTableFilter(InputStream filterInputStream,
                                    InputStreamAdapter inputStreamAdapter,
                                    int dimension) {
        this(filterInputStream, inputStreamAdapter, dimension, 1.0f);
    }
    
    public GPUImage3DLutTableFilter(InputStream filterInputStream,
                                    InputStreamAdapter inputStreamAdapter,
                                    int dimension, final float intensity) {
        super(LOOKUP_FRAGMENT_SHADER);
        this.dimension = dimension;
        this.lookupTextureDimension = getLookupTextureDimensionLocation(dimension);
        this.intensity = intensity;
        this.setBitmap(inputStreamAdapter.toBitmap(filterInputStream));
    }

    private int getLookupTextureDimensionLocation(int dimension) {
        return (int) Math.ceil(Math.sqrt(Math.pow(dimension, 3)));
    }

    @Override
    public void onInit() {
        super.onInit();
        dimensionLocation = GLES20.glGetUniformLocation(getProgram(), "dimension");
        intensityLocation = GLES20.glGetUniformLocation(getProgram(), "intensity");
        lookupTextureDimensionLocation = GLES20.glGetUniformLocation(getProgram(), "lookupTextureDimensionLocation");
    }


    @Override
    public void onInitialized() {
        super.onInitialized();
        setIntensity(intensity);
        setDimension(dimension);
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
        setFloat(intensityLocation, this.intensity);
    }

    private void setDimension(int dimension) {
        this.dimension = dimension;
        this.lookupTextureDimension = getLookupTextureDimensionLocation(dimension);
        this.setFloat(dimensionLocation, dimension);
        this.setFloat(lookupTextureDimensionLocation, lookupTextureDimension);
    }

    interface InputStreamAdapter {

        Bitmap toBitmap(InputStream inputStream);

    }
}
