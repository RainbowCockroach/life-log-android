package com.rainbowcockroach.lifelog.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rainbowcockroach.lifelog.BuildConfig

// Only check once per process, so navigation and activity recreation (e.g. a
// config change) don't re-nag the user with the same popup.
private var checkedThisProcess = false

/**
 * Silently checks for a newer release on launch and, only if one exists, shows a
 * dialog offering to open the release page. Stays quiet when up to date or when
 * the check fails — the manual "Check for updates" in Settings surfaces those.
 */
@Composable
fun LaunchUpdateCheck() {
    val context = LocalContext.current
    val checker = remember { UpdateChecker() }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        if (checkedThisProcess) return@LaunchedEffect
        checkedThisProcess = true
        runCatching { checker.check() }
            .onSuccess { if (it.isNewer && it.releaseUrl != null) update = it }
    }

    val u = update ?: return
    AlertDialog(
        onDismissRequest = { update = null },
        title = { Text("Update available") },
        text = {
            Column {
                Text("Latest: ${u.latestTag}${u.latestBuild?.let { " (build $it)" } ?: ""}")
                Text("Installed: ${BuildConfig.VERSION_NAME} (build ${u.currentBuild})")
                if (!u.notes.isNullOrBlank()) {
                    Text(
                        u.notes.take(800),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Text(
                    "Tap Download to open the release page in your browser, " +
                        "then download and install the APK from there.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                update = null
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(u.releaseUrl))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = { update = null }) { Text("Later") }
        },
    )
}
