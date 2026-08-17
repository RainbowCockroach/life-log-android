package com.rainbowcockroach.lifelog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Covers the pure EXIF date parsing behind "create entry from image". The Android-facing half
 * of [ImageMetadata] (ExifInterface / MediaStore) needs an instrumented test.
 */
class ImageMetadataTest {

    @Test
    fun `parses an exif timestamp with an explicit offset`() {
        val millis = ImageMetadata.parseExifDateTime("2026:08:17 14:03:52", "+07:00")
        // 14:03:52+07:00 == 07:03:52Z
        assertEquals(java.time.Instant.parse("2026-08-17T07:03:52Z").toEpochMilli(), millis)
    }

    @Test
    fun `falls back to the device zone when the camera recorded no offset`() {
        val expected = LocalDateTime.of(2026, 8, 17, 14, 3, 52)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, ImageMetadata.parseExifDateTime("2026:08:17 14:03:52", null))
    }

    @Test
    fun `ignores a malformed offset but keeps the timestamp`() {
        val expected = LocalDateTime.of(2026, 8, 17, 14, 3, 52)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, ImageMetadata.parseExifDateTime("2026:08:17 14:03:52", "not-an-offset"))
    }

    @Test
    fun `returns null when there is nothing to parse`() {
        assertNull(ImageMetadata.parseExifDateTime(null, null))
        assertNull(ImageMetadata.parseExifDateTime("", null))
        assertNull(ImageMetadata.parseExifDateTime("   ", null))
    }

    @Test
    fun `returns null for the all-zero placeholder some cameras write`() {
        assertNull(ImageMetadata.parseExifDateTime("0000:00:00 00:00:00", null))
    }

    @Test
    fun `returns null for a value that is not in exif shape`() {
        assertNull(ImageMetadata.parseExifDateTime("2026-08-17T14:03:52Z", null))
        assertNull(ImageMetadata.parseExifDateTime("2026:08:17", null))
    }
}
