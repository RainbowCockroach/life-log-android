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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import com.rainbowcockroach.lifelog.ui.theme.parseColorOrNull
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

    // Two-step date+time override (mirrors the web `datetime-local` input). The date dialog
    // hands off the picked day to the time dialog, which combines them into a local-zone epoch.
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateUtcMillis by remember { mutableStateOf<Long?>(null) }

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

    Scaffold { padding ->
        EditorContent(
            padding = padding,
            state = state,
            pendingCount = pendingCount,
            onForceSync = viewModel::forceSyncNow,
            onOpenSettings = onOpenSettings,
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
            onOpenDatePicker = { showDatePicker = true },
            onClearDateTime = { viewModel.setDateTime(null) },
        )

        if (showDatePicker) {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = state.customDateTime ?: System.currentTimeMillis(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickedDateUtcMillis = dateState.selectedDateMillis
                            showDatePicker = false
                            if (pickedDateUtcMillis != null) showTimePicker = true
                        },
                        enabled = dateState.selectedDateMillis != null,
                    ) { Text("Next") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                },
            ) {
                DatePicker(state = dateState)
            }
        }

        if (showTimePicker) {
            val existing = state.customDateTime?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            }
            val timeState = rememberTimePickerState(
                initialHour = existing?.hour ?: 12,
                initialMinute = existing?.minute ?: 0,
                is24Hour = true,
            )
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Time") },
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    TextButton(onClick = {
                        val dateMillis = pickedDateUtcMillis
                        if (dateMillis != null) {
                            viewModel.setDateTime(
                                combineDateAndTime(dateMillis, timeState.hour, timeState.minute)
                            )
                        }
                        showTimePicker = false
                    }) { Text("Set") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                },
            )
        }

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
    pendingCount: Int,
    onForceSync: () -> Unit,
    onOpenSettings: () -> Unit,
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
    onOpenDatePicker: () -> Unit,
    onClearDateTime: () -> Unit,
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
        // Location (required) + tags toggles, moved up into the space the title used to occupy.
        // Sync + settings actions sit on the trailing edge.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
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
                if (state.customDateTime == null) {
                    AssistChip(
                        onClick = onOpenDatePicker,
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Date") },
                    )
                } else {
                    InputChip(
                        selected = true,
                        onClick = onOpenDatePicker,
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(formatDateTimeLabel(state.customDateTime)) },
                        trailingIcon = {
                            IconButton(onClick = onClearDateTime, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear date", modifier = Modifier.size(14.dp))
                            }
                        },
                    )
                }
                state.tags.forEach { tag ->
                    val tagBg = parseColorOrNull(tag.backgroundColor)
                    val tagFg = parseColorOrNull(tag.textColor)
                    InputChip(
                        selected = true,
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag.name) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                        },
                        // Render each tag in its own server-defined colors when present,
                        // otherwise fall back to the theme's paper chip.
                        colors = if (tagBg != null || tagFg != null) {
                            val content = tagFg ?: MaterialTheme.colorScheme.onSecondaryContainer
                            InputChipDefaults.inputChipColors(
                                selectedContainerColor = tagBg ?: MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = content,
                                selectedTrailingIconColor = content,
                            )
                        } else InputChipDefaults.inputChipColors(),
                    )
                }
            }
            if (pendingCount > 0) {
                BadgedBox(badge = { Badge { Text(pendingCount.toString()) } }) {
                    IconButton(onClick = onForceSync) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync now")
                    }
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                Icon(PhotoCameraIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Photo", modifier = Modifier.padding(start = 6.dp))
            }
            TextButton(onClick = onAddLink) {
                Icon(LinkIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Link", modifier = Modifier.padding(start = 6.dp))
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

/**
 * The Material date picker reports the selected day as UTC midnight; combine that calendar day
 * with the picked wall-clock time in the device's local zone so the entry lands on the intended
 * day/time regardless of timezone (this epoch becomes both the entry id and createdAt).
 */
private fun combineDateAndTime(dateUtcMillis: Long, hour: Int, minute: Int): Long {
    val localDate = Instant.ofEpochMilli(dateUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return localDate.atTime(hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private val dateTimeLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatDateTimeLabel(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(dateTimeLabelFormatter)

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
