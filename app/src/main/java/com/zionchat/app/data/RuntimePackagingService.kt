package com.zionchat.app.data

interface RuntimePackagingService {
    suspend fun triggerRuntimePackaging(
        app: SavedApp,
        deployUrl: String,
        versionModel: Int
    ): Result<SavedApp>

    suspend fun syncRuntimePackaging(app: SavedApp): Result<SavedApp>
}

