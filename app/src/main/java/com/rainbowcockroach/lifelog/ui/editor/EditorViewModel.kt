package com.rainbowcockroach.lifelog.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainbowcockroach.lifelog.LifeLogApp
import com.rainbowcockroach.lifelog.data.local.CachedTag
import com.rainbowcockroach.lifelog.sync.SyncScheduler
import com.rainbowcockroach.lifelog.util.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val content: String = "",
    val mediaPaths: List<String> = emptyList(),
    val location: CachedTag? = null,
    val tags: List<CachedTag> = emptyList(),
    val isSaving: Boolean = false,
    val savedFlash: Boolean = false,
    val errorMessage: String? = null,
    /** User-picked entry timestamp; null means "use now() at save time". */
    val customDateTime: Long? = null,
    /**
     * Bumped every time images arrive from the system share sheet. The screen watches it to
     * reveal the Date section, so the user can see what was (or wasn't) read off the photo.
     */
    val sharedImportCount: Int = 0,
    /** True when [customDateTime] came from the shared photo's own metadata, not the user. */
    val dateFromImage: Boolean = false,
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as LifeLogApp).container
    private val repo = container.entryRepository
    private val imageStorage = container.imageStorage
    private val imageMetadata = container.imageMetadata
    private val tagRepository = container.tagRepository
    private val settings = container.settings

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    val pendingCount: StateFlow<Int> = repo.observeUnsyncedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Prefill location from the last entry the user saved on this device.
        viewModelScope.launch {
            val lastId = settings.currentLastUsedLocationId() ?: return@launch
            val cached = tagRepository.findById(lastId) ?: return@launch
            _state.update { if (it.location == null) it.copy(location = cached) else it }
        }
    }

    fun onContentChange(value: String) {
        _state.update { it.copy(content = value, errorMessage = null) }
    }

    fun setLocation(tag: CachedTag?) {
        _state.update { it.copy(location = tag, errorMessage = null) }
    }

    fun toggleTag(tag: CachedTag) {
        _state.update {
            val exists = it.tags.any { t -> t.id == tag.id }
            it.copy(tags = if (exists) it.tags.filterNot { t -> t.id == tag.id } else it.tags + tag)
        }
    }

    fun removeTag(tag: CachedTag) {
        _state.update { it.copy(tags = it.tags.filterNot { t -> t.id == tag.id }) }
    }

    /** Override the entry's timestamp; pass null to fall back to now() at save time. */
    fun setDateTime(epochMs: Long?) {
        _state.update { it.copy(customDateTime = epochMs, dateFromImage = false) }
    }

    suspend fun searchTags(query: String, type: String): List<CachedTag> =
        tagRepository.search(query, type)

    /** Online-only — bubbles the exception up so the picker can show "offline, can't create." */
    suspend fun createTag(name: String, type: String): CachedTag =
        tagRepository.createOnServer(name, type)

    /** Surface a problem the screen ran into before the image ever reached the ViewModel. */
    fun reportError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { imageStorage.importImage(uri) }) {
                is ImageStorage.Result.Failed ->
                    _state.update { it.copy(errorMessage = result.reason) }
                is ImageStorage.Result.Ok -> {
                    val snippet = "\n![image](${result.token})\n"
                    _state.update {
                        it.copy(
                            content = it.content + snippet,
                            mediaPaths = it.mediaPaths + result.absolutePath,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * "Create entry from image": the user picked LifeLog in the system share sheet.
     *
     * Each image is imported exactly like a picked one (copied + downscaled into private
     * storage, spliced into the markdown), and the entry's date/time is prefilled from the
     * first image that carries capture metadata. If none of them do, the date is left blank —
     * we never guess from the file's mtime. An existing user-set date always wins; sharing a
     * photo into a half-written entry shouldn't silently move it.
     */
    fun addSharedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                uris.map { uri -> imageMetadata.capturedAtMillis(uri) to imageStorage.importImage(uri) }
            }
            // Date comes from the first image that both imported and carried a capture time.
            val capturedAt = imported.firstNotNullOfOrNull { (millis, result) ->
                if (result is ImageStorage.Result.Ok) millis else null
            }
            val images = imported.mapNotNull { (_, result) -> result as? ImageStorage.Result.Ok }
            // Report the first real reason rather than a count — "HEIC needs Android 9" tells the
            // user what to do next, "couldn't read 1 of 1" doesn't.
            val firstFailure = imported.firstNotNullOfOrNull { (_, result) ->
                (result as? ImageStorage.Result.Failed)?.reason
            }
            if (images.isEmpty()) {
                _state.update {
                    it.copy(
                        errorMessage = firstFailure ?: "Couldn't read that image",
                        sharedImportCount = it.sharedImportCount + 1,
                    )
                }
                return@launch
            }
            _state.update { current ->
                val snippets = images.joinToString("") { "\n![image](${it.token})\n" }
                // A date the user typed in outranks the photo's; a date a previous photo
                // supplied is only replaced by another photo that actually has one.
                val userSetDate = current.customDateTime != null && !current.dateFromImage
                val date = if (userSetDate) current.customDateTime else capturedAt ?: current.customDateTime
                current.copy(
                    content = current.content + snippets,
                    mediaPaths = current.mediaPaths + images.map { it.absolutePath },
                    customDateTime = date,
                    dateFromImage = !userSetDate && date != null,
                    sharedImportCount = current.sharedImportCount + 1,
                    errorMessage = firstFailure,
                )
            }
        }
    }

    fun removeImage(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            imageStorage.delete(path)
        }
        val filename = path.substringAfterLast('/')
        val tokenPattern = Regex("""!\[[^\]]*]\(pending://${Regex.escape(filename)}\)\n?""")
        _state.update {
            it.copy(
                content = it.content.replace(tokenPattern, ""),
                mediaPaths = it.mediaPaths - path,
            )
        }
    }

    fun insertLink(url: String) {
        if (url.isBlank()) return
        val snippet = "\n[🔗]($url)\n"
        _state.update { it.copy(content = it.content + snippet) }
    }

    fun save() {
        val current = _state.value
        if (current.content.isBlank() && current.mediaPaths.isEmpty()) return
        val location = current.location
        if (location == null) {
            _state.update { it.copy(errorMessage = "Location is required") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            repo.enqueue(
                content = current.content,
                mediaLocalPaths = current.mediaPaths,
                locationId = location.id,
                tagIds = current.tags.map { it.id },
                createdAt = current.customDateTime ?: System.currentTimeMillis(),
            )
            settings.setLastUsedLocationId(location.id)
            tagRepository.bumpLastUsed(listOf(location.id) + current.tags.map { it.id })
            SyncScheduler.schedule(getApplication())
            // Keep location as the prefilled value for the next entry.
            _state.value = EditorUiState(savedFlash = true, location = location)
        }
    }

    fun clearFlash() {
        _state.update { it.copy(savedFlash = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun forceSyncNow() {
        SyncScheduler.schedule(getApplication(), replace = true)
        SyncScheduler.scheduleTagSync(getApplication(), replace = true)
    }
}
