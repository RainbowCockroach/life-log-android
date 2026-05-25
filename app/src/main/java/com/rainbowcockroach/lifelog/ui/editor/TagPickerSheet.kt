package com.rainbowcockroach.lifelog.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rainbowcockroach.lifelog.data.local.CachedTag
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Reusable picker for tags or locations. Drives entirely off the local Room cache so it works
 * offline. The "Create '<x>'" row only appears when the user is online; failure to create surfaces
 * inline.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TagPickerSheet(
    title: String,
    type: String, // "tag" or "location"
    selectedIds: Set<Long>,
    allowMultiSelect: Boolean,
    allowCreate: Boolean,
    search: suspend (query: String, type: String) -> List<CachedTag>,
    onCreate: suspend (name: String, type: String) -> CachedTag,
    onPick: (CachedTag) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CachedTag>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(type) {
        loading = true
        results = search("", type)
        loading = false
    }

    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(180)
            .collect { q ->
                loading = true
                results = search(q, type)
                loading = false
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it; createError = null },
                placeholder = { Text("Search…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            if (createError != null) {
                Text(
                    createError!!,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val trimmed = query.trim()
            val exactExists = results.any { it.name.equals(trimmed, ignoreCase = true) }
            val showCreate = allowCreate && trimmed.isNotEmpty() && !exactExists

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Box(modifier = Modifier.heightIn(min = 120.dp, max = 420.dp)) {
                if (loading && results.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (results.isEmpty() && !showCreate) {
                    Text(
                        if (trimmed.isEmpty()) "No items yet. Connect to sync."
                        else "No matches.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(results, key = { it.id }) { tag ->
                            val selected = tag.id in selectedIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPick(tag)
                                        if (!allowMultiSelect) onDismiss()
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(tag.name, modifier = Modifier.weight(1f))
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        }
                        if (showCreate) {
                            item("create") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        "Create \"$trimmed\"",
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        enabled = !creating,
                                        onClick = {
                                            scope.launch {
                                                creating = true
                                                createError = null
                                                try {
                                                    val created = onCreate(trimmed, type)
                                                    onPick(created)
                                                    if (!allowMultiSelect) onDismiss()
                                                    query = ""
                                                    results = search("", type)
                                                } catch (t: Throwable) {
                                                    createError = "Couldn't create: ${t.message ?: "offline?"}"
                                                } finally {
                                                    creating = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text(if (creating) "Creating…" else "Create")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (allowMultiSelect) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}
