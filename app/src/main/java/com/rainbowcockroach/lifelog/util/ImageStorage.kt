package com.rainbowcockroach.lifelog.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Copies user-picked images into the app's private storage so they survive even if the user
 * deletes the original from the gallery. Also rotates them upright, downscales, and
 * JPEG-recompresses to stay well below the 10 MB server limit.
 *
 * Files live in `filesDir/pending_media/`. Caller is responsible for deleting them after a
 * successful sync (see SyncWorker).
 */
class ImageStorage(private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "pending_media").apply { mkdirs() }
    }

    /** Outcome of an import. [Failed.reason] is written to be shown to the user as-is. */
    sealed interface Result {
        data class Ok(val absolutePath: String, val token: String) : Result
        data class Failed(val reason: String) : Result
    }

    /**
     * Copy + compress an image into private storage.
     * Returns the absolute path and the `pending://<filename>` token to splice into markdown.
     * The token is rewritten to the server-assigned filename at sync time.
     *
     * Never throws. A URI can go bad in ways the caller can't check up front — a share grant
     * that has already lapsed (SecurityException), a file the sender deleted mid-import
     * (IOException), a format this device can't decode — and none of those are worth losing
     * the user's half-written entry over. Each becomes a [Result.Failed] with a reason.
     */
    fun importImage(uri: Uri, maxDim: Int = 2048, quality: Int = 85): Result = try {
        val src = decode(uri)
        if (src == null) {
            Result.Failed(undecodableReason(uri))
        } else {
            // Rotate before downscaling, so maxDim bounds the dimensions the user will actually see.
            val upright = applyTransform(src, transformFor(readExifOrientation(uri)))
            val scaled = downscale(upright, maxDim)
            val filename = "${UUID.randomUUID()}.jpg"
            val outFile = File(dir, filename)
            FileOutputStream(outFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (scaled !== upright) scaled.recycle()
            if (upright !== src) upright.recycle()
            src.recycle()
            Result.Ok(outFile.absolutePath, "pending://$filename")
        }
    } catch (e: SecurityException) {
        Result.Failed("No permission to read that image")
    } catch (e: IOException) {
        Result.Failed("Couldn't read that image")
    } catch (e: OutOfMemoryError) {
        // A huge image on a low-memory device. Recoverable — the entry survives, the photo doesn't.
        Result.Failed("That image is too large to process")
    }

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }

    // --- decoding -------------------------------------------------------------------------

    private fun decode(uri: Uri): Bitmap? {
        directDecode(uri)?.let { return it }
        // BitmapFactory gained HEIF support in API 28. Below that, ask the provider for a JPEG
        // rendition instead — MediaStore-backed URIs can transcode, which covers the common case
        // of an iPhone HEIC sitting in the gallery. Providers that can't just decline.
        if (!needsJpegRendition(Build.VERSION.SDK_INT)) return null
        return jpegRenditionDecode(uri)
    }

    private fun directDecode(uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

    /**
     * Second chance at a format this platform can't decode itself: ask the provider to hand the
     * bytes over as JPEG. Visible for testing so the mechanism can be exercised on any API level,
     * not just the pre-28 devices where [decode] actually reaches for it.
     */
    internal fun jpegRenditionDecode(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openTypedAssetFileDescriptor(uri, "image/jpeg", null)?.use { afd ->
            afd.createInputStream().use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()

    /** Why [decode] came back empty. */
    private fun undecodableReason(uri: Uri): String = undecodableReason(
        mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull(),
        sdkInt = Build.VERSION.SDK_INT,
    )

    // --- orientation ----------------------------------------------------------------------

    /**
     * How a stored image must be transformed to appear upright.
     *
     * We re-encode every import as a fresh JPEG with no EXIF, so an orientation tag on the
     * original would simply be lost and the photo would land sideways in the entry. Baking the
     * rotation into the pixels here is what keeps a portrait phone photo portrait.
     */
    internal data class Transform(val rotationDegrees: Int, val mirrored: Boolean) {
        val isIdentity: Boolean get() = rotationDegrees == 0 && !mirrored
    }

    private fun readExifOrientation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    /**
     * Rotate first, then mirror — the order [transformFor]'s table is written against. Swapping
     * them is not a no-op: mirroring before a quarter turn lands 180° away from mirroring after,
     * which is exactly the difference between EXIF's TRANSPOSE and TRANSVERSE.
     */
    private fun applyTransform(bm: Bitmap, transform: Transform): Bitmap {
        if (transform.isIdentity) return bm
        val matrix = Matrix().apply {
            if (transform.rotationDegrees != 0) postRotate(transform.rotationDegrees.toFloat())
            if (transform.mirrored) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
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

    companion object {
        private val HEIF_MIME_TYPES = setOf("image/heic", "image/heif", "image/heic-sequence")

        /**
         * Whether this platform needs the provider to transcode for it. `BitmapFactory` learned
         * HEIF in API 28; below that a HEIC from an iPhone decodes to nothing.
         *
         * Takes the SDK level rather than reading `Build.VERSION` so the rule is unit testable —
         * otherwise it could only ever be exercised on the old devices it exists for.
         */
        @JvmStatic
        internal fun needsJpegRendition(sdkInt: Int): Boolean = sdkInt < Build.VERSION_CODES.P

        /**
         * The message shown when nothing could be decoded. Worth distinguishing the HEIC case:
         * "your phone is too old for this format" is actionable — re-share as JPEG, or switch the
         * camera to JPEG — whereas a generic failure leaves the user with nowhere to go.
         */
        @JvmStatic
        internal fun undecodableReason(mimeType: String?, sdkInt: Int): String =
            if (mimeType?.lowercase() in HEIF_MIME_TYPES && needsJpegRendition(sdkInt)) {
                "HEIC images need Android 9 or newer — re-share it as a JPEG"
            } else {
                "Couldn't read that image"
            }

        /**
         * Maps an EXIF orientation tag (1–8) to the rotation + mirror that makes the image
         * upright. Pure, so the mapping is unit tested without a device. Unknown or absent
         * values mean "already upright", which is what the spec says tag 1 is.
         */
        @JvmStatic
        internal fun transformFor(exifOrientation: Int): Transform = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Transform(90, mirrored = false)
            ExifInterface.ORIENTATION_ROTATE_180 -> Transform(180, mirrored = false)
            ExifInterface.ORIENTATION_ROTATE_270 -> Transform(270, mirrored = false)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Transform(0, mirrored = true)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Transform(180, mirrored = true)
            ExifInterface.ORIENTATION_TRANSPOSE -> Transform(90, mirrored = true)
            ExifInterface.ORIENTATION_TRANSVERSE -> Transform(270, mirrored = true)
            else -> Transform(0, mirrored = false)
        }
    }
}
