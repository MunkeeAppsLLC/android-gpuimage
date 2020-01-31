package jp.co.cyberagent.android.gpuimage.filter.lookup3d

import android.graphics.Bitmap
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class LookupTableBitmapAdapter {

    private val BUFFER_READ_SIZE = 2048
    private val BUFFER_SIZE = 4
    private val buffer = ByteArray(BUFFER_SIZE)
    private val readBuffer = ByteArray(BUFFER_READ_SIZE)

    private var readBufferIndex = 0
    private var readBufferLength = 0

    fun toBitmap(inputStream: InputStream?, dimension: Int)
            = toBitmap(inputStream, dimension * dimension, dimension, dimension)

    fun toBitmap(inputStream: InputStream?, width: Int, height: Int,
                 dimension: Int): Bitmap? = inputStream?.let {
        val lut = toIntArray(inputStream, dimension)
        Bitmap.createBitmap(lut, width, height, Bitmap.Config.ARGB_8888)
    }

    fun toIntArray(inputStream: InputStream?, dimension: Int) : IntArray? = inputStream?.let {
        readBufferIndex = 0
        readBufferLength = 0

        val lut = IntArray(dimension * dimension * dimension)

        var red: Float
        var green: Float
        var blue: Float

        for (bIdx in 0 until dimension) {
            for (gIdx in 0 until dimension) {
                for (rIdx in 0 until dimension) {
                    red = readFloat(inputStream, true) // Stay within the RGB limits.
                    green = readFloat(inputStream, true)
                    blue = readFloat(inputStream, true)
                    readFloat(inputStream, false) // remove alpha from the stream
                    lut[bIdx * dimension * dimension + gIdx * dimension + rIdx] = bgr(blue, green, red)
                }
            }
        }
        try {
            inputStream.close()
        } catch (e: IOException) { }
        lut
    }

    private fun argb(alpha: Float, red: Float,  green: Float, blue: Float): Int {
        return (alpha * 255.0f + 0.5f).toInt() shl 24 or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt()
    }

    private fun rgb(red: Float, green: Float, blue: Float): Int {
        return -0x1000000 or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt()
    }

    private fun bgr(blue: Float, green: Float, red: Float ): Int {
        return -0x1000000 or
                ((0xff * red).toInt() and 0xFF) or
                (((0xff * green).toInt() and 0xFF) shl 8) or
                (((0xff * blue).toInt() and 0xFF) shl 16)
    }

    private fun readFloat(inputStream: InputStream, computeValue: Boolean): Float {
        try {
            if (readBufferIndex + 4 > readBufferLength) {
                readBufferIndex = 0
                readBufferLength = inputStream.read(readBuffer, 0, BUFFER_READ_SIZE)
            }
            System.arraycopy(readBuffer, readBufferIndex, buffer, 0, BUFFER_SIZE)
            readBufferIndex += BUFFER_SIZE
        } catch (e: IOException) {
            Log.e("Buffer", "Error", e);
        }
        return if (computeValue) Math.min(ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).float, 1.0f) else 1.0f
    }

}