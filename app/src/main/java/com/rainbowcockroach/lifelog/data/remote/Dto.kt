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
data class UploadMediaResponse(
    val success: Boolean = true,
    val filename: String,
    val path: String,
    val url: String? = null,
    val id: String? = null,
)
