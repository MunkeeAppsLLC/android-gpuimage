package jp.co.cyberagent.android.gpuimage

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * If it's good enough for Picasso.get(), it's good enough for GPUImage
 */
class GPUImageContextProvider : ContentProvider() {

    companion object {
        var context: Context? = null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(uri: Uri, projection: Array<String>?,
                       selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null

    override fun onCreate(): Boolean {
        Companion.context = context
        return true
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun getType(uri: Uri): String? = null

}