package com.rainbowcockroach.lifelog.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
    var showLocationPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addImage(uri)
    }

    // Holds the FileProvider Uri the camera app writes the captured photo into, until the
    // result comes back and we import + downscale it like any other picked image.
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) viewModel.addImage(uri)
        pendingCameraUri = null
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
            state = state,
            onContentChange = viewModel::onContentChange,
            onPickImage = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onTakePhoto = {
                val uri = createCameraImageUri(context)
                pendingCameraUri = uri
                takePhoto.launch(uri)
            },
            onAddLink = { showLinkDialog = true },
            onRemoveImage = viewModel::removeImage,
            onSave = viewModel::save,
            onClearFlash = viewModel::clearFlash,
            onOpenLocationPicker = { showLocationPicker = true },
            onOpenTagPicker = { showTagPicker = true },
            onRemoveTag = viewModel::removeTag,
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

        if (showLocationPicker) {
            TagPickerSheet(
                title = "Location",
                type = "location",
                selectedIds = setOfNotNull(state.location?.id),
                allowMultiSelect = false,
                allowCreate = true,
                search = viewModel::searchTags,
                onCreate = viewModel::createTag,
                onPick = { viewModel.setLocation(it) },
                onDismiss = { showLocationPicker = false },
            )
        }

        if (showTagPicker) {
            TagPickerSheet(
                title = "Tags",
                type = "tag",
                selectedIds = state.tags.map { it.id }.toSet(),
                allowMultiSelect = true,
                allowCreate = true,
                search = viewModel::searchTags,
                onCreate = viewModel::createTag,
                onPick = { viewModel.toggleTag(it) },
                onDismiss = { showTagPicker = false },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorContent(
    padding: PaddingValues,
    state: EditorUiState,
    onContentChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onAddLink: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    onClearFlash: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    onOpenTagPicker: () -> Unit,
    onRemoveTag: (com.rainbowcockroach.lifelog.data.local.CachedTag) -> Unit,
) {
    LaunchedEffect(state.savedFlash) {
        if (state.savedFlash) {
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
        // Location (required) + tags toggles, single row.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = onOpenLocationPicker,
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text(state.location?.name ?: "Location *") },
                colors = if (state.location == null) {
                    AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.error,
                        leadingIconContentColor = MaterialTheme.colorScheme.error,
                    )
                } else AssistChipDefaults.assistChipColors(),
            )
            AssistChip(
                onClick = onOpenTagPicker,
                label = {
                    val count = state.tags.size
                    Text(if (count == 0) "Tags" else "Tags · $count")
                },
            )
            state.tags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { onRemoveTag(tag) },
                    label = { Text(tag.name) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                    },
                )
            }
        }

        if (state.errorMessage != null) {
            Text(
                state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        OutlinedTextField(
            value = state.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Write your entry…") },
            label = { Text("Markdown") },
        )

        if (state.mediaPaths.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.mediaPaths) { path ->
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
            TextButton(onClick = onTakePhoto) {
                Text("📷 Photo")
            }
            TextButton(onClick = onAddLink) {
                Text("🔗 Link")
            }
            Box(modifier = Modifier.weight(1f))
            if (state.savedFlash) {
                Text("Queued ✓", modifier = Modifier.padding(end = 8.dp))
            }
            Button(onClick = onSave, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Text("Save", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/**
 * Creates an empty temp file in `filesDir/camera_temp/` and returns a FileProvider content Uri
 * the camera app can write the captured photo into. The photo is imported + downscaled into
 * permanent pending storage on result (see [EditorViewModel.addImage]); these temp files are
 * disposable scratch space.
 */
private fun createCameraImageUri(context: android.content.Context): android.net.Uri {
    val dir = File(context.filesDir, "camera_temp").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
