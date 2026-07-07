package com.rainbowcockroach.lifelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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

private object Routes {
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val SYNC_DEBUG = "sync_debug"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as LifeLogApp).container.settings
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            LifeLogTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = Routes.EDITOR) {
                        composable(Routes.EDITOR) {
                            EditorScreen(onOpenSettings = { nav.navigate(Routes.SETTINGS) })
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
}
