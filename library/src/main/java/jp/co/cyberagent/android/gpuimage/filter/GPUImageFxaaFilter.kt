package jp.co.cyberagent.android.gpuimage.filter

import android.opengl.GLES20
import jp.co.cyberagent.android.gpuimage.R


class GPUImageFxaaFilter : BaseGPUImageFilter(R.raw.shader_fxaa_simple_2_frag) {

    private var resolutionLocation = 0
    private var enabledLocation = 0

    var resolution = FloatArray(2)
        set(value) {
            field = value
            setFloatVec2(resolutionLocation, value)
        }
    var enabled = 1
        set(value) {
            field = value
            setInteger(enabledLocation, value)
        }

    override fun onInit() {
        super.onInit()
        resolutionLocation = GLES20.glGetUniformLocation(getProgram(), "iResolution")
        enabledLocation = GLES20.glGetUniformLocation(getProgram(), "enabled")
    }

    override fun onInitialized() {
        super.onInitialized()
        setFloatVec2(resolutionLocation, resolution)
        setInteger(enabledLocation, enabled)
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
//        resolution = arrayOf(width.toFloat(), height.toFloat()).toFloatArray()
    }

}
