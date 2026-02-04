package com.zionchat.app.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class McpClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    
    /**
     * Fetch tools from MCP server
     */
    suspend fun fetchTools(config: McpConfig): Result<List<McpTool>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                when (config.protocol) {
                    McpProtocol.HTTP -> fetchToolsHttp(config)
                    McpProtocol.SSE -> fetchToolsSse(config)
                }
            }
        }
    }
    
    /**
     * Call a tool on MCP server
     */
    suspend fun callTool(config: McpConfig, toolCall: McpToolCall): Result<McpToolResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = config.url.trimEnd('/') + "/tools/execute"
                
                val body = gson.toJson(mapOf(
                    "tool" to toolCall.toolName,
                    "arguments" to toolCall.arguments
                ))
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody(jsonMediaType))
                    .addHeader("Content-Type", "application/json")
                
                config.headers
                    .filter { it.key.isNotBlank() }
                    .forEach { header -> 
                        requestBuilder.addHeader(header.key.trim(), header.value) 
                    }
                
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@use McpToolResult(
                            success = false,
                            content = "",
                            error = "HTTP ${response.code}: $raw"
                        )
                    }
                    
                    parseToolResult(raw)
                }
            }
        }
    }
    
    /**
     * Test connection to MCP server
     */
    suspend fun testConnection(config: McpConfig): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = config.url.trimEnd('/') + "/health"
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                
                config.headers
                    .filter { it.key.isNotBlank() }
                    .forEach { header -> 
                        requestBuilder.addHeader(header.key.trim(), header.value) 
                    }
                
                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}")
                    }
                    "Connected"
                }
            }
        }
    }
    
    private fun fetchToolsHttp(config: McpConfig): List<McpTool> {
        val url = config.url.trimEnd('/') + "/tools"
        
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
        
        config.headers
            .filter { it.key.isNotBlank() }
            .forEach { header -> 
                requestBuilder.addHeader(header.key.trim(), header.value) 
            }
        
        client.newCall(requestBuilder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: $raw")
            }
            
            return parseToolsResponse(raw)
        }
    }
    
    private fun fetchToolsSse(config: McpConfig): List<McpTool> {
        return fetchToolsHttp(config)
    }
    
    private fun parseToolsResponse(raw: String): List<McpTool> {
        val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            ?: return emptyList()
        
        val toolsArray = json.getAsJsonArray("tools") ?: return emptyList()
        
        return toolsArray.mapNotNull { element ->
            val obj = runCatching { element.asJsonObject }.getOrNull() ?: return@mapNotNull null
            
            val name = obj.get("name")?.asString?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            
            val description = obj.get("description")?.asString?.trim().orEmpty()
            
            val parameters = obj.getAsJsonArray("parameters")?.mapNotNull { paramEl ->
                val paramObj = runCatching { paramEl.asJsonObject }.getOrNull() ?: return@mapNotNull null
                McpToolParameter(
                    name = paramObj.get("name")?.asString?.trim().orEmpty(),
                    type = paramObj.get("type")?.asString?.trim() ?: "string",
                    required = paramObj.get("required")?.asBoolean ?: true,
                    description = paramObj.get("description")?.asString?.trim().orEmpty()
                )
            }.orEmpty()
            
            McpTool(name = name, description = description, parameters = parameters)
        }
    }
    
    private fun parseToolResult(raw: String): McpToolResult {
        return runCatching {
            val json = JsonParser.parseString(raw).asJsonObject
            
            val success = json.get("success")?.asBoolean ?: true
            val content = json.get("content")?.asString?.trim().orEmpty()
            val error = json.get("error")?.asString?.trim()
            
            McpToolResult(success = success, content = content, error = error)
        }.getOrElse {
            // Fallback: treat raw response as content
            McpToolResult(success = true, content = raw)
        }
    }
}
