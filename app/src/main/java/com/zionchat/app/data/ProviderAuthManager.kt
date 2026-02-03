package com.zionchat.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProviderAuthManager(
    private val repository: AppRepository,
    private val oauthClient: OAuthClient
) {
    suspend fun ensureValidProvider(provider: ProviderConfig, forceRefresh: Boolean = false): ProviderConfig {
        val oauthProvider = provider.oauthProvider?.trim()?.lowercase().orEmpty()
        if (oauthProvider.isBlank()) return provider

        val refreshToken = provider.oauthRefreshToken?.trim().orEmpty()
        if (refreshToken.isBlank()) return provider

        val expiresAtMs = provider.oauthExpiresAtMs ?: Long.MAX_VALUE
        val now = System.currentTimeMillis()
        val shouldRefresh = forceRefresh || expiresAtMs <= now + EXPIRY_SKEW_MS
        if (!shouldRefresh) return provider

        return withContext(Dispatchers.IO) {
            val updated =
                when (oauthProvider) {
                    "codex" ->
                        oauthClient.refreshCodex(refreshToken).getOrThrow().let { token ->
                            provider.copy(
                                apiKey = token.accessToken,
                                oauthAccessToken = token.accessToken,
                                oauthRefreshToken = token.refreshToken ?: provider.oauthRefreshToken,
                                oauthIdToken = token.idToken ?: provider.oauthIdToken,
                                oauthAccountId = token.accountId ?: provider.oauthAccountId,
                                oauthEmail = token.email ?: provider.oauthEmail,
                                oauthExpiresAtMs = token.expiresAtMs
                            )
                        }
                    "iflow" ->
                        oauthClient.refreshIFlow(refreshToken).getOrThrow().let { token ->
                            provider.copy(
                                apiKey = token.apiKey,
                                oauthAccessToken = token.accessToken,
                                oauthRefreshToken = token.refreshToken ?: provider.oauthRefreshToken,
                                oauthEmail = token.email ?: provider.oauthEmail,
                                oauthExpiresAtMs = token.expiresAtMs
                            )
                        }
                    "antigravity" ->
                        oauthClient.refreshAntigravity(refreshToken).getOrThrow().let { token ->
                            provider.copy(
                                apiKey = token.accessToken,
                                oauthAccessToken = token.accessToken,
                                oauthRefreshToken = token.refreshToken ?: provider.oauthRefreshToken,
                                oauthEmail = token.email ?: provider.oauthEmail,
                                oauthProjectId = token.projectId ?: provider.oauthProjectId,
                                oauthExpiresAtMs = token.expiresAtMs
                            )
                        }
                    "gemini-cli" ->
                        oauthClient.refreshGeminiCli(refreshToken).getOrThrow().let { token ->
                            provider.copy(
                                apiKey = token.accessToken,
                                oauthAccessToken = token.accessToken,
                                oauthRefreshToken = token.refreshToken ?: provider.oauthRefreshToken,
                                oauthEmail = token.email ?: provider.oauthEmail,
                                oauthProjectId = token.projectId ?: provider.oauthProjectId,
                                oauthExpiresAtMs = token.expiresAtMs
                            )
                        }
                    else -> provider
                }

            if (updated != provider) {
                repository.upsertProvider(updated)
            }
            updated
        }
    }

    private companion object {
        private const val EXPIRY_SKEW_MS = 60_000L
    }
}
