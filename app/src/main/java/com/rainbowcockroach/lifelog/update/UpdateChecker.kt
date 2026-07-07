package com.rainbowcockroach.lifelog.update

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
private data class GhRelease(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val html_url: String? = null,
)

data class UpdateInfo(
    val latestTag: String,
    val latestBuild: Int?,
    val currentBuild: Int,
    val isNewer: Boolean,
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
        // The CI publishes a rolling "latest" tag/name, so the real build number
        // lives in the release body as "Build number: `12`". Prefer that, then
        // fall back to any integer in the tag or name (e.g. "build-12", "v12").
        val latestBuild = release.body?.let { extractBuildNumberFromBody(it) }
            ?: extractBuildNumber(release.tag_name)
            ?: release.name?.let { extractBuildNumber(it) }
        val currentBuild = BuildConfig.VERSION_CODE
        return UpdateInfo(
            latestTag = release.tag_name,
            latestBuild = latestBuild,
            currentBuild = currentBuild,
            isNewer = latestBuild != null && latestBuild > currentBuild,
            notes = release.body,
            releaseUrl = release.html_url,
        )
    }

    companion object {
        /** First integer in the tag, e.g. "build-3" -> 3, "v12" -> 12, "v1.2.3" -> 1. */
        fun extractBuildNumber(tag: String): Int? =
            Regex("\\d+").find(tag)?.value?.toIntOrNull()

        /**
         * Build number from a release body line like "Build number: `12`".
         * Anchored on the label so it doesn't accidentally match digits in the
         * commit hash or branch name.
         */
        fun extractBuildNumberFromBody(body: String): Int? =
            Regex("(?i)build\\s*number[:\\s]*`?(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()
    }
}
