package com.zionchat.app

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zionchat.app.ui.screens.*
import com.zionchat.app.ui.theme.ZionChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            ZionChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "chat"
                    ) {
                        composable("chat") { ChatScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("model_services") { ModelServicesScreen(navController) }
                        composable("add_provider") { backStackEntry ->
                            val preset = backStackEntry.arguments?.getString("preset")
                            AddProviderScreen(navController, preset)
                        }
                        composable("models") { ModelsScreen(navController) }
                        composable("model_config") { ModelConfigScreen(navController) }
                    }
                }
            }
        }
    }
}
