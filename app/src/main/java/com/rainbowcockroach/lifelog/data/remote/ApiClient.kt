package com.rainbowcockroach.lifelog.data.remote

import com.rainbowcockroach.lifelog.data.local.SettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Ktor-based HTTP client for the life-log API.
 *
 * Base URL + API key are read fresh from [SettingsStore] on every call, so changes in the
 * Settings screen take effect immediately without re-creating the client.
 *
 * Endpoint paths mirror what life-log-web uses (no `/api` prefix):
 *   POST {baseUrl}/entries
 *   POST {baseUrl}/media/upload  (multipart, field name "file")
 *
 * If your deployed API actually serves under `/api/...`, set baseUrl to include it
 * (e.g. `https://example.com/api`).
 */
class ApiClient(private val settings: SettingsStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
        expectSuccess = false
    }

    /** Upload a single media file. Returns the server-side path/filename to store on the entry. */
    suspend fun uploadMedia(file: File): UploadMediaResponse {
        val baseUrl = settings.currentBaseUrl()
        val apiKey = settings.currentApiKey()
        val response = http.post("$baseUrl/media/upload") {
            header("x-api-key", apiKey)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = file.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, file.guessMime())
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            }
                        )
                    }
                )
            )
        }
        if (response.status != HttpStatusCode.OK) {
            error("Upload failed: HTTP ${response.status.value} ${response.bodyText()}")
        }
        return response.body()
    }

    /** Fetch the full tag list. Tags include both `type=tag` and `type=location`. */
    suspend fun fetchAllTags(): List<TagDto> {
        val baseUrl = settings.currentBaseUrl()
        val apiKey = settings.currentApiKey()
        val response = http.get("$baseUrl/tags") {
            header("x-api-key", apiKey)
        }
        if (!response.status.isSuccess2xx()) {
            error("Fetch tags failed: HTTP ${response.status.value} ${response.bodyText()}")
        }
        return response.body()
    }

    /** Create a new tag/location on the server. Returns the assigned id. */
    suspend fun createTag(request: CreateTagRequest): TagDto {
        val baseUrl = settings.currentBaseUrl()
        val apiKey = settings.currentApiKey()
        val response = http.post("$baseUrl/tags") {
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess2xx()) {
            error("Create tag failed: HTTP ${response.status.value} ${response.bodyText()}")
        }
        return response.body()
    }

    /** Create a diary entry. Server assigns the id and returns it. */
    suspend fun createEntry(request: CreateEntryRequest): EntryResponse {
        val baseUrl = settings.currentBaseUrl()
        val apiKey = settings.currentApiKey()
        val response = http.post("$baseUrl/entries") {
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess2xx()) {
            error("Create entry failed: HTTP ${response.status.value} ${response.bodyText()}")
        }
        return response.body()
    }
}

private fun HttpStatusCode.isSuccess2xx() = value in 200..299

private suspend fun io.ktor.client.statement.HttpResponse.bodyText(): String =
    try { body<String>() } catch (_: Throwable) { "" }

private fun File.guessMime(): String = when (extension.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "heic" -> "image/heic"
    "heif" -> "image/heif"
    else -> "application/octet-stream"
}
