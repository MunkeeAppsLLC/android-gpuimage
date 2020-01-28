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

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;

public class GPUImage3DSamplerInputFilter extends GPUImageFilter {
    private static final String VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec3 textureCoordinate2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xyz;\n" +
            "}";

    private int filterSecondTextureCoordinateAttribute;
    private int filterInputTextureUniform2;
    private int filterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    private int[] texture;
    private int dimension;

    public GPUImage3DSamplerInputFilter(String fragmentShader) {
        this(VERTEX_SHADER, fragmentShader);
    }

    public GPUImage3DSamplerInputFilter(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
    }

    @Override
    public void onInit() {
        super.onInit();

        filterSecondTextureCoordinateAttribute = GLES30.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        filterInputTextureUniform2 = GLES30.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        GLES30.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute);
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        if (texture != null) {
            setTexture(texture, dimension);
        }
    }

    public void setTexture(final int[] texture, final int dimension) {
        this.texture = texture;
        this.dimension = dimension;
        if (this.texture == null || texture.length == 0 || dimension <= 0
                || dimension * dimension * dimension != texture.length) {
            return;
        }
        runOnDraw(new Runnable() {
            public void run() {
                if (filterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                    if (texture == null || texture.length == 0 || dimension <= 0
                            || dimension * dimension * dimension != texture.length) {
                        return;
                    }
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
                    Log.e("OpenGLUtils", "0 "+GLES30.glGetError());
                    IntBuffer texBuffer = (IntBuffer) ByteBuffer.allocateDirect(texture.length * Integer.SIZE)
                            .order(ByteOrder.nativeOrder())
                            .asIntBuffer()
                            .put(texture)
                            .position(0);
                    filterSourceTexture2 = OpenGlUtils.load3DTexture(texBuffer, dimension, dimension, dimension,
                            OpenGlUtils.NO_TEXTURE);
                }
            }
        });
    }

    public int[] getTexture() {
        return texture;
    }

    public void clearTexture() {
        this.texture = null;
        this.dimension = 0;
    }

    public void onDestroy() {
        super.onDestroy();
        clearTexture();
        GLES30.glDeleteTextures(1, new int[]{
                filterSourceTexture2
        }, 0);
        filterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    protected void onDrawArraysPre() {
        GLES30.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, filterSourceTexture2);
        GLES30.glUniform1i(filterInputTextureUniform2, 3);
    }

    public int getDimension() {
        return dimension;
    }
}
