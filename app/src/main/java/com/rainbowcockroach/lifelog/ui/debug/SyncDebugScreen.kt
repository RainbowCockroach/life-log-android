package com.rainbowcockroach.lifelog.ui.debug

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.rainbowcockroach.lifelog.LifeLogApp
import com.rainbowcockroach.lifelog.data.local.PendingEntry
import com.rainbowcockroach.lifelog.sync.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncDebugViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as LifeLogApp).container
    private val repo = container.entryRepository

    val entries: StateFlow<List<PendingEntry>> = repo.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workInfos: StateFlow<List<WorkInfo>> = WorkManager.getInstance(app)
        .getWorkInfosForUniqueWorkFlow(SyncScheduler.UNIQUE_WORK_NAME)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun retryAll() {
        SyncScheduler.schedule(getApplication(), replace = true)
    }

    fun retry(localId: String) {
        viewModelScope.launch {
            repo.resetForRetry(localId)
            SyncScheduler.schedule(getApplication(), replace = true)
        }
    }

    fun discard(localId: String) {
        viewModelScope.launch { repo.discard(localId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDebugScreen(
    onBack: () -> Unit,
    viewModel: SyncDebugViewModel = viewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val workInfos by viewModel.workInfos.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                WorkManagerCard(
                    workInfos = workInfos,
                    onRetryAll = viewModel::retryAll,
                )
            }
            item {
                Text(
                    "Queued entries (${entries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        "Nothing in the queue.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(entries, key = { it.localId }) { entry ->
                    EntryCard(
                        entry = entry,
                        onRetry = { viewModel.retry(entry.localId) },
                        onDiscard = { viewModel.discard(entry.localId) },
                        onCopyError = { copyToClipboard(context, "lifelog-error", it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkManagerCard(workInfos: List<WorkInfo>, onRetryAll: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("WorkManager", style = MaterialTheme.typography.titleMedium)
            if (workInfos.isEmpty()) {
                Text("No sync work scheduled.", style = MaterialTheme.typography.bodyMedium)
            } else {
                workInfos.forEach { wi ->
                    Text(
                        "${wi.state}  · runs: ${wi.runAttemptCount}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val blockedReason = describeStopReason(wi)
                    if (blockedReason != null) {
                        Text(
                            blockedReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onRetryAll,
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Sync now") }
        }
    }
}

private fun describeStopReason(wi: WorkInfo): String? {
    return when (wi.state) {
        WorkInfo.State.ENQUEUED -> "Waiting for constraints (network)…"
        WorkInfo.State.BLOCKED -> "Blocked"
        WorkInfo.State.FAILED -> "Worker failed permanently"
        WorkInfo.State.CANCELLED -> "Cancelled"
        else -> null
    }
}

@Composable
private fun EntryCard(
    entry: PendingEntry,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onCopyError: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${entry.status} · attempts ${entry.attempts}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                formatTime(entry.createdAt),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                entry.content.take(160).ifBlank { "(empty content)" },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            val err = entry.lastError
            if (!err.isNullOrBlank()) {
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedButton(onClick = onRetry) { Text("Retry") }
                if (!err.isNullOrBlank()) {
                    TextButton(onClick = { onCopyError(err) }) { Text("Copy error") }
                }
                TextButton(onClick = onDiscard) { Text("Discard") }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun formatTime(epochMs: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return fmt.format(Date(epochMs))
}
