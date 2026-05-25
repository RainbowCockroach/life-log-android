package com.rainbowcockroach.lifelog.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.rainbowcockroach.lifelog.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val RELEASES_URL =
    "https://api.github.com/repos/RainbowCockroach/life-log-android/releases/latest"

@Serializable
private data class GhAsset(val name: String, val browser_download_url: String)

@Serializable
private data class GhRelease(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val html_url: String? = null,
    val assets: List<GhAsset> = emptyList(),
)

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val isNewer: Boolean,
    val apkUrl: String?,
    val notes: String?,
    val releaseUrl: String?,
)

class UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp)

    suspend fun check(): UpdateInfo {
        val text = client.get(RELEASES_URL) {
            header("Accept", "application/vnd.github+json")
        }.bodyAsText()
        val release = json.decodeFromString(GhRelease.serializer(), text)
        val latest = release.tag_name.removePrefix("v")
        val current = BuildConfig.VERSION_NAME
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return UpdateInfo(
            latestVersion = latest,
            currentVersion = current,
            isNewer = compareSemver(latest, current) > 0,
            apkUrl = apk?.browser_download_url,
            notes = release.body,
            releaseUrl = release.html_url,
        )
    }

    companion object {
        fun compareSemver(a: String, b: String): Int {
            val pa = parts(a)
            val pb = parts(b)
            val n = maxOf(pa.size, pb.size)
            for (i in 0 until n) {
                val x = pa.getOrElse(i) { 0 }
                val y = pb.getOrElse(i) { 0 }
                if (x != y) return x.compareTo(y)
            }
            return 0
        }

        private fun parts(v: String): List<Int> =
            v.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
    }
}

class ApkInstaller(private val context: Context) {

    fun hasInstallPermission(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun downloadAndInstall(apkUrl: String, versionName: String) {
        val app = context.applicationContext
        val fileName = "lifelog-$versionName.apk"
        val dm = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Life Log $versionName")
            .setDescription("Downloading update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
        val id = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val done = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (done != id) return
                app.unregisterReceiver(this)
                val uri = dm.getUriForDownloadedFile(id) ?: return
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                app.startActivity(install)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(receiver, filter)
        }
    }
}
