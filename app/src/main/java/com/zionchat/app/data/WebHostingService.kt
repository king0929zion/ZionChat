package com.zionchat.app.data

interface WebHostingService {
    suspend fun validateConfig(config: WebHostingConfig): Result<Unit>

    suspend fun deployApp(
        appId: String,
        html: String,
        config: WebHostingConfig
    ): Result<String>
}

