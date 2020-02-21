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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.Rotation;

/**
 * The main accessor for GPUImage functionality. This class helps to do common
 * tasks through a simple interface.
 */
public class GPUImage {

    public enum ScaleType {
        CENTER_INSIDE,
        CENTER_CROP,
        MATRIX,
        CENTER,
        FIT_XY
    }

    static final int SURFACE_TYPE_SURFACE_VIEW = 0;
    static final int SURFACE_TYPE_TEXTURE_VIEW = 1;

    private final Context context;
    private final GPUImageRenderer renderer;
    private int surfaceType = SURFACE_TYPE_TEXTURE_VIEW;
    private GLSurfaceView glSurfaceView;
    private GLTextureView glTextureView;
    private GPUImageFilter filter;
    private Bitmap currentBitmap;
    private int displayWidth;
    private int displayHeight;
    private Matrix matrix = new Matrix();
    private ScaleType scaleType = ScaleType.CENTER_CROP;

    private Queue<Runnable> runOnImageLoaded = new ConcurrentLinkedQueue<>();

    /**
     * Instantiates a new GPUImage object.
     *
     * @param context the context
     */
    public GPUImage(final Context context) {
        if (!supportsOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        this.context = context;
        filter = new GPUImageFilter();
        renderer = new GPUImageRenderer(filter);
        this.initDisplaySize(context);
    }

    private void initDisplaySize(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        displayWidth = size.x;
        displayHeight = size.y;
    }

    /**
     * Checks if OpenGL ES 2.0 is supported on the current device.
     *
     * @param context the context
     * @return true, if successful
     */
    private boolean supportsOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    /**
     * Sets the GLSurfaceView which will display the preview.
     *
     * @param view the GLSurfaceView
     */
    public void setGLSurfaceView(final GLSurfaceView view) {
        surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        glSurfaceView = view;
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        glSurfaceView.requestRender();
    }

    /**
     * Sets the GLTextureView which will display the preview.
     *
     * @param view the GLTextureView
     */
    public void setGLTextureView(final GLTextureView view) {
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW;
        glTextureView = view;
        glTextureView.setEGLContextClientVersion(3);
        glTextureView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glTextureView.setOpaque(false);
        glTextureView.setRenderer(renderer);
        glTextureView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glTextureView.requestRender();
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public void setBackgroundColor(float red, float green, float blue, float alpha) {
        renderer.setBackgroundColor(red, green, blue, alpha);
    }

    /**
     * Request the preview to be rendered again.
     */
    public void requestRender() {
        if (surfaceType == SURFACE_TYPE_SURFACE_VIEW) {
            if (glSurfaceView != null) {
                glSurfaceView.requestRender();
            }
        } else if (surfaceType == SURFACE_TYPE_TEXTURE_VIEW) {
            if (glTextureView != null) {
                glTextureView.requestRender();
            }
        }
    }

    /**
     * Deprecated: Please call
     * {@link GPUImage#updatePreviewFrame(byte[], int, int)} frame by frame
     * <p>
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera the camera
     */
    @Deprecated
    public void setUpCamera(final Camera camera) {
        setUpCamera(camera, 0, false, false);
    }

    /**
     * Deprecated: Please call
     * {@link GPUImage#updatePreviewFrame(byte[], int, int)} frame by frame
     * <p>
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera         the camera
     * @param degrees        by how many degrees the image should be rotated
     * @param flipHorizontal if the image should be flipped horizontally
     * @param flipVertical   if the image should be flipped vertically
     */
    @Deprecated
    public void setUpCamera(final Camera camera, final int degrees, final boolean flipHorizontal,
                            final boolean flipVertical) {
        if (surfaceType == SURFACE_TYPE_SURFACE_VIEW) {
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else if (surfaceType == SURFACE_TYPE_TEXTURE_VIEW) {
            glTextureView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
        renderer.setUpSurfaceTexture(camera);
        Rotation rotation = Rotation.NORMAL;
        switch (degrees) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        renderer.setRotationCamera(rotation, flipHorizontal, flipVertical);
    }

    /**
     * Sets the filter which should be applied to the image which was (or will
     * be) set by setImage(...).
     *
     * @param filter the new filter
     */
    public void setFilter(final GPUImageFilter filter) {
        this.filter = filter;
        renderer.setFilter(this.filter);
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public void setImage(final Bitmap bitmap) {
        currentBitmap = bitmap;
        renderer.setImageBitmap(bitmap, false);
        requestRender();
    }

    /**
     * Returns a new scaled bitmap using the current set bitmap.
     * If the bitmap is not set, the returned value is null
     * @param newWidth width to be scaled to
     * @param newHeight height to be scaled to
     * @return the new scaled bitmap
     */
    public Bitmap buildThumbnail(int newWidth, int newHeight) {
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            return Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true);
        } else {
            return null;
        }
    }

    public Bitmap getCurrentBitmap() {
        return currentBitmap;
    }

    /**
     * Update camera preview frame with YUV format data.
     *
     * @param data   Camera preview YUV data for frame.
     * @param width  width of camera preview
     * @param height height of camera preview
     */
    public void updatePreviewFrame(final byte[] data, final int width, final int height) {
        renderer.onPreviewFrame(data, width, height);
    }

    /**
     * This sets the scale type of GPUImage. This has to be run before setting the image.
     * If image is set and scale type changed, image needs to be reset.
     *
     * @param scaleType The new ScaleType
     */
    public void setScaleType(ScaleType scaleType) {
        this.scaleType = scaleType;
        renderer.setScaleType(scaleType);
        renderer.deleteImage();
        currentBitmap = null;
        requestRender();
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
        renderer.setMatrix(matrix);
    }

    /**
     * Sets the rotation of the displayed image.
     *
     * @param rotation new rotation
     */
    public void setRotation(Rotation rotation) {
        renderer.setRotation(rotation);
    }

    /**
     * Sets the rotation of the displayed image with flip options.
     *
     * @param rotation new rotation
     */
    public void setRotation(Rotation rotation, boolean flipHorizontal, boolean flipVertical) {
        renderer.setRotation(rotation, flipHorizontal, flipVertical);
    }

    /**
     * Deletes the current image.
     */
    public void deleteImage() {
        renderer.deleteImage();
        currentBitmap = null;
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied from a Uri.
     *
     * @param uri the uri of the new image
     */
    public void setImage(final Uri uri) {
        renderer.enqueueOnSurfaceChanged(new Runnable() {
            @Override
            public void run() {
                new LoadImageUriTask(GPUImage.this, uri).execute();
            }
        });
    }

    /**
     * Sets the image on which the filter should be applied from a File.
     *
     * @param file the file of the new image
     */
    public void setImage(final File file) {
        renderer.enqueueOnSurfaceChanged(new Runnable() {
            @Override
            public void run() {
                new LoadImageFileTask(GPUImage.this, file).execute();
            }
        });
    }

    private String getPath(final Uri uri) {
        String[] projection = {
                MediaStore.Images.Media.DATA,
        };
        Cursor cursor = context.getContentResolver()
                .query(uri, projection, null, null, null);
        String path = null;
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            int pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            path = cursor.getString(pathIndex);
        }
        cursor.close();
        return path;
    }

    /**
     * Gets the current displayed image with applied filter as a Bitmap.
     *
     * @return the current image with filter applied
     */
    public Bitmap getBitmapWithFilterApplied() {
        return getBitmapWithFilterApplied(currentBitmap);
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap the bitmap on which the current filter should be applied
     * @return the bitmap with filter applied
     */
    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap) {
        return getBitmapWithFilterApplied(bitmap, false);
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap  the bitmap on which the current filter should be applied
     * @param recycle recycle the bitmap or not.
     * @return the bitmap with filter applied
     */
    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap, boolean recycle) {
        if (glSurfaceView != null || glTextureView != null) {
            renderer.deleteImage();
            renderer.runOnDraw(new Runnable() {

                @Override
                public void run() {
                    synchronized (filter) {
                        filter.destroy();
                        filter.notify();
                    }
                }
            });
            synchronized (filter) {
                requestRender();
                try {
                    filter.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        GPUImageRenderer renderer = new GPUImageRenderer(filter);
        renderer.setRotation(Rotation.NORMAL,
                this.renderer.isFlippedHorizontally(), this.renderer.isFlippedVertically());
        renderer.setScaleType(scaleType);
        renderer.setMatrix(matrix);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);
        renderer.setImageBitmap(bitmap, recycle);
        Bitmap result = buffer.getBitmap();
        filter.destroy();
        renderer.deleteImage();
        buffer.destroy();

        this.renderer.setFilter(filter);
        if (currentBitmap != null) {
            this.renderer.setImageBitmap(currentBitmap, false);
        }
        requestRender();

        return result;
    }

    /**
     * Gets the images for multiple filters on a image. This can be used to
     * quickly get thumbnail images for filters. <br>
     * Whenever a new Bitmap is ready, the listener will be called with the
     * bitmap. The order of the calls to the listener will be the same as the
     * filter order.
     *
     * @param bitmap   the bitmap on which the filters will be applied
     * @param filters  the filters which will be applied on the bitmap
     * @param listener the listener on which the results will be notified
     */
    public static void getBitmapForMultipleFilters(final Bitmap bitmap,
                                                   final List<GPUImageFilter> filters, final ResponseListener<Bitmap> listener) {
        if (filters.isEmpty()) {
            return;
        }
        GPUImageRenderer renderer = new GPUImageRenderer(filters.get(0));
        renderer.setImageBitmap(bitmap, false);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);

        for (GPUImageFilter filter : filters) {
            renderer.setFilter(filter);
            listener.response(buffer.getBitmap());
            filter.destroy();
        }
        renderer.deleteImage();
        buffer.destroy();
    }

    public static @Nullable  Bitmap getBitmapForFilter(@NonNull final Bitmap bitmap, @NonNull GPUImageFilter filter) {
        Bitmap result = null;
        GPUImageRenderer renderer = new GPUImageRenderer(filter);
        renderer.setImageBitmap(bitmap, false);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);
        result = buffer.getBitmap();
        renderer.deleteImage();
        buffer.destroy();
        return result;
    }

    /**
     * Save current image with applied filter to Pictures. It will be stored on
     * the default Picture folder on the phone below the given folderName and
     * fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param folderName the folder name
     * @param fileName   the file name
     * @param listener   the listener
     */
    public void saveToPictures(final String folderName, final String fileName,
                               final OnPictureSavedListener listener) {
        saveToPictures(currentBitmap, folderName, fileName, listener);
    }

    public int getImageWidth() {
        return renderer.getImageWidth();
    }

    public int getImageHeight() {
        return renderer.getImageHeight();
    }

    /**
     * Apply and save the given bitmap with applied filter to Pictures. It will
     * be stored on the default Picture folder on the phone below the given
     * folerName and fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param bitmap     the bitmap
     * @param folderName the folder name
     * @param fileName   the file name
     * @param listener   the listener
     */
    public void saveToPictures(final Bitmap bitmap, final String folderName, final String fileName,
                               final OnPictureSavedListener listener) {
        new SaveTask(this, bitmap, folderName, fileName, listener).execute();
    }

    /**
     * Runs the given Runnable on the OpenGL thread.
     *
     * @param runnable The runnable to be run on the OpenGL thread.
     */
    void runOnGLThread(Runnable runnable) {
        renderer.runOnDrawEnd(runnable);
    }

    private GPUImageView.Size getOutputSize(float imageWidth, float imageHeight) {
        int outputWidth = displayWidth;
        int outputHeight = displayHeight;
        if (renderer != null && renderer.getFrameWidth() != 0 && renderer.getFrameHeight() != 0) {
            int viewWidth = renderer.getFrameWidth();
            int viewHeight = renderer.getFrameHeight();
            if (viewWidth > viewHeight) {
                if (imageWidth > imageHeight) {
                    outputWidth = viewWidth;
                    outputHeight = (int)(imageHeight * viewHeight / imageWidth);
                } else {
                    outputWidth = (int)(imageWidth * viewHeight / imageHeight);
                    outputHeight = viewHeight;
                }
            } else {
                if (imageWidth > imageHeight) {
                    outputWidth = (int)(imageWidth * viewHeight / imageHeight);
                    outputHeight = viewHeight;
                } else {
                    outputWidth = viewWidth;
                    outputHeight = (int)(imageHeight * viewWidth / imageWidth);
                }
            }
        } else if (currentBitmap != null) {
            return new GPUImageView.Size(currentBitmap.getWidth(), currentBitmap.getHeight());
        }
        return new GPUImageView.Size(outputWidth, outputHeight);
    }

    public void enqueueOnImageLoaded(final Runnable runnable) {
        runOnImageLoaded.add(runnable);
        if (currentBitmap != null) {
            while(!runOnImageLoaded.isEmpty()) {
                runOnImageLoaded.poll().run();
            }
        }
    }

    public void clearOnImageLoadedQueue(final Runnable runnable) {
        runOnImageLoaded.clear();
    }

    @Deprecated
    private static class SaveTask extends AsyncTask<Void, Void, Void> {

        private GPUImage gpuImage;
        private final Bitmap bitmap;
        private final String folderName;
        private final String fileName;
        private final OnPictureSavedListener listener;
        private final Handler handler;

        public SaveTask(GPUImage gpuImage, final Bitmap bitmap,
                        final String folderName, final String fileName,
                        final OnPictureSavedListener listener) {
            this.gpuImage = gpuImage;
            this.bitmap = bitmap;
            this.folderName = folderName;
            this.fileName = fileName;
            this.listener = listener;
            handler = new Handler();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            Bitmap result = gpuImage.getBitmapWithFilterApplied(bitmap);
            saveImage(folderName, fileName, result);
            return null;
        }

        private void saveImage(final String folderName, final String fileName, final Bitmap image) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(path, folderName + "/" + fileName);
            try {
                file.getParentFile().mkdirs();
                image.compress(CompressFormat.JPEG, 80, new FileOutputStream(file));
                MediaScannerConnection.scanFile(gpuImage.context,
                        new String[]{
                                file.toString()
                        }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(final String path, final Uri uri) {
                                if (listener != null) {
                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            listener.onPictureSaved(uri);
                                        }
                                    });
                                }
                            }
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnPictureSavedListener {
        void onPictureSaved(Uri uri);
    }

    public interface OnImageLoaded {
        void onImageLoaded();
    }

    private static class LoadImageUriTask extends LoadImageTask {

        private final Uri uri;

        public LoadImageUriTask(GPUImage gpuImage, Uri uri) {
            super(gpuImage);
            this.uri = uri;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            try {
                InputStream inputStream;
                if (uri.getScheme().startsWith("http") || uri.getScheme().startsWith("https")) {
                    inputStream = new URL(uri.toString()).openStream();
                } else if (uri.getPath().startsWith("/android_asset/")) {
                    inputStream = gpuImage.context.getAssets().open(uri.getPath().substring((
                            "/android_asset/").length()));
                } else {
                    inputStream = gpuImage.context.getContentResolver().openInputStream(uri);
                }
                return BitmapFactory.decodeStream(inputStream, null, options);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected int getImageOrientation() throws IOException {
            Cursor cursor = gpuImage.context.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

            if (cursor == null || cursor.getCount() != 1) {
                return 0;
            }

            cursor.moveToFirst();
            int orientation = cursor.getInt(0);
            cursor.close();
            return orientation;
        }
    }

    private static class LoadImageFileTask extends LoadImageTask {

        private final File imageFile;

        public LoadImageFileTask(GPUImage gpuImage, File file) {
            super(gpuImage);
            imageFile = file;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        }

        @Override
        protected int getImageOrientation() throws IOException {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 0;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    private static abstract class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {

        protected final GPUImage gpuImage;

        public LoadImageTask(final GPUImage gpuImage) {
            this.gpuImage = gpuImage;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return loadResizedImage();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            gpuImage.deleteImage();
            gpuImage.setImage(bitmap);
            while (!gpuImage.runOnImageLoaded.isEmpty()) {
                gpuImage.runOnImageLoaded.poll().run();
            }
        }

        protected abstract Bitmap decode(BitmapFactory.Options options);

        private Bitmap loadResizedImage() {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            decode(options);

            GPUImageView.Size outputSize
                    = gpuImage.getOutputSize(options.outWidth, options.outHeight);

            int scale = 1;
            boolean widthBigger = options.outWidth / scale > outputSize.width;
            boolean heightBigger = options.outHeight / scale > outputSize.height;
            while (checkSize(widthBigger, heightBigger, gpuImage.scaleType)) {
                widthBigger = options.outWidth / scale > outputSize.width;
                heightBigger = options.outHeight / scale > outputSize.height;
                scale++;
            }

            scale--;
            if (scale < 1) {
                scale = 1;
            }
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inTempStorage = new byte[32 * 1024];
            Bitmap bitmap = decode(options);
            if (bitmap == null) {
                return null;
            }
            bitmap = rotateImage(bitmap);
            bitmap = scaleBitmap(bitmap, outputSize, gpuImage.scaleType);
            return bitmap;
        }

        private Bitmap scaleBitmap(Bitmap bitmap,
                                   GPUImageView.Size outputSize, ScaleType scaleType) {
            // resize to desired dimensions
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] newSize = getScaleSize(width, height, outputSize, scaleType);
            Bitmap workBitmap = Bitmap.createScaledBitmap(bitmap, newSize[0], newSize[1], true);
            if (workBitmap != bitmap) {
                bitmap.recycle();
                bitmap = workBitmap;
                System.gc();
            }

            if (scaleType == ScaleType.CENTER_CROP) {
                // Crop it
                int diffWidth = newSize[0] - outputSize.width;
                int diffHeight = newSize[1] - outputSize.height;
                workBitmap = Bitmap.createBitmap(bitmap, diffWidth / 2, diffHeight / 2,
                        newSize[0] - diffWidth, newSize[1] - diffHeight);
                if (workBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = workBitmap;
                }
            }

            return bitmap;
        }

        /**
         * Retrieve the scaling size for the image dependent on the ScaleType.<br>
         * <br>
         * If CROP: sides are same size or bigger than output's sides<br>
         * Else   : sides are same size or smaller than output's sides
         */
        private int[] getScaleSize(int width, int height,
                                   GPUImageView.Size outputSize, ScaleType scaleType) {
            float newWidth;
            float newHeight;

            float withRatio = (float) width / outputSize.width;
            float heightRatio = (float) height / outputSize.height;

            boolean adjustWidth = scaleType == ScaleType.CENTER_CROP
                    ? withRatio > heightRatio
                    : withRatio < heightRatio;

            if (adjustWidth) {
                newHeight = outputSize.height;
                newWidth = (newHeight / height) * width;
            } else {
                newWidth = outputSize.width;
                newHeight = (newWidth / width) * height;
            }
            return new int[]{Math.round(newWidth), Math.round(newHeight)};
        }

        private boolean checkSize(boolean widthBigger, boolean heightBigger, ScaleType scaleType) {
            if (scaleType == ScaleType.CENTER_CROP) {
                return widthBigger && heightBigger;
            } else {
                return widthBigger || heightBigger;
            }
        }

        private Bitmap rotateImage(final Bitmap bitmap) {
            if (bitmap == null) {
                return null;
            }
            Bitmap rotatedBitmap = bitmap;
            try {
                int orientation = getImageOrientation();
                if (orientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                            bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return rotatedBitmap;
        }

        protected abstract int getImageOrientation() throws IOException;
    }

    public interface ResponseListener<T> {
        void response(T item);
    }
}
