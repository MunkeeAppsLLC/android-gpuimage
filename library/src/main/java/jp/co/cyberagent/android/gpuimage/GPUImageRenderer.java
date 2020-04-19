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

package jp.co.cyberagent.android.gpuimage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageIdentityFilter;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

public class GPUImageRenderer implements GLSurfaceView.Renderer, GLTextureView.Renderer, PreviewCallback {

    private static final int NO_IMAGE = -1;

    public static final float ZERO[] = {
            0, 0,
            0, 0,
            0, 0,
            0, 0,
    };

    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private GPUImageFilter filter;

    private int glTextureId = NO_IMAGE;
    private SurfaceTexture surfaceTexture = null;
    private final FloatBuffer glCubeBuffer;
    private final FloatBuffer glTextureBuffer;
    private IntBuffer glRgbBuffer;

    private int outputWidth;
    private int outputHeight;
    private int imageWidth;
    private int imageHeight;
    private int addedPadding;

    private final Queue<Runnable> runOnDraw = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> runOnDrawEnd = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> runOnSurfaceChanged = new ConcurrentLinkedQueue<>();
    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;
    private Matrix matrix = new Matrix();

    private float backgroundRed = 0f;
    private float backgroundGreen = 0f;
    private float backgroundBlue = 0f;
    private float backgroundAlpha = 0f;

    public GPUImageRenderer() {
        this(new GPUImageIdentityFilter());
    }

