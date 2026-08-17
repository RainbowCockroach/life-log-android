package com.rainbowcockroach.lifelog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rainbowcockroach.lifelog.ui.debug.SyncDebugScreen
import com.rainbowcockroach.lifelog.ui.editor.EditorScreen
import com.rainbowcockroach.lifelog.ui.settings.SettingsScreen
import com.rainbowcockroach.lifelog.ui.theme.LifeLogTheme
import com.rainbowcockroach.lifelog.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow

private object Routes {
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val SYNC_DEBUG = "sync_debug"
}

class MainActivity : ComponentActivity() {

    /**
     * Images handed to us by the system share sheet, waiting for the editor to import them.
     * Emptied by [EditorScreen] once consumed so a configuration change can't re-import them.
     */
    private val sharedImages = MutableStateFlow<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: after a recreation the original intent is redelivered, but the
        // images it carried were already imported by the previous instance.
        if (savedInstanceState == null) acceptSharedImages(intent)
        val settings = (application as LifeLogApp).container.settings
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            LifeLogTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    // A share can arrive while the user is deep in Settings; bring the editor
                    // back to the front so they actually see the entry being started.
                    val pendingShare by sharedImages.collectAsState()
                    LaunchedEffect(pendingShare) {
                        if (pendingShare.isNotEmpty()) {
                            nav.popBackStack(Routes.EDITOR, inclusive = false)
                        }
                    }
                    NavHost(navController = nav, startDestination = Routes.EDITOR) {
                        composable(Routes.EDITOR) {
                            EditorScreen(
                                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                                sharedImages = sharedImages,
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onBack = { nav.popBackStack() },
                                onOpenSyncDebug = { nav.navigate(Routes.SYNC_DEBUG) },
                            )
                        }
                        composable(Routes.SYNC_DEBUG) {
                            SyncDebugScreen(onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    /** Sharing into an already-running app lands here (the activity is `singleTask`). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptSharedImages(intent)
    }

    private fun acceptSharedImages(intent: Intent?) {
        val uris = intent?.extractSharedImageUris().orEmpty()
        if (uris.isNotEmpty()) sharedImages.value = uris
    }
}
