package com.zionchat.app.data

fun ProviderConfig.isCodexProvider(): Boolean {
    if (type.trim().equals("codex", ignoreCase = true)) return true
    if (presetId?.trim()?.equals("codex", ignoreCase = true) == true) return true
    if (oauthProvider?.trim()?.equals("codex", ignoreCase = true) == true) return true
    if (apiUrl.contains("/backend-api/codex", ignoreCase = true)) return true
    return false
}

