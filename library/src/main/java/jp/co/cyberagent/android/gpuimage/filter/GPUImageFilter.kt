package jp.co.cyberagent.android.gpuimage.filter

import android.graphics.PointF
import java.nio.FloatBuffer

interface GPUImageFilter {

    fun onInit()
    fun onInitialized()
    fun initIfNeeded()
    fun getProgram(): Int
    fun isInitialized(): Boolean
    fun getOutputWidth(): Int
    fun getOutputHeight(): Int
    fun getAttribPosition(): Int
    fun getAttribTextureCoordinate(): Int
    fun getUniformTexture(): Int
    fun destroy()
    fun onDestroy()
    fun onOutputSizeChanged(width: Int, height: Int)
    fun onDraw(textureId: Int, cubeBuffer: FloatBuffer?,
               textureBuffer: FloatBuffer?)

    fun onDrawArraysPre()
    fun runPendingOnDrawTasks()
    fun setInteger(location: Int, intValue: Int)
    fun setFloat(location: Int, floatValue: Float)
    fun setFloatVec2(location: Int, arrayValue: FloatArray?)
    fun setFloatVec3(location: Int, arrayValue: FloatArray?)
    fun setFloatVec4(location: Int, arrayValue: FloatArray?)
    fun setFloatArray(location: Int, arrayValue: FloatArray?)
    fun setPoint(location: Int, point: PointF?)
    fun setUniformMatrix3f(location: Int, matrix: FloatArray?)
    fun setUniformMatrix4f(location: Int, matrix: FloatArray?)
    fun runOnDraw(runnable: Runnable?)

    companion object {
        const val NO_FILTER_VERTEX_SHADER = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "}"
        const val NO_FILTER_FRAGMENT_SHADER = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                "uniform sampler2D inputImageTexture;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "}"
    }
}