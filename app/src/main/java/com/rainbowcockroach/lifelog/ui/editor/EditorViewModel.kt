package com.rainbowcockroach.lifelog.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainbowcockroach.lifelog.LifeLogApp
import com.rainbowcockroach.lifelog.data.local.CachedTag
import com.rainbowcockroach.lifelog.sync.SyncScheduler
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
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as LifeLogApp).container
    private val repo = container.entryRepository
    private val imageStorage = container.imageStorage
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

    suspend fun searchTags(query: String, type: String): List<CachedTag> =
        tagRepository.search(query, type)

    /** Online-only — bubbles the exception up so the picker can show "offline, can't create." */
    suspend fun createTag(name: String, type: String): CachedTag =
        tagRepository.createOnServer(name, type)

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            val img = withContext(Dispatchers.IO) { imageStorage.importImage(uri) } ?: return@launch
            val snippet = "\n![image](${img.token})\n"
            _state.update {
                it.copy(
                    content = it.content + snippet,
                    mediaPaths = it.mediaPaths + img.absolutePath,
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
