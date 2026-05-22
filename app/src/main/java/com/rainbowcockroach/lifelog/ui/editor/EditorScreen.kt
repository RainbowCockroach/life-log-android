package com.rainbowcockroach.lifelog.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onOpenSettings: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    var showLinkDialog by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addImage(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Life Log") },
                actions = {
                    if (pendingCount > 0) {
                        BadgedBox(badge = { Badge { Text(pendingCount.toString()) } }) {
                            IconButton(onClick = { viewModel.forceSyncNow() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync now")
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        EditorContent(
            padding = padding,
            content = state.content,
            mediaPaths = state.mediaPaths,
            isSaving = state.isSaving,
            savedFlash = state.savedFlash,
            onContentChange = viewModel::onContentChange,
            onPickImage = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onAddLink = { showLinkDialog = true },
            onRemoveImage = viewModel::removeImage,
            onSave = viewModel::save,
            onClearFlash = viewModel::clearFlash,
        )

        if (showLinkDialog) {
            LinkDialog(
                onDismiss = { showLinkDialog = false },
                onConfirm = { url ->
                    viewModel.insertLink(url)
                    showLinkDialog = false
                }
            )
        }
    }
}

@Composable
private fun EditorContent(
    padding: PaddingValues,
    content: String,
    mediaPaths: List<String>,
    isSaving: Boolean,
    savedFlash: Boolean,
    onContentChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onAddLink: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    onClearFlash: () -> Unit,
) {
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            kotlinx.coroutines.delay(1200)
            onClearFlash()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Write your entry…") },
            label = { Text("Markdown") },
        )

        if (mediaPaths.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mediaPaths) { path ->
                    Box {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                        )
                        IconButton(
                            onClick = { onRemoveImage(path) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.Add, contentDescription = "Add image")
            }
            TextButton(onClick = onAddLink) {
                Text("🔗 Link")
            }
            Box(modifier = Modifier.weight(1f))
            if (savedFlash) {
                Text("Queued ✓", modifier = Modifier.padding(end = 8.dp))
            }
            Button(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Text("Save", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun LinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert link") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text("https://…") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url) }) { Text("Insert") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
