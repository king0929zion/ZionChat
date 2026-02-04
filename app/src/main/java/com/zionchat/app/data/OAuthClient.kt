package com.zionchat.app.data

import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class OAuthClient {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun extractCodexAccountId(idToken: String?, accessToken: String?): String? {
        return parseCodexTokenClaims(idToken)?.accountId ?: parseCodexTokenClaims(accessToken)?.accountId
    }

    fun extractCodexEmail(idToken: String?, accessToken: String?): String? {
        return parseCodexTokenClaims(idToken)?.email ?: parseCodexTokenClaims(accessToken)?.email
    }

    fun startCodexOAuth(): OAuthStartResult {
        val state = generateRandomState()
        val codeVerifier = generatePkceCodeVerifier()
        val codeChallenge = generatePkceCodeChallenge(codeVerifier)

        val authUrl =
            Uri.parse(CODEX_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", CODEX_CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", CODEX_REDIRECT_URI)
                .appendQueryParameter("scope", "openid profile email offline_access")
                .appendQueryParameter("state", state)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("id_token_add_organizations", "true")
                .appendQueryParameter("codex_cli_simplified_flow", "true")
                .appendQueryParameter("originator", CODEX_ORIGINATOR)
                .build()
                .toString()

        return OAuthStartResult(
            provider = OAuthProvider.Codex,
            authUrl = authUrl,
            redirectUri = CODEX_REDIRECT_URI,
            state = state,
            pkceCodeVerifier = codeVerifier
        )
    }

    fun startIFlowOAuth(): OAuthStartResult {
        val state = generateRandomState()
        val redirectUri = "http://localhost:$IFLOW_CALLBACK_PORT/oauth2callback"
        val authUrl =
            Uri.parse(IFLOW_AUTHORIZE_URL).buildUpon()
                .appendQueryParameter("loginMethod", "phone")
                .appendQueryParameter("type", "phone")
                .appendQueryParameter("redirect", redirectUri)
                .appendQueryParameter("state", state)
                .appendQueryParameter("client_id", IFLOW_CLIENT_ID)
                .build()
                .toString()

        return OAuthStartResult(
            provider = OAuthProvider.IFlow,
            authUrl = authUrl,
            redirectUri = redirectUri,
            state = state
        )
    }

    fun parseCallback(input: String): OAuthCallback? {
        val raw = input.trim()
        if (raw.isBlank()) return null

        val uri =
            runCatching {
                when {
                    raw.startsWith("http://", ignoreCase = true) ||
                        raw.startsWith("https://", ignoreCase = true) -> Uri.parse(raw)
                    raw.startsWith("localhost", ignoreCase = true) -> Uri.parse("http://$raw")
                    raw.startsWith("?") -> Uri.parse("http://localhost/$raw")
                    raw.contains("code=") || raw.contains("state=") -> Uri.parse("http://localhost/?$raw")
                    else -> Uri.parse(raw)
                }
            }.getOrNull() ?: return null

        return OAuthCallback(
            code = uri.getQueryParameter("code"),
            state = uri.getQueryParameter("state"),
            error = uri.getQueryParameter("error"),
            errorDescription = uri.getQueryParameter("error_description")
        )
    }

    suspend fun exchangeCodex(code: String, redirectUri: String, pkceCodeVerifier: String): Result<CodexOAuthResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val form =
                    FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("client_id", CODEX_CLIENT_ID)
                        .add("code", code)
                        .add("redirect_uri", redirectUri)
                        .add("code_verifier", pkceCodeVerifier)
                        .build()

                val request =
                    Request.Builder()
                        .url(CODEX_TOKEN_URL)
                        .post(form)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error("HTTP ${response.code}: $raw")

                    val parsed = gson.fromJson(raw, CodexTokenResponse::class.java)
                    val accessToken = parsed.access_token?.trim().orEmpty()
                    if (accessToken.isBlank()) error("Missing access_token in response")

                    val expiresInSec = parsed.expires_in?.coerceAtLeast(0L) ?: 0L
                    val expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L

                    val idToken = parsed.id_token?.trim()
                    val refreshToken = parsed.refresh_token?.trim()

                    val claims =
                        parseCodexTokenClaims(idToken) ?: parseCodexTokenClaims(accessToken)

                    CodexOAuthResult(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        idToken = idToken,
                        expiresAtMs = expiresAtMs,
                        email = claims?.email,
                        accountId = claims?.accountId
                    )
                }
            }
        }
    }

    suspend fun exchangeIFlow(code: String, redirectUri: String): Result<IFlowOAuthResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val form =
                    FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", redirectUri)
                        .add("client_id", IFLOW_CLIENT_ID)
                        .add("client_secret", IFLOW_CLIENT_SECRET)
                        .build()

                val basic =
                    Base64.encodeToString(
                        "${IFLOW_CLIENT_ID}:${IFLOW_CLIENT_SECRET}".toByteArray(),
                        Base64.NO_WRAP
                    )

                val request =
                    Request.Builder()
                        .url(IFLOW_TOKEN_URL)
                        .post(form)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("Authorization", "Basic $basic")
                        .build()

                val token =
                    client.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
                        gson.fromJson(raw, IFlowTokenResponse::class.java)
                    }

                val accessToken = token.access_token?.trim().orEmpty()
                if (accessToken.isBlank()) error("Missing access_token in response")

                val expiresInSec = token.expires_in?.coerceAtLeast(0L) ?: 0L
                val expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L

                val info = fetchIFlowUserInfo(accessToken)
                val apiKey = info.apiKey?.trim().orEmpty()
                if (apiKey.isBlank()) error("Empty apiKey returned by iFlow user info")

                val email = info.email?.trim().takeIf { !it.isNullOrBlank() } ?: info.phone?.trim()

                IFlowOAuthResult(
                    accessToken = accessToken,
                    refreshToken = token.refresh_token?.trim(),
                    apiKey = apiKey,
                    expiresAtMs = expiresAtMs,
                    email = email
                )
            }
        }
    }

    suspend fun refreshCodex(refreshToken: String): Result<CodexOAuthResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val token = refreshToken.trim()
                if (token.isBlank()) error("Missing refresh token")

                val form =
                    FormBody.Builder()
                        .add("client_id", CODEX_CLIENT_ID)
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", token)
                        .add("scope", "openid profile email")
                        .build()

                val request =
                    Request.Builder()
                        .url(CODEX_TOKEN_URL)
                        .post(form)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error("HTTP ${response.code}: $raw")

                    val parsed = gson.fromJson(raw, CodexTokenResponse::class.java)
                    val accessToken = parsed.access_token?.trim().orEmpty()
                    if (accessToken.isBlank()) error("Missing access_token in response")

                    val expiresInSec = parsed.expires_in?.coerceAtLeast(0L) ?: 0L
                    val expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L

                    val idToken = parsed.id_token?.trim()
                    val newRefreshToken = parsed.refresh_token?.trim().takeIf { !it.isNullOrBlank() }

                    val claims =
                        parseCodexTokenClaims(idToken) ?: parseCodexTokenClaims(accessToken)

                    CodexOAuthResult(
                        accessToken = accessToken,
                        refreshToken = newRefreshToken ?: token,
                        idToken = idToken,
                        expiresAtMs = expiresAtMs,
                        email = claims?.email,
                        accountId = claims?.accountId
                    )
                }
            }
        }
    }

    suspend fun refreshIFlow(refreshToken: String): Result<IFlowOAuthResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val token = refreshToken.trim()
                if (token.isBlank()) error("Missing refresh token")

                val form =
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", token)
                        .add("client_id", IFLOW_CLIENT_ID)
                        .add("client_secret", IFLOW_CLIENT_SECRET)
                        .build()

                val basic =
                    Base64.encodeToString(
                        "${IFLOW_CLIENT_ID}:${IFLOW_CLIENT_SECRET}".toByteArray(),
                        Base64.NO_WRAP
                    )

                val request =
                    Request.Builder()
                        .url(IFLOW_TOKEN_URL)
                        .post(form)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("Authorization", "Basic $basic")
                        .build()

                val tokenResp =
                    client.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
                        gson.fromJson(raw, IFlowTokenResponse::class.java)
                    }

                val accessToken = tokenResp.access_token?.trim().orEmpty()
                if (accessToken.isBlank()) error("Missing access_token in response")

                val expiresInSec = tokenResp.expires_in?.coerceAtLeast(0L) ?: 0L
                val expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L

                val info = fetchIFlowUserInfo(accessToken)
                val apiKey = info.apiKey?.trim().orEmpty()
                if (apiKey.isBlank()) error("Empty apiKey returned by iFlow user info")

                val email = info.email?.trim().takeIf { !it.isNullOrBlank() } ?: info.phone?.trim()
                val newRefreshToken = tokenResp.refresh_token?.trim().takeIf { !it.isNullOrBlank() } ?: token

                IFlowOAuthResult(
                    accessToken = accessToken,
                    refreshToken = newRefreshToken,
                    apiKey = apiKey,
                    expiresAtMs = expiresAtMs,
                    email = email
                )
            }
        }
    }

    private fun fetchIFlowUserInfo(accessToken: String): IFlowUserInfoData {
        val url = "${IFLOW_USERINFO_URL}?accessToken=${Uri.encode(accessToken)}"
        val request = Request.Builder().url(url).get().addHeader("Accept", "application/json").build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
            val parsed = gson.fromJson(raw, IFlowUserInfoResponse::class.java)
            if (!parsed.success) error("iFlow user info returned success=false")
            parsed.data ?: error("iFlow user info missing data")
        }
    }

    private fun parseCodexTokenClaims(token: String?): CodexIdClaims? {
        val trimmed = token?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val payloadJson = parseJwtPayload(trimmed) ?: return null
        val email = payloadJson.get("email")?.asString?.trim()
        val accountId = extractAccountIdFromClaims(payloadJson)

        if (email.isNullOrBlank() && accountId.isNullOrBlank()) return null
        return CodexIdClaims(email = email, accountId = accountId)
    }

    private fun parseJwtPayload(token: String): JsonObject? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val payload = base64UrlDecode(parts[1]) ?: return null
        return runCatching { JsonParser.parseString(String(payload)).asJsonObject }.getOrNull()
    }

    private fun extractAccountIdFromClaims(payloadJson: JsonObject): String? {
        payloadJson.get("chatgpt_account_id")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        payloadJson.getAsJsonObject("https://api.openai.com/auth")
            ?.get("chatgpt_account_id")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val org0 =
            payloadJson.getAsJsonArray("organizations")
                ?.firstOrNull()
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
        org0?.get("id")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return null
    }

    private fun generateRandomState(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generatePkceCodeVerifier(): String {
        val bytes = ByteArray(96)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generatePkceCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun base64UrlDecode(input: String): ByteArray? {
        var s = input
        val mod = s.length % 4
        if (mod != 0) s += "=".repeat(4 - mod)
        return runCatching { Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP) }.getOrNull()
    }

    data class OAuthStartResult(
        val provider: OAuthProvider,
        val authUrl: String,
        val redirectUri: String,
        val state: String,
        val pkceCodeVerifier: String? = null
    )

    enum class OAuthProvider {
        Codex,
        IFlow
    }

    data class OAuthCallback(
        val code: String?,
        val state: String?,
        val error: String?,
        val errorDescription: String?
    )

    data class CodexOAuthResult(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val expiresAtMs: Long,
        val email: String?,
        val accountId: String?
    )

    data class IFlowOAuthResult(
        val accessToken: String,
        val refreshToken: String?,
        val apiKey: String,
        val expiresAtMs: Long,
        val email: String?
    )

    private data class CodexTokenResponse(
        val access_token: String?,
        val refresh_token: String?,
        val id_token: String?,
        val expires_in: Long?
    )

    private data class IFlowTokenResponse(
        val access_token: String?,
        val refresh_token: String?,
        val expires_in: Long?,
        val token_type: String?,
        val scope: String?
    )

    private data class IFlowUserInfoResponse(
        val success: Boolean,
        val data: IFlowUserInfoData?
    )

    private data class IFlowUserInfoData(
        val apiKey: String?,
        val email: String?,
        val phone: String?
    )

    private data class CodexIdClaims(
        val email: String?,
        val accountId: String?
    )

    companion object {
        private const val CODEX_AUTH_URL = "https://auth.openai.com/oauth/authorize"
        private const val CODEX_TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val CODEX_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val CODEX_REDIRECT_URI = "http://localhost:1455/auth/callback"
        private const val CODEX_ORIGINATOR = "codex_cli_rs"

        private const val IFLOW_AUTHORIZE_URL = "https://iflow.cn/oauth"
        private const val IFLOW_TOKEN_URL = "https://iflow.cn/oauth/token"
        private const val IFLOW_USERINFO_URL = "https://iflow.cn/api/oauth/getUserInfo"
        private const val IFLOW_CLIENT_ID = "10009311001"
        private const val IFLOW_CLIENT_SECRET = "4Z3YjXycVsQvyGF1etiNlIBB4RsqSDtW"
        private const val IFLOW_CALLBACK_PORT = 11451
    }
}
