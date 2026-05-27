package com.rainbowcockroach.lifelog.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request/response shapes that mirror the life-log-api contract.
 *
 * Only the fields the Android app currently sends/needs are declared. Extra fields the server
 * returns are ignored thanks to `ignoreUnknownKeys = true` in the JSON config (see ApiClient).
 */

@Serializable
data class CreateEntryRequest(
    /** Client-assigned id = local timestamp at save time. Server uses this as the entry id. */
    val id: Long? = null,
    val content: String,
    val searchHint: String = "",
    val mediaPaths: List<String> = emptyList(),
    /** Optional. Server defaults to now. We send the local creation time so offline entries keep their real time. */
    val createdAt: String? = null,
    val isHighlighted: Boolean = false,
    val tagIds: List<Long> = emptyList(),
    val locationId: Long? = null,
)

@Serializable
data class EntryResponse(
    val id: Long,
    val content: String = "",
    @SerialName("createdAt") val createdAt: String? = null,
    val mediaPaths: List<String> = emptyList(),
)

@Serializable
data class TagDto(
    val id: Long,
    val name: String = "",
    val searchHint: String = "",
    val type: String = "tag",
    /** ISO-8601 timestamp from the server; null if the tag has never been used in an entry. */
    val lastUsed: String? = null,
)

@Serializable
data class CreateTagRequest(
    val name: String,
    val searchHint: String = "",
    val type: String = "tag",
    val config: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap()),
)

@Serializable
data class UploadMediaResponse(
    val success: Boolean = true,
    val filename: String,
    val path: String,
    val url: String? = null,
    val id: String? = null,
)
