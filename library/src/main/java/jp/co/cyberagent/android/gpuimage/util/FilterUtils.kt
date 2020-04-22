package jp.co.cyberagent.android.gpuimage.util

import android.util.Log
import androidx.annotation.RawRes
import jp.co.cyberagent.android.gpuimage.GPUImageContextProvider
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter


object FilterUtils {

    @JvmStatic
    fun loadShader(@RawRes fileResId: Int): String? =
            synchronized(GPUImageFilter::class.java) {
                GPUImageContextProvider.context?.resources?.openRawResource(fileResId)?.bufferedReader()?.use {
                    it.readText().apply { it.close() }
                } ?: let {
                    Log.e(GPUImageFilter::class.java.name, "Failed to load shader: $fileResId")
                    ""
                }
            }

}