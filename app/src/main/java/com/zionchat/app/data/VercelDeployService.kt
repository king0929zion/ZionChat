package com.zionchat.app.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

class VercelDeployService : WebHostingService {
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    override suspend fun validateConfig(config: WebHostingConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val token = config.token.trim()
                if (token.isBlank()) error("Missing Vercel token")

                val request =
                    Request.Builder()
                        .url("https://api.vercel.com/v2/user")
                        .get()
                        .header("Authorization", "Bearer $token")
                        .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}: $raw")
                    }
                }
            }
        }
    }

    override suspend fun deployApp(
        appId: String,
        html: String,
        config: WebHostingConfig
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val token = config.token.trim()
                val content = html.trim()
                if (token.isBlank()) error("Missing Vercel token")
                if (content.isBlank()) error("HTML payload is empty")

                val deploymentName = buildDeploymentName(appId)
                val payload = mutableMapOf<String, Any>(
                    "name" to deploymentName,
                    "target" to "production",
                    "files" to listOf(
                        mapOf(
                            "file" to "index.html",
                            "data" to content
                        )
                    ),
                    "projectSettings" to mapOf(
                        "framework" to null,
                        "buildCommand" to null,
                        "outputDirectory" to null
                    )
                )

                val projectId = config.projectId.trim()
                if (projectId.isNotBlank()) {
                    payload["project"] = projectId
                }

                val urlBuilder =
                    "https://api.vercel.com/v13/deployments".toHttpUrl().newBuilder()
                        .addQueryParameter("forceNew", "0")
                val teamId = config.teamId.trim()
                if (teamId.isNotBlank()) {
                    urlBuilder.addQueryParameter("teamId", teamId)
                }

                val body = gson.toJson(payload)
                val request =
                    Request.Builder()
                        .url(urlBuilder.build())
                        .post(body.toRequestBody(jsonMediaType))
                        .header("Authorization", "Bearer $token")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}: $raw")
                    }
                    parseDeploymentUrl(raw)
                }
            }
        }
    }

    private fun buildDeploymentName(appId: String): String {
        val normalized = appId.trim().lowercase().replace(Regex("[^a-z0-9-]"), "-")
        val compact = normalized.replace(Regex("-+"), "-").trim('-')
        val suffix = compact.takeIf { it.isNotBlank() }?.take(32) ?: "app"
        return "zionchat-$suffix"
    }

    private fun parseDeploymentUrl(raw: String): String {
        val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            ?: error("Invalid deployment response")
        val primary =
            json.get("url")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        if (primary.isNotBlank()) {
            return normalizeUrl(primary)
        }
        val alias =
            json.getAsJsonArray("alias")
                ?.firstOrNull()
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.trim()
                .orEmpty()
        if (alias.isNotBlank()) {
            return normalizeUrl(alias)
        }
        error("Missing deployment url")
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return "https://$trimmed"
    }
}

