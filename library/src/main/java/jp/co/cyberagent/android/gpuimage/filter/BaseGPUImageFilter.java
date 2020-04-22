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

import android.graphics.PointF;
import android.opengl.GLES20;
import android.view.View;

import androidx.annotation.RawRes;

import org.jetbrains.annotations.NotNull;

import java.nio.FloatBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jp.co.cyberagent.android.gpuimage.R;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;

import static jp.co.cyberagent.android.gpuimage.util.FilterUtils.checkIsFalse;
import static jp.co.cyberagent.android.gpuimage.util.FilterUtils.loadShader;

public abstract class BaseGPUImageFilter implements GPUImageFilter {

    protected final String vertexShader;
    protected final String fragmentShader;
    private final Queue<Runnable> runOnDraw;
    private int glProgId;
    private int glAttribPosition;
    private int glUniformTexture;
    private int glAttribTextureCoordinate;
    private int outputWidth;
    private int outputHeight;
    private boolean isInitialized;

    public BaseGPUImageFilter() {
        this(R.raw.shader_no_filter_vert, R.raw.shader_no_filter_frag);
    }

    public BaseGPUImageFilter(@RawRes int fragmentShaderResId) {
        this(R.raw.shader_no_filter_vert, fragmentShaderResId);
    }

    public BaseGPUImageFilter(String fragmentShader) {
        this(null, fragmentShader, R.raw.shader_no_filter_vert, View.NO_ID);
    }

    public BaseGPUImageFilter(final String vertexShader, final String fragmentShader) {
        this(vertexShader, fragmentShader, View.NO_ID, View.NO_ID);
    }

    public BaseGPUImageFilter(@RawRes final int vertexShaderResId,
                              @RawRes final int fragmentShaderResId) {
        this(null, null, vertexShaderResId, fragmentShaderResId);
    }

    private BaseGPUImageFilter(final String vertexShader,
                               final String fragmentShader,
                               @RawRes final int vertexShaderResId,
                               @RawRes final int fragmentShaderResId) {
        this.runOnDraw = new ConcurrentLinkedQueue<>();
        if (vertexShader == null) {
            checkIsFalse(vertexShaderResId == View.NO_ID, "vertexShader == null && vertexShaderResId not set");
            this.vertexShader = loadShader(vertexShaderResId);
        } else {
            this.vertexShader = vertexShader;
        }
        if (fragmentShader == null) {
            checkIsFalse(vertexShaderResId == View.NO_ID, "vertexShader == null && vertexShaderResId not set");
            this.fragmentShader = loadShader(fragmentShaderResId);
        } else {
            this.fragmentShader = fragmentShader;
        }
    }

    private final void init() {
        onInit();
        onInitialized();
    }

    @Override
    public void onInit() {
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader);
        glAttribPosition = GLES20.glGetAttribLocation(glProgId, "position");
        glUniformTexture = GLES20.glGetUniformLocation(glProgId, "inputImageTexture");
        glAttribTextureCoordinate = GLES20.glGetAttribLocation(glProgId, "inputTextureCoordinate");
        isInitialized = true;
    }

    @Override
    public void onInitialized() {
    }

    @Override
    public void initIfNeeded() {
        if (!isInitialized) init();
    }

    @Override
    public final void destroy() {
        isInitialized = false;
        GLES20.glDeleteProgram(glProgId);
        onDestroy();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        outputWidth = width;
        outputHeight = height;
    }

    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(glProgId);
        runPendingOnDrawTasks();
        if (!isInitialized) {
            return;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(glUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    public void onDrawArraysPre() {
    }

    @Override
    public void runPendingOnDrawTasks() {
        while (!runOnDraw.isEmpty()) {
            runOnDraw.remove().run();
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public int getOutputWidth() {
        return outputWidth;
    }

    @Override
    public int getOutputHeight() {
        return outputHeight;
    }

    @Override
    public int getProgram() {
        return glProgId;
    }

    @Override
    public int getAttribPosition() {
        return glAttribPosition;
    }

    @Override
    public int getAttribTextureCoordinate() {
        return glAttribTextureCoordinate;
    }

    @Override
    public int getUniformTexture() {
        return glUniformTexture;
    }

    @Override
    public void setInteger(final int location, final int intValue) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniform1i(location, intValue);
        });
    }

    @Override
    public void setFloat(final int location, final float floatValue) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniform1f(location, floatValue);
        });
    }

    @Override
    public void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
        });
    }

    @Override
    public void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
        });
    }

    @Override
    public void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
        });
    }

    @Override
    public void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
        });
    }

    @Override
    public void setPoint(final int location, final PointF point) {
        runOnDraw(() -> {
            initIfNeeded();
            float[] vec2 = new float[2];
            vec2[0] = point.x;
            vec2[1] = point.y;
            GLES20.glUniform2fv(location, 1, vec2, 0);
        });
    }

    @Override
    public void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
        });
    }

    @Override
    public void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(() -> {
            initIfNeeded();
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
        });
    }

    @Override
    public void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

    @NotNull
    @Override
    public GPUImageFilter copy() {
        return new GPUImageIdentityFilter();
    }

}