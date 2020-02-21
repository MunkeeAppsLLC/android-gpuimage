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
    private int isInputImageTexture2LoadedLocation;
    private boolean isInputImageTexture2Loaded = false;

    private Bitmap texture;
    private int textureWidth;
    private int textureHeight;
    private int textureDepth;

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
        isInputImageTexture2LoadedLocation = GLES30.glGetUniformLocation(getProgram(), "isInputImageTexture2Loaded"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader

        GLES30.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute);
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        if (texture != null && !texture.isRecycled()) {
            setTexture(texture, textureWidth, textureHeight, textureDepth);
        }
    }

    /**
     * Assuming the texture is a cube, based on width and height, will compute a the texture dimension
     * @param texture
     */
    public void setTexture(final Bitmap texture) {
        int dimension = computeCubeDimension(texture);
        this.setTexture(texture, dimension, dimension, dimension);
    }

    public void setTexture(final Bitmap texture,
                           final int textureWidth, final int textureHeight, final int textureDepth) {
        if (this.texture != null || texture == null || texture.isRecycled()) {
            return;
        }
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureDepth = textureDepth;
        final int[] pixels = new int[texture.getWidth() * texture.getHeight()];
        runOnDraw(new Runnable() {
            public void run() {
                if (filterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                    if (texture == null || texture.isRecycled()) {
                        return;
                    }
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
                    Log.e("OpenGLUtils", "0 "+GLES30.glGetError());
                    texture.getPixels(pixels, 0, texture.getWidth(), 0, 0, texture.getWidth(), texture.getHeight());
                    IntBuffer texBuffer = (IntBuffer) ByteBuffer.allocateDirect(pixels.length * Integer.SIZE)
                            .order(ByteOrder.nativeOrder())
                            .asIntBuffer()
                            .put(pixels)
                            .position(0);
                    filterSourceTexture2 = OpenGlUtils.load3DTexture(texBuffer, textureWidth, textureHeight, textureDepth, OpenGlUtils.NO_TEXTURE);
                    texBuffer.clear();
                    setInputImageTexture2Loaded(true);
                }
            }
        });
    }

    public Bitmap getTexture() {
        return texture;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public int getTextureDepth() {
        return textureDepth;
    }

    private void setInputImageTexture2Loaded(boolean inputImageTexture2Loaded) {
        isInputImageTexture2Loaded = inputImageTexture2Loaded;
        setInteger(isInputImageTexture2LoadedLocation, isInputImageTexture2Loaded ? 1 : 0);
    }

    public void recycleBitmap() {
        if (texture != null && !texture.isRecycled()) {
            texture.recycle();
            texture = null;
            setInputImageTexture2Loaded(false);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        recycleBitmap();
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

    private int computeCubeDimension(Bitmap texture) {
        if (texture == null || texture.isRecycled()) {
            return 0;
        }
        int dimension = Math.min(texture.getWidth(), texture.getHeight());
        if (texture.getWidth() == texture.getHeight()) {
            dimension = ((int) Math.cbrt(Math.pow(texture.getWidth(), 2)));
        }
        return dimension;
    }
}
