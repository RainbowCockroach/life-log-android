package com.rainbowcockroach.lifelog

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Pulls the image URIs out of an `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent aimed at the
 * share target declared in the manifest. Returns an empty list for every other intent
 * (including the launcher's `ACTION_MAIN`), so callers can hand it any intent unconditionally.
 *
 * The URIs carry a read grant scoped to the receiving activity, so they must be consumed —
 * copied into `filesDir` by `ImageStorage` — while that activity is alive. We do exactly that
 * in `EditorViewModel.addSharedImages`; nothing holds a raw URI past import.
 */
fun Intent.extractSharedImageUris(): List<Uri> = when (action) {
    Intent.ACTION_SEND -> listOfNotNull(parcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
    Intent.ACTION_SEND_MULTIPLE ->
        parcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    else -> emptyList()
}.filter { type?.startsWith("image/") == true || it.isProbablyImage() }

/** Some senders set a vague wildcard mime type; fall back to the URI's own extension. */
private fun Uri.isProbablyImage(): Boolean =
    lastPathSegment?.substringAfterLast('.', "")?.lowercase() in IMAGE_EXTENSIONS

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")

@Suppress("DEPRECATION")
private fun <T> Intent.parcelableExtra(name: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(name, clazz)
    else getParcelableExtra(name) as? T

@Suppress("DEPRECATION")
private fun <T> Intent.parcelableArrayListExtra(name: String, clazz: Class<T>): List<T>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableArrayListExtra(name, clazz)
    else getParcelableArrayListExtra<android.os.Parcelable>(name)?.filterIsInstance(clazz)
