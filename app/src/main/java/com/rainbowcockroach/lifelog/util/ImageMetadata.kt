package com.rainbowcockroach.lifelog.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Reads "when was this photo taken" out of an image the user shared into the app.
 *
 * Used by the "create entry from image" share target to prefill the entry's date/time.
 * Deliberately narrow: it only reports timestamps that came from the *image*, never the
 * filesystem's mtime — a file copied around the internet last week is not a moment in the
 * user's diary. When nothing trustworthy is found we return null and the editor leaves the
 * date blank.
 */
class ImageMetadata(private val context: Context) {

    /**
     * Capture time of [uri] as epoch millis, or null if the image carries none.
     *
     * Order of trust:
     * 1. EXIF `DateTimeOriginal` (shutter time), then `DateTimeDigitized`, then `DateTime`.
     *    Combined with the matching `OffsetTime*` tag when the camera recorded one; otherwise
     *    read as device-local wall-clock, which is what those tags mean by spec.
     * 2. MediaStore's `DATE_TAKEN`, for images whose EXIF was stripped in transit (Google Photos
     *    re-encodes, messaging apps strip) but which the media scanner already dated.
     */
    fun capturedAtMillis(uri: Uri): Long? = readExifCaptureTime(uri) ?: readMediaStoreDateTaken(uri)

    private fun readExifCaptureTime(uri: Uri): Long? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            CAPTURE_TAGS.firstNotNullOfOrNull { (dateTag, offsetTag) ->
                parseExifDateTime(exif.getAttribute(dateTag), exif.getAttribute(offsetTag))
            }
        }
    }.getOrNull()

    private fun readMediaStoreDateTaken(uri: Uri): Long? = runCatching {
        if (uri.scheme != "content") return@runCatching null
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DATE_TAKEN),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val column = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            if (column < 0 || cursor.isNull(column)) return@use null
            cursor.getLong(column).takeIf { it > 0L }
        }
    }.getOrNull()

    companion object {
        /** Date tag paired with the UTC-offset tag that qualifies it, most authoritative first. */
        private val CAPTURE_TAGS = listOf(
            ExifInterface.TAG_DATETIME_ORIGINAL to ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED to ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
            ExifInterface.TAG_DATETIME to ExifInterface.TAG_OFFSET_TIME,
        )

        /** EXIF spec format for every date tag: `2026:08:17 14:03:52`. */
        private val EXIF_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

        /**
         * Pure parser for an EXIF date tag, split out from the Android bits so it can be unit
         * tested. [raw] is the `yyyy:MM:dd HH:mm:ss` value; [offset] is the optional companion
         * `OffsetTime*` tag (`+07:00`). Returns null for anything malformed, including the
         * all-zero placeholder some cameras write when the clock was never set.
         */
        @JvmStatic
        fun parseExifDateTime(raw: String?, offset: String?): Long? {
            val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val local = try {
                LocalDateTime.parse(text, EXIF_FORMAT)
            } catch (_: DateTimeParseException) {
                return null
            }
            // "0000:00:00 00:00:00" parses as year 0 in a lenient world; treat pre-1900 as unset.
            if (local.year < 1900) return null
            val zone = offset?.trim()?.takeIf { it.isNotEmpty() }?.let {
                try {
                    ZoneOffset.of(it)
                } catch (_: DateTimeException) {
                    null
                }
            } ?: ZoneId.systemDefault()
            return local.atZone(zone).toInstant().toEpochMilli()
        }
    }
}
