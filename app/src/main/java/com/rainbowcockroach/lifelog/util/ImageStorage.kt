package com.rainbowcockroach.lifelog.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies user-picked images into the app's private storage so they survive even if the user
 * deletes the original from the gallery. Also downscales + JPEG-recompresses to stay well below
 * the 10 MB server limit.
 *
 * Files live in `filesDir/pending_media/`. Caller is responsible for deleting them after a
 * successful sync (see SyncWorker).
 */
class ImageStorage(private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "pending_media").apply { mkdirs() }
    }

    data class Imported(val absolutePath: String, val token: String) {
        val filename: String get() = absolutePath.substringAfterLast('/')
    }

    /**
     * Copy + compress an image picked via the photo picker into private storage.
     * Returns the absolute path and the `pending://<filename>` token to splice into markdown,
     * or null on failure. The token is rewritten to the server-assigned filename at sync time.
     */
    fun importImage(uri: Uri, maxDim: Int = 2048, quality: Int = 85): Imported? {
        val src: Bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        val scaled = downscale(src, maxDim)
        val filename = "${UUID.randomUUID()}.jpg"
        val outFile = File(dir, filename)
        FileOutputStream(outFile).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        if (scaled !== src) scaled.recycle()
        src.recycle()
        return Imported(outFile.absolutePath, "pending://$filename")
    }

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }

    private fun downscale(bm: Bitmap, maxDim: Int): Bitmap {
        val w = bm.width
        val h = bm.height
        val longest = maxOf(w, h)
        if (longest <= maxDim) return bm
        val scale = maxDim.toFloat() / longest
        val nw = (w * scale).toInt()
        val nh = (h * scale).toInt()
        return Bitmap.createScaledBitmap(bm, nw, nh, true)
    }
}
