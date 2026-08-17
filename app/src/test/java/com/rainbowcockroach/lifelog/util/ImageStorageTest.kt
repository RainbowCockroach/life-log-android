package com.rainbowcockroach.lifelog.util

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The EXIF orientation → transform mapping. Pure, so it runs without a device.
 *
 * The transform is applied as *rotate, then mirror* (see `ImageStorage.applyTransform`); these
 * expectations only hold in that order, which is what makes TRANSPOSE and TRANSVERSE distinct
 * rather than 180° apart.
 */
class ImageStorageTest {

    private fun transform(orientation: Int) = ImageStorage.transformFor(orientation)

    @Test
    fun `a normal photo is left alone`() {
        assertTrue(transform(ExifInterface.ORIENTATION_NORMAL).isIdentity)
    }

    @Test
    fun `an absent or unrecognised tag is treated as upright`() {
        assertTrue(transform(ExifInterface.ORIENTATION_UNDEFINED).isIdentity)
        assertTrue(transform(0).isIdentity)
        assertTrue(transform(99).isIdentity)
        assertTrue(transform(-1).isIdentity)
    }

    @Test
    fun `the three plain rotations carry no mirror`() {
        assertEquals(ImageStorage.Transform(90, false), transform(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(ImageStorage.Transform(180, false), transform(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(ImageStorage.Transform(270, false), transform(ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test
    fun `the four mirrored orientations mirror`() {
        assertEquals(ImageStorage.Transform(0, true), transform(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(ImageStorage.Transform(180, true), transform(ExifInterface.ORIENTATION_FLIP_VERTICAL))
        assertEquals(ImageStorage.Transform(90, true), transform(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(ImageStorage.Transform(270, true), transform(ExifInterface.ORIENTATION_TRANSVERSE))
    }

    @Test
    fun `transpose and transverse are a half turn apart, not identical`() {
        val transpose = transform(ExifInterface.ORIENTATION_TRANSPOSE)
        val transverse = transform(ExifInterface.ORIENTATION_TRANSVERSE)
        assertTrue(transpose.mirrored && transverse.mirrored)
        assertEquals(180, Math.floorMod(transverse.rotationDegrees - transpose.rotationDegrees, 360))
    }

    @Test
    fun `every orientation the spec defines maps to a quarter turn`() {
        val allTags = listOf(
            ExifInterface.ORIENTATION_NORMAL,
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270,
        )
        allTags.forEach { tag ->
            val t = transform(tag)
            assertTrue("$tag -> $t", t.rotationDegrees in listOf(0, 90, 180, 270))
        }
        // All eight are distinct: the mapping loses no information.
        assertEquals(8, allTags.map { transform(it) }.toSet().size)
    }

    @Test
    fun `isIdentity is false as soon as anything has to change`() {
        assertFalse(transform(ExifInterface.ORIENTATION_ROTATE_90).isIdentity)
        assertFalse(transform(ExifInterface.ORIENTATION_FLIP_HORIZONTAL).isIdentity)
    }

    // --- HEIF handling below API 28 -------------------------------------------------------
    // These take the SDK level as a parameter precisely so the old-device rule can be checked
    // here rather than only on an Android 8 phone nobody has to hand.

    private val oreo = 26   // minSdk, no HEIF decoder
    private val pie = 28    // BitmapFactory learned HEIF here

    @Test
    fun `only pre-28 platforms ask the provider to transcode`() {
        assertTrue(ImageStorage.needsJpegRendition(oreo))
        assertTrue(ImageStorage.needsJpegRendition(27))
        assertFalse(ImageStorage.needsJpegRendition(pie))
        assertFalse(ImageStorage.needsJpegRendition(36))
    }

    @Test
    fun `an undecodable HEIC on an old phone says what to do about it`() {
        val expected = "HEIC images need Android 9 or newer — re-share it as a JPEG"
        assertEquals(expected, ImageStorage.undecodableReason("image/heic", oreo))
        assertEquals(expected, ImageStorage.undecodableReason("image/heif", oreo))
        assertEquals(expected, ImageStorage.undecodableReason("image/heic-sequence", oreo))
        assertEquals(expected, ImageStorage.undecodableReason("IMAGE/HEIC", oreo))
    }

    @Test
    fun `a HEIC failure on a modern phone is not blamed on the platform`() {
        // API 28+ can decode HEIF, so a failure here means something else went wrong and
        // telling the user to upgrade Android would be a lie.
        assertEquals("Couldn't read that image", ImageStorage.undecodableReason("image/heic", pie))
        assertEquals("Couldn't read that image", ImageStorage.undecodableReason("image/heic", 36))
    }

    @Test
    fun `non-HEIF failures stay generic at every api level`() {
        listOf(oreo, pie, 36).forEach { sdk ->
            assertEquals("Couldn't read that image", ImageStorage.undecodableReason("image/jpeg", sdk))
            assertEquals("Couldn't read that image", ImageStorage.undecodableReason("image/png", sdk))
            assertEquals("Couldn't read that image", ImageStorage.undecodableReason(null, sdk))
        }
    }
}
