package com.rainbowcockroach.lifelog.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainbowcockroach.lifelog.BuildConfig
import com.rainbowcockroach.lifelog.LifeLogApp
import com.rainbowcockroach.lifelog.update.ApkInstaller
import com.rainbowcockroach.lifelog.update.UpdateChecker
import com.rainbowcockroach.lifelog.update.UpdateInfo
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as LifeLogApp).container.settings

    suspend fun load(): Pair<String, String> =
        settings.currentBaseUrl() to settings.currentApiKey()

    fun save(baseUrl: String, apiKey: String, onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setBaseUrl(baseUrl)
            settings.setApiKey(apiKey)
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSyncDebug: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (b, k) = viewModel.load()
        baseUrl = b
        apiKey = k
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API base URL") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "Use http://10.0.2.2:3000 for the emulator. No trailing slash.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                singleLine = true,
            )

            Button(
                onClick = { viewModel.save(baseUrl, apiKey, onBack) },
                modifier = Modifier.padding(top = 24.dp),
                enabled = loaded,
            ) { Text("Save") }

            OutlinedButton(
                onClick = onOpenSyncDebug,
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("Sync debug") }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            UpdateSection()
        }
    }
}

@Composable
private fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val checker = remember { UpdateChecker() }
    val installer = remember { ApkInstaller(context) }

    var checking by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Text("Updates", style = MaterialTheme.typography.titleMedium)
    Text(
        "Installed: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
    )

    OutlinedButton(
        onClick = {
            error = null
            info = null
            checking = true
            scope.launch {
                runCatching { checker.check() }
                    .onSuccess {
                        info = it
                        showDialog = true
                    }
                    .onFailure { error = it.message ?: "Check failed" }
                checking = false
            }
        },
        enabled = !checking,
        modifier = Modifier.padding(top = 12.dp),
    ) { Text(if (checking) "Checking…" else "Check for updates") }

    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (showDialog && info != null) {
        val u = info!!
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(if (u.isNewer) "Update available" else "Up to date")
            },
            text = {
                Column {
                    Text("Latest: ${u.latestVersion}")
                    Text("Installed: ${u.currentVersion}")
                    if (u.isNewer && !u.notes.isNullOrBlank()) {
                        Text(
                            u.notes.take(800),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    if (u.isNewer && u.apkUrl == null) {
                        Text(
                            "No .apk asset on the latest release.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                if (u.isNewer && u.apkUrl != null) {
                    TextButton(onClick = {
                        showDialog = false
                        if (!installer.hasInstallPermission()) {
                            context.startActivity(
                                installer.installPermissionIntent()
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else {
                            installer.downloadAndInstall(u.apkUrl, u.latestVersion)
                        }
                    }) { Text("Download & install") }
                } else {
                    TextButton(onClick = { showDialog = false }) { Text("OK") }
                }
            },
            dismissButton = {
                if (u.isNewer) {
                    TextButton(onClick = { showDialog = false }) { Text("Later") }
                }
            },
        )
    }
}
