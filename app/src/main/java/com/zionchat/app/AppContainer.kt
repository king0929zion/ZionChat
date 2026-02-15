package com.zionchat.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.zionchat.app.data.AppRepository
import com.zionchat.app.data.ChatApiClient
import com.zionchat.app.data.LocalBridgeRuntimePackagingService
import com.zionchat.app.data.OAuthClient
import com.zionchat.app.data.ProviderAuthManager
import com.zionchat.app.data.RuntimePackagingConfig
import com.zionchat.app.data.RuntimePackagingService
import com.zionchat.app.data.VercelDeployService
import com.zionchat.app.data.WebHostingService

class AppContainer(context: Context) {
    val repository = AppRepository(context)
    val chatApiClient = ChatApiClient()
    val oauthClient = OAuthClient()
    val providerAuthManager = ProviderAuthManager(repository, oauthClient)
    val webHostingService: WebHostingService = VercelDeployService()
    val runtimePackagingService: RuntimePackagingService =
        LocalBridgeRuntimePackagingService(
            appContext = context.applicationContext,
            config =
                RuntimePackagingConfig(
                    localBridgeBaseUrl = BuildConfig.RUNTIME_PACKAGER_BASE_URL,
                    localBridgeToken = BuildConfig.RUNTIME_PACKAGER_TOKEN
                )
        )
}

val LocalAppRepository = staticCompositionLocalOf<AppRepository> {
    error("LocalAppRepository not provided")
}

val LocalChatApiClient = staticCompositionLocalOf<ChatApiClient> {
    error("LocalChatApiClient not provided")
}

val LocalOAuthClient = staticCompositionLocalOf<OAuthClient> {
    error("LocalOAuthClient not provided")
}

val LocalProviderAuthManager = staticCompositionLocalOf<ProviderAuthManager> {
    error("LocalProviderAuthManager not provided")
}

val LocalWebHostingService = staticCompositionLocalOf<WebHostingService> {
    error("LocalWebHostingService not provided")
}

val LocalRuntimePackagingService = staticCompositionLocalOf<RuntimePackagingService> {
    error("LocalRuntimePackagingService not provided")
}
