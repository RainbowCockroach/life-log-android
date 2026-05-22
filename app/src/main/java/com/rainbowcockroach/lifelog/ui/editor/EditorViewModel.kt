package com.rainbowcockroach.lifelog.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainbowcockroach.lifelog.LifeLogApp
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
    val isSaving: Boolean = false,
    val savedFlash: Boolean = false,
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as LifeLogApp).container
    private val repo = container.entryRepository
    private val imageStorage = container.imageStorage

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    val pendingCount: StateFlow<Int> = repo.observeUnsyncedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onContentChange(value: String) {
        _state.update { it.copy(content = value) }
    }

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
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repo.enqueue(content = current.content, mediaLocalPaths = current.mediaPaths)
            SyncScheduler.schedule(getApplication())
            _state.value = EditorUiState(savedFlash = true)
        }
    }

    fun clearFlash() {
        _state.update { it.copy(savedFlash = false) }
    }

    fun forceSyncNow() {
        SyncScheduler.schedule(getApplication(), replace = true)
    }
}
