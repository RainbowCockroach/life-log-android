package com.rainbowcockroach.lifelog.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * On-device behaviour of the import pipeline — the half that needs a real decoder, a real
 * ContentResolver and a real EXIF writer, so it can't live in `src/test`.
 *
 * The orientation cases matter because every import is re-encoded as a fresh JPEG with no EXIF:
 * if the rotation isn't baked into the pixels here, portrait phone photos land sideways in the
 * entry and there is no tag left to recover them from.
 */
@RunWith(AndroidJUnit4::class)
class ImageStorageTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val storage = ImageStorage(context)
    private val scratch = File(context.filesDir, "camera_temp").apply { mkdirs() }
    private val written = mutableListOf<String>()

    @Before
    fun clearScratch() {
        scratch.listFiles()?.forEach { it.delete() }
    }

    @After
    fun cleanUp() {
        written.forEach { storage.delete(it) }
        scratch.listFiles()?.forEach { it.delete() }
    }

    /** A landscape image: red left half, blue right half. Asymmetric along its long axis. */
    private fun writeTestJpeg(name: String, exifOrientation: Int): Uri {
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until 80) for (y in 0 until 40) {
                setPixel(x, y, if (x < 40) Color.RED else Color.BLUE)
            }
        }
        val file = File(scratch, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation.toString())
            saveAttributes()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun importAndDecode(uri: Uri): Bitmap {
        val result = storage.importImage(uri)
        assertTrue("expected a successful import, got $result", result is ImageStorage.Result.Ok)
        val path = (result as ImageStorage.Result.Ok).absolutePath
        written += path
        return BitmapFactory.decodeFile(path)
    }

    // JPEG is lossy, so pure red comes back as 0xFFFE0000 and pure blue as 0xFF0000FE. Compare
    // which channel dominates rather than exact values — the question is where the pixels
    // ended up, not how faithfully they survived the encoder.
    private fun assertRed(actual: Int, where: String) = assertTrue(
        "$where should be red, was #${Integer.toHexString(actual)}",
        Color.red(actual) > 200 && Color.blue(actual) < 60,
    )

    private fun assertBlue(actual: Int, where: String) = assertTrue(
        "$where should be blue, was #${Integer.toHexString(actual)}",
        Color.blue(actual) > 200 && Color.red(actual) < 60,
    )

    @Test
    fun anUprightPhotoKeepsItsPixelsWhereTheyWere() {
        val out = importAndDecode(writeTestJpeg("normal.jpg", ExifInterface.ORIENTATION_NORMAL))
        assertEquals("should stay landscape", 80, out.width)
        assertEquals(40, out.height)
        assertRed(out.getPixel(10, 20), "left half")
        assertBlue(out.getPixel(70, 20), "right half")
    }

    @Test
    fun aQuarterTurnTagIsBakedIntoThePixels() {
        val out = importAndDecode(writeTestJpeg("rot90.jpg", ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals("a 90 degree turn should make it portrait", 40, out.width)
        assertEquals(80, out.height)
        // Rotating clockwise sends the stored left edge to the top.
        assertRed(out.getPixel(20, 10), "top after rotation")
        assertBlue(out.getPixel(20, 70), "bottom after rotation")
    }

    @Test
    fun aHalfTurnTagIsBakedIntoThePixels() {
        val out = importAndDecode(writeTestJpeg("rot180.jpg", ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals("a 180 degree turn stays landscape", 80, out.width)
        assertEquals(40, out.height)
        assertBlue(out.getPixel(10, 20), "left half after a half turn")
        assertRed(out.getPixel(70, 20), "right half after a half turn")
    }

    @Test
    fun aMirroredTagIsBakedIntoThePixels() {
        val out = importAndDecode(writeTestJpeg("flip.jpg", ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(80, out.width)
        assertEquals(40, out.height)
        assertBlue(out.getPixel(10, 20), "left half after mirroring")
        assertRed(out.getPixel(70, 20), "right half after mirroring")
    }

    @Test
    fun theOutputCarriesNoOrientationTagOfItsOwn() {
        // Belt and braces: if we ever wrote the tag through *and* rotated, viewers would
        // double-apply it and the photo would come out sideways the other way.
        val result = storage.importImage(writeTestJpeg("tagfree.jpg", ExifInterface.ORIENTATION_ROTATE_90))
        val path = (result as ImageStorage.Result.Ok).absolutePath
        written += path
        val orientation = ExifInterface(path)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        // Either "upright" or "no tag at all" is fine; what must never appear is a rotation.
        assertTrue(
            "output should carry no rotation, had orientation $orientation",
            orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED,
        )
    }

    @Test
    fun theJpegRenditionFallbackCanActuallyFetchBytes() {
        // The pre-API-28 HEIC path leans on this. It only *runs* on old devices, but the
        // mechanism has to work, so exercise it directly here against a provider that can serve
        // the requested type.
        val bitmap = storage.jpegRenditionDecode(writeTestJpeg("rendition.jpg", ExifInterface.ORIENTATION_NORMAL))
        assertNotNull("provider should have served a JPEG rendition", bitmap)
        assertEquals(80, bitmap!!.width)
    }

    @Test
    fun aUriThatIsNotAnImageFailsWithAReasonRatherThanThrowing() {
        val junk = File(scratch, "notanimage.jpg").apply { writeText("this is not a JPEG") }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", junk)
        val result = storage.importImage(uri)
        assertTrue("expected Failed, got $result", result is ImageStorage.Result.Failed)
        assertEquals("Couldn't read that image", (result as ImageStorage.Result.Failed).reason)
    }

    @Test
    fun aUriThatPointsNowhereFailsWithAReasonRatherThanThrowing() {
        val uri = Uri.parse("content://${context.packageName}.fileprovider/camera_temp/missing.jpg")
        val result = storage.importImage(uri)
        assertTrue("expected Failed, got $result", result is ImageStorage.Result.Failed)
    }

    @Test
    fun aBigPhotoIsBoundedByMaxDimAfterRotating() {
        val big = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
        val file = File(scratch, "big.jpg")
        FileOutputStream(file).use { big.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        big.recycle()
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val result = storage.importImage(uri, maxDim = 100) as ImageStorage.Result.Ok
        written += result.absolutePath
        val out = BitmapFactory.decodeFile(result.absolutePath)
        // Rotated to portrait first, then bounded: the long side is the height.
        assertEquals(100, out.height)
        assertEquals(50, out.width)
    }
}
