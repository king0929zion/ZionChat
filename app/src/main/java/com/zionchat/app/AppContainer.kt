package com.zionchat.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.zionchat.app.data.AppRepository
import com.zionchat.app.data.ChatApiClient
import com.zionchat.app.data.OAuthClient
import com.zionchat.app.data.ProviderAuthManager

class AppContainer(context: Context) {
    val repository = AppRepository(context)
    val chatApiClient = ChatApiClient()
    val oauthClient = OAuthClient()
    val providerAuthManager = ProviderAuthManager(repository, oauthClient)
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
