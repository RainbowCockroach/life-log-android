package com.rainbowcockroach.lifelog

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Intent parsing for the "create entry from image" share target. Instrumented rather than JVM
 * because `Intent`/`Uri` are Android types with real Parcelable behaviour — and because the
 * `ArrayList<Uri>` shape that `ACTION_SEND_MULTIPLE` senders actually use can't be produced by
 * `adb shell am`, so this is the only place that path gets exercised.
 */
@RunWith(AndroidJUnit4::class)
class SharedImagesTest {

    private val photo: Uri = Uri.parse("content://media/external/images/media/1")
    private val other: Uri = Uri.parse("content://media/external/images/media/2")

    @Test
    fun singleSendYieldsTheImage() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, photo)
        }
        assertEquals(listOf(photo), intent.extractSharedImageUris())
    }

    @Test
    fun multipleSendYieldsEveryImageInOrder() {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(photo, other))
        }
        assertEquals(listOf(photo, other), intent.extractSharedImageUris())
    }

    @Test
    fun aWildcardTypeStillCountsWhenTheUriLooksLikeAnImage() {
        val jpg = Uri.parse("content://com.example.files/docs/holiday.JPG")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, jpg)
        }
        assertEquals(listOf(jpg), intent.extractSharedImageUris())
    }

    @Test
    fun nonImageSharesAreIgnored() {
        val pdf = Uri.parse("content://com.example.files/docs/report.pdf")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdf)
        }
        assertEquals(emptyList<Uri>(), intent.extractSharedImageUris())
    }

    @Test
    fun theLauncherIntentIsNotAShare() {
        assertEquals(emptyList<Uri>(), Intent(Intent.ACTION_MAIN).extractSharedImageUris())
    }

    @Test
    fun aSendWithNoStreamExtraIsEmpty() {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "image/jpeg" }
        assertEquals(emptyList<Uri>(), intent.extractSharedImageUris())
    }
}
