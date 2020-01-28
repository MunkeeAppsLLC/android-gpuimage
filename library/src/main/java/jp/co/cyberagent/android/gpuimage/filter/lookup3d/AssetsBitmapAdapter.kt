package jp.co.cyberagent.android.gpuimage.filter.lookup3d

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream


class AssetsBitmapAdapter {

    fun toBitmap(inputStream: InputStream?): Bitmap {
        return BitmapFactory.decodeStream(inputStream)
    }

}