    public GPUImageRenderer(final GPUImageFilter filter) {
        this.filter = filter;
        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(ZERO).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES20.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        filter.initIfNeeded();
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        outputWidth = width;
        outputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUseProgram(filter.getProgram());
        filter.onOutputSizeChanged(width, height);
        adjustImageScaling();
        runAll(runOnSurfaceChanged);
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(runOnDraw);
        filter.onDraw(glTextureId, glCubeBuffer, glTextureBuffer);
        runAll(runOnDrawEnd);
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }
    }

    @Override
    public void onSurfaceDestroyed() {
    }

    @Override
    public void onEglContextDestroyed() {
        filter.destroy();
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public void setBackgroundColor(float red, float green, float blue, float alpha) {
        backgroundRed = red;
        backgroundGreen = green;
        backgroundBlue = blue;
        backgroundAlpha = alpha;
    }

    private void runAll(Queue<Runnable> queue) {
        while (!queue.isEmpty()) {
            queue.poll().run();
        }
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        final Size previewSize = camera.getParameters().getPreviewSize();
        onPreviewFrame(data, previewSize.width, previewSize.height);
    }

    public void onPreviewFrame(final byte[] data, final int width, final int height) {
        if (glRgbBuffer == null) {
            glRgbBuffer = IntBuffer.allocate(width * height);
        }
        if (runOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbBuffer.array());
                    glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                    if (imageWidth != width) {
                        imageWidth = width;
                        imageHeight = height;
                        adjustImageScaling();
                    }
                }
            });
        }
    }

    public void setUpSurfaceTexture(final Camera camera) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                surfaceTexture = new SurfaceTexture(textures[0]);
                try {
                    camera.setPreviewTexture(surfaceTexture);
                    camera.setPreviewCallback(GPUImageRenderer.this);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                final GPUImageFilter oldFilter = GPUImageRenderer.this.filter;
                GPUImageRenderer.this.filter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                GPUImageRenderer.this.filter.initIfNeeded();
                GLES20.glUseProgram(GPUImageRenderer.this.filter.getProgram());
                GPUImageRenderer.this.filter.onOutputSizeChanged(outputWidth, outputHeight);
            }
        });
    }

    public GPUImageFilter getFilter() {
        return filter;
    }

    public void deleteImage() {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glDeleteTextures(1, new int[]{
                        glTextureId
                }, 0);
                glTextureId = NO_IMAGE;
            }
        });
    }

    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, true);
    }

    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        runOnDraw(new Runnable() {

            @Override
            public void run() {
                Bitmap resizedBitmap = null;
                if (bitmap.getWidth() % 2 == 1) {
                    resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1, bitmap.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas can = new Canvas(resizedBitmap);
                    can.drawARGB(0x00, 0x00, 0x00, 0x00);
                    can.drawBitmap(bitmap, 0, 0, null);
                    addedPadding = 1;
                } else {
                    addedPadding = 0;
                }

                Bitmap finalBitmap = resizedBitmap != null ? resizedBitmap : bitmap;
                glTextureId = OpenGlUtils.loadTexture(finalBitmap, glTextureId, recycle);
                if (resizedBitmap != null && !resizedBitmap.isRecycled()) {
                    resizedBitmap.recycle();
                }
                if (recycle && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
                adjustImageScaling();
            }
        });
    }

    public void setScaleType(GPUImage.ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
        adjustImageScaling();
    }

    protected int getFrameWidth() {
        return imageWidth;
    }

    protected int getFrameHeight() {
        return imageHeight;
    }

    private void adjustImageScaling() {

        if (outputWidth > 0 && outputHeight > 0 && imageWidth > 0 && imageHeight > 0) {
            float outputWidth = this.outputWidth;
            float outputHeight = this.outputHeight;
            if (rotation == Rotation.ROTATION_270 || rotation == Rotation.ROTATION_90) {
                outputWidth = this.outputHeight;
                outputHeight = this.outputWidth;
            }

            float ratio1 = outputWidth / imageWidth;
            float ratio2 = outputHeight / imageHeight;
            float ratioMax = Math.max(ratio1, ratio2);
            int imageWidthNew = Math.round(imageWidth * ratioMax);
            int imageHeightNew = Math.round(imageHeight * ratioMax);

            float ratioWidth = imageWidthNew / outputWidth;
            float ratioHeight = imageHeightNew / outputHeight;

            float[] cube = CUBE;
            float[] textureCords = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);

            switch (scaleType) {
                case CENTER_INSIDE: {
                    cube = new float[]{
                            CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                            CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                            CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                            CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
                    };
                }
                break;
                case CENTER_CROP: {
                    float distHorizontal = (1 - 1 / ratioWidth) / 2;
                    float distVertical = (1 - 1 / ratioHeight) / 2;
                    textureCords = new float[]{
                            addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                            addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                            addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                            addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
                    };
                }
                break;
                case MATRIX: {
                    matrix.mapPoints(textureCords);
                    cube = new float[]{
                            CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                            CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                            CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                            CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
                    };
                }
                break;
                case FIT_XY: {
                    matrix.mapPoints(textureCords);
                }
                break;
                case CENTER: {
                    cube = new float[]{
                            CUBE[0] * ratioWidth, CUBE[1] * ratioHeight,
                            CUBE[2] * ratioWidth, CUBE[3] * ratioHeight,
                            CUBE[4] * ratioWidth, CUBE[5] * ratioHeight,
                            CUBE[6] * ratioWidth, CUBE[7] * ratioHeight,
                    };
                }
                break;
            }
            glCubeBuffer.clear();
            glCubeBuffer.put(cube).position(0);
            glTextureBuffer.clear();
            glTextureBuffer.put(textureCords).position(0);
        }

    }


    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    public void setRotationCamera(final Rotation rotation, final boolean flipHorizontal,
                                  final boolean flipVertical) {
        setRotation(rotation, flipVertical, flipHorizontal);
    }

    public void setRotation(final Rotation rotation) {
        this.rotation = rotation;
        adjustImageScaling();
    }

    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        setRotation(rotation);
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean isFlippedHorizontally() {
        return flipHorizontal;
    }

    public boolean isFlippedVertically() {
        return flipVertical;
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

    protected void enqueueOnSurfaceChanged(final Runnable runnable) {
        runOnSurfaceChanged.add(runnable);
        //run them already if onSurfaceChanged has already been called
        if (outputWidth > 0 && outputHeight > 0) {
            runAll(runOnSurfaceChanged);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (runOnDrawEnd) {
            runOnDrawEnd.add(runnable);
        }
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }
}
