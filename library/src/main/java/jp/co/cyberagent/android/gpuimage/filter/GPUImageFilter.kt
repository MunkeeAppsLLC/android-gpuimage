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

    fun copy(): GPUImageFilter

}