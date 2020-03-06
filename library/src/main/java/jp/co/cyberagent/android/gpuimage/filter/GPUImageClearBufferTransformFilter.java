package jp.co.cyberagent.android.gpuimage.filter;

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
}
