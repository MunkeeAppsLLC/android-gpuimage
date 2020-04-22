package jp.co.cyberagent.android.gpuimage.filter;

import org.jetbrains.annotations.NotNull;

public class GPUImageClearBufferTransformFilter extends GPUImageTransformFilter {

    private boolean enableClearBuffer = true;

    @Override
    protected void clearBufferBit() {
        if (enableClearBuffer) {
            super.clearBufferBit();
        }
    }

    public boolean isEnableClearBuffer() {
        return enableClearBuffer;
    }

    public void setEnableClearBuffer(boolean enableClearBuffer) {
        this.enableClearBuffer = enableClearBuffer;
    }

    public void toggleClearBuffer() {
        this.enableClearBuffer = !enableClearBuffer;
    }

    @NotNull
    @Override
    public GPUImageFilter copy() {
        GPUImageClearBufferTransformFilter result = new GPUImageClearBufferTransformFilter();
        result.setAnchorTopLeft(anchorTopLeft());
        result.setIgnoreAspectRatio(ignoreAspectRatio());
        result.setTransform3D(getTransform3D());
        result.setEnableClearBuffer(isEnableClearBuffer());
        return result;
    }
}
