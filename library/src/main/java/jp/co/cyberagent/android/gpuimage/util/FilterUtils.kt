package jp.co.cyberagent.android.gpuimage.util

import android.util.Log
import androidx.annotation.RawRes
import androidx.core.util.lruCache
import jp.co.cyberagent.android.gpuimage.GPUImageContextProvider
import jp.co.cyberagent.android.gpuimage.R
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter


object FilterUtils {

    /**
     * A LRUCache that stores a max of 50KB string shader content
     * For instance [R.raw.shader_3d_lut_input_2d] is only 1.9KB
     * In theory, this cache would allow for 25 such shaders to be cached in memory
     * On cache miss, the [create] method is called an the new shader gets loaded
     * into the cache and returned.
     */
    private val lruShaderCache = lruCache<Int, String>(
            maxSize = 50 * 1024,
            sizeOf = { _, value -> value.length },
            create = { key ->
                GPUImageContextProvider.context?.resources?.openRawResource(key)
                        ?.bufferedReader()
                        ?.use { reader -> reader.readText().apply { reader.close() } }
                        ?: run {
                            Log.e(GPUImageFilter::class.java.name, "Failed to load shader: $key")
                            ""
                        }
            }
    )

    @JvmStatic
    fun loadShader(@RawRes fileResId: Int): String? = lruShaderCache[fileResId]

    @JvmStatic
    fun checkIsFalse(value: Boolean, message: String) {
        checkIsTrue(value.not(), message)
    }

    @JvmStatic
    fun checkIsTrue(value: Boolean, message: String) {
        if (value.not()) {
            throw IllegalStateException(message)
        }
    }
}