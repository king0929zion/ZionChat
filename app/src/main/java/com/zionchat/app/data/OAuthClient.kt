package com.zionchat.app.data

import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class OAuthClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun startCodexOAuth(): OAuthStartResult {
        val state = generateRandomState()
        val codeVerifier = generatePkceCodeVerifier()
        val codeChallenge = generatePkceCodeChallenge(codeVerifier)

        val authUrl =
            Uri.parse(CODEX_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", CODEX_CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", CODEX_REDIRECT_URI)
                .appendQueryParameter("scope", "openid email profile offline_access")
                .appendQueryParameter("state", state)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("prompt", "login")
                .appendQueryParameter("id_token_add_organizations", "true")
                .appendQueryParameter("codex_cli_simplified_flow", "true")
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

    fun startAntigravityOAuth(): OAuthStartResult {
        val state = generateRandomState()
        val redirectUri = "http://localhost:$ANTIGRAVITY_CALLBACK_PORT/oauth-callback"
        val scopes =
            listOf(
                "https://www.googleapis.com/auth/cloud-platform",
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/userinfo.profile",
                "https://www.googleapis.com/auth/cclog",
                "https://www.googleapis.com/auth/experimentsandconfigs"
            ).joinToString(" ")

        val authUrl =
            Uri.parse(ANTIGRAVITY_AUTH_URL).buildUpon()
                .appendQueryParameter("access_type", "offline")
                .appendQueryParameter("client_id", ANTIGRAVITY_CLIENT_ID)
                .appendQueryParameter("prompt", "consent")
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", scopes)
                .appendQueryParameter("state", state)
                .build()
                .toString()

        return OAuthStartResult(
            provider = OAuthProvider.Antigravity,
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
                    val claims = idToken?.let { parseCodexIdTokenClaims(it) }

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

    suspend fun exchangeAntigravity(code: String, redirectUri: String): Result<AntigravityOAuthResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val form =
                    FormBody.Builder()
                        .add("code", code)
                        .add("client_id", ANTIGRAVITY_CLIENT_ID)
                        .add("client_secret", ANTIGRAVITY_CLIENT_SECRET)
                        .add("redirect_uri", redirectUri)
                        .add("grant_type", "authorization_code")
                        .build()

                val request =
                    Request.Builder()
                        .url(ANTIGRAVITY_TOKEN_URL)
                        .post(form)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()

                val token =
                    client.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
                        gson.fromJson(raw, AntigravityTokenResponse::class.java)
                    }

                val accessToken = token.access_token?.trim().orEmpty()
                if (accessToken.isBlank()) error("Missing access_token in response")

                val expiresInSec = token.expires_in?.coerceAtLeast(0L) ?: 0L
                val expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L

                val email = fetchGoogleUserEmail(accessToken)
                val projectId = fetchAntigravityProjectId(accessToken)

                AntigravityOAuthResult(
                    accessToken = accessToken,
                    refreshToken = token.refresh_token?.trim(),
                    expiresAtMs = expiresAtMs,
                    email = email,
                    projectId = projectId
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
                    val claims = idToken?.let { parseCodexIdTokenClaims(it) }

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

    suspend fun refreshAntigravity(refreshToken: String): Result<AntigravityOAuthResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val token = refreshToken.trim()
                if (token.isBlank()) error("Missing refresh token")

                val form =
                    FormBody.Builder()
                        .add("client_id", ANTIGRAVITY_CLIENT_ID)
                        .add("client_secret", ANTIGRAVITY_CLIENT_SECRET)
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", token)
                        .build()

                val request =
                    Request.Builder()
                        .url(ANTIGRAVITY_TOKEN_URL)
                        .post(form)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("User-Agent", ANTIGRAVITY_TOKEN_USER_AGENT)
                        .build()

                val tokenResp =
                    client.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
                        gson.fromJson(raw, AntigravityTokenResponse::class.java)
                    }

                val accessToken = tokenResp.access_token?.trim().orEmpty()
                if (accessToken.isBlank()) error("Missing access_token in response")

                val expiresInSec = tokenResp.expires_in?.coerceAtLeast(0L) ?: 0L
                val expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L

                val email = fetchGoogleUserEmail(accessToken)
                val projectId = fetchAntigravityProjectId(accessToken)
                val newRefreshToken = tokenResp.refresh_token?.trim().takeIf { !it.isNullOrBlank() } ?: token

                AntigravityOAuthResult(
                    accessToken = accessToken,
                    refreshToken = newRefreshToken,
                    expiresAtMs = expiresAtMs,
                    email = email,
                    projectId = projectId
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

    private fun fetchGoogleUserEmail(accessToken: String): String? {
        val request =
            Request.Builder()
                .url(ANTIGRAVITY_USERINFO_URL)
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()

        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
            val parsed = gson.fromJson(raw, GoogleUserInfo::class.java)
            parsed.email?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun fetchAntigravityProjectId(accessToken: String): String? {
        val payload =
            gson.toJson(
                mapOf(
                    "metadata" to
                        mapOf(
                            "ideType" to "ANTIGRAVITY",
                            "platform" to "PLATFORM_UNSPECIFIED",
                            "pluginType" to "GEMINI"
                        )
                )
            )

        val loadUrl = "$ANTIGRAVITY_API_BASE/$ANTIGRAVITY_API_VERSION:loadCodeAssist"
        val request =
            Request.Builder()
                .url(loadUrl)
                .post(payload.toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", ANTIGRAVITY_API_USER_AGENT)
                .addHeader("X-Goog-Api-Client", ANTIGRAVITY_API_CLIENT)
                .addHeader("Client-Metadata", ANTIGRAVITY_CLIENT_METADATA)
                .build()

        val loadJson =
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
                raw
            }

        val loadObj = JsonParser.parseString(loadJson).asJsonObject
        val directProjectId =
            runCatching {
                when (val v = loadObj.get("cloudaicompanionProject")) {
                    null -> null
                    else -> {
                        if (v.isJsonPrimitive) v.asString
                        else if (v.isJsonObject) v.asJsonObject.get("id")?.asString
                        else null
                    }
                }
            }.getOrNull()?.trim().orEmpty()

        if (directProjectId.isNotBlank()) return directProjectId

        val tierId =
            runCatching {
                val tiers = loadObj.getAsJsonArray("allowedTiers") ?: return@runCatching "legacy-tier"
                tiers.firstOrNull { it.isJsonObject && it.asJsonObject.get("isDefault")?.asBoolean == true }
                    ?.asJsonObject
                    ?.get("id")
                    ?.asString
            }.getOrNull()?.trim().takeIf { !it.isNullOrBlank() } ?: "legacy-tier"

        return onboardAntigravity(accessToken, tierId)
    }

    private suspend fun onboardAntigravity(accessToken: String, tierId: String): String? {
        val payload =
            gson.toJson(
                mapOf(
                    "tierId" to tierId,
                    "metadata" to
                        mapOf(
                            "ideType" to "ANTIGRAVITY",
                            "platform" to "PLATFORM_UNSPECIFIED",
                            "pluginType" to "GEMINI"
                        )
                )
            )

        val url = "$ANTIGRAVITY_API_BASE/$ANTIGRAVITY_API_VERSION:onboardUser"
        repeat(5) {
            val request =
                Request.Builder()
                    .url(url)
                    .post(payload.toRequestBody(jsonMediaType))
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", ANTIGRAVITY_API_USER_AGENT)
                    .addHeader("X-Goog-Api-Client", ANTIGRAVITY_API_CLIENT)
                    .addHeader("Client-Metadata", ANTIGRAVITY_CLIENT_METADATA)
                    .build()

            val raw =
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error("HTTP ${response.code}: $body")
                    body
                }

            val obj = JsonParser.parseString(raw).asJsonObject
            val done = obj.get("done")?.asBoolean ?: false
            if (!done) {
                delay(2000)
                return@repeat
            }

            val projectId =
                runCatching {
                    val responseObj = obj.getAsJsonObject("response") ?: return@runCatching null
                    val project = responseObj.get("cloudaicompanionProject") ?: return@runCatching null
                    if (project.isJsonPrimitive) project.asString
                    else if (project.isJsonObject) project.asJsonObject.get("id")?.asString
                    else null
                }.getOrNull()?.trim()

            if (!projectId.isNullOrBlank()) return projectId
        }
        return null
    }

    private fun parseCodexIdTokenClaims(idToken: String): CodexIdClaims? {
        val parts = idToken.split(".")
        if (parts.size != 3) return null
        val payload = base64UrlDecode(parts[1]) ?: return null
        val payloadJson = runCatching { JsonParser.parseString(String(payload)).asJsonObject }.getOrNull() ?: return null
        val email = payloadJson.get("email")?.asString?.trim()
        val accountId =
            payloadJson.getAsJsonObject("https://api.openai.com/auth")
                ?.get("chatgpt_account_id")
                ?.asString
                ?.trim()
        return CodexIdClaims(email = email, accountId = accountId)
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
        IFlow,
        Antigravity
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

    data class AntigravityOAuthResult(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAtMs: Long,
        val email: String?,
        val projectId: String?
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

    private data class AntigravityTokenResponse(
        val access_token: String?,
        val refresh_token: String?,
        val expires_in: Long?,
        val token_type: String?
    )

    private data class GoogleUserInfo(
        val email: String?
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

        private const val IFLOW_AUTHORIZE_URL = "https://iflow.cn/oauth"
        private const val IFLOW_TOKEN_URL = "https://iflow.cn/oauth/token"
        private const val IFLOW_USERINFO_URL = "https://iflow.cn/api/oauth/getUserInfo"
        private const val IFLOW_CLIENT_ID = "10009311001"
        private const val IFLOW_CLIENT_SECRET = "4Z3YjXycVsQvyGF1etiNlIBB4RsqSDtW"
        private const val IFLOW_CALLBACK_PORT = 11451

        private const val ANTIGRAVITY_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val ANTIGRAVITY_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val ANTIGRAVITY_USERINFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json"
        private const val ANTIGRAVITY_CLIENT_ID =
            "1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com"
        private const val ANTIGRAVITY_CLIENT_SECRET = "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"
        private const val ANTIGRAVITY_CALLBACK_PORT = 51121

        private const val ANTIGRAVITY_API_BASE = "https://cloudcode-pa.googleapis.com"
        private const val ANTIGRAVITY_API_VERSION = "v1internal"
        private const val ANTIGRAVITY_API_USER_AGENT = "google-api-nodejs-client/9.15.1"
        private const val ANTIGRAVITY_API_CLIENT = "google-cloud-sdk vscode_cloudshelleditor/0.1"
        private const val ANTIGRAVITY_CLIENT_METADATA =
            """{"ideType":"IDE_UNSPECIFIED","platform":"PLATFORM_UNSPECIFIED","pluginType":"GEMINI"}"""

        private const val ANTIGRAVITY_TOKEN_USER_AGENT = "antigravity/1.104.0 darwin/arm64"
    }
}
