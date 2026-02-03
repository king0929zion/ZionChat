package com.zionchat.app

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                    val appContainer = remember { AppContainer(applicationContext) }
                    val navController = rememberNavController()
                    CompositionLocalProvider(
                        LocalAppRepository provides appContainer.repository,
                        LocalChatApiClient provides appContainer.chatApiClient
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "chat"
                        ) {
                            composable("chat") { ChatScreen(navController) }
                            composable("settings") { SettingsScreen(navController) }
                            composable("personalization") { PersonalizationScreen(navController) }
                            composable("memories") { MemoriesScreen(navController) }
                            composable("default_model") { DefaultModelScreen(navController) }
                            composable("model_services") { ModelServicesScreen(navController) }
                            composable("add_oauth_provider") { AddOAuthProviderScreen(navController) }
                            composable(
                                route = "add_provider?preset={preset}&providerId={providerId}",
                                arguments = listOf(
                                    navArgument("preset") { defaultValue = "" },
                                    navArgument("providerId") { defaultValue = "" }
                                )
                            ) { backStackEntry ->
                                val preset = backStackEntry.arguments?.getString("preset")
                                val providerId = backStackEntry.arguments?.getString("providerId")
                                AddProviderScreen(navController, preset, providerId)
                            }
                            composable(
                                route = "models?providerId={providerId}",
                                arguments = listOf(navArgument("providerId") { defaultValue = "" })
                            ) { backStackEntry ->
                                val providerId = backStackEntry.arguments?.getString("providerId")
                                ModelsScreen(navController, providerId)
                            }
                            composable(
                                route = "model_config?id={id}",
                                arguments = listOf(navArgument("id") { defaultValue = "" })
                            ) { backStackEntry ->
                                val modelId = backStackEntry.arguments?.getString("id")
                                ModelConfigScreen(navController, modelId)
                            }
                        }
                    }
                }
            }
        }
    }
}
