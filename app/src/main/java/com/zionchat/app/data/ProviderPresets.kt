package com.zionchat.app.data

data class ProviderPreset(
    val id: String,
    val name: String,
    val type: String,
    val apiUrl: String,
    val iconAsset: String?
)

val DEFAULT_PROVIDER_PRESETS: List<ProviderPreset> = listOf(
    ProviderPreset(
        id = "rikkahub",
        name = "RikkaHub",
        type = "openai",
        apiUrl = "https://api.rikka-ai.com/v1",
        iconAsset = "rikkahub.svg"
    ),
    ProviderPreset(
        id = "openai",
        name = "OpenAI",
        type = "openai",
        apiUrl = "https://api.openai.com/v1",
        iconAsset = "openai.svg"
    ),
    ProviderPreset(
        id = "gemini",
        name = "Gemini",
        type = "google",
        apiUrl = "https://generativelanguage.googleapis.com/v1beta",
        iconAsset = "gemini-color.svg"
    ),
    ProviderPreset(
        id = "aihubmix",
        name = "AiHubMix",
        type = "openai",
        apiUrl = "https://aihubmix.com/v1",
        iconAsset = "aihubmix-color.svg"
    ),
    ProviderPreset(
        id = "siliconflow",
        name = "硅基流动",
        type = "openai",
        apiUrl = "https://api.siliconflow.cn/v1",
        iconAsset = "siliconflow.svg"
    ),
    ProviderPreset(
        id = "deepseek",
        name = "DeepSeek",
        type = "openai",
        apiUrl = "https://api.deepseek.com/v1",
        iconAsset = "deepseek-color.svg"
    ),
    ProviderPreset(
        id = "openrouter",
        name = "OpenRouter",
        type = "openai",
        apiUrl = "https://openrouter.ai/api/v1",
        iconAsset = "openrouter.svg"
    ),
    ProviderPreset(
        id = "tokenpony",
        name = "小马算力",
        type = "openai",
        apiUrl = "https://api.tokenpony.cn/v1",
        iconAsset = "tokenpony.svg"
    ),
    ProviderPreset(
        id = "alibabacloud",
        name = "阿里云百炼",
        type = "openai",
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        iconAsset = "alibabacloud-color.svg"
    ),
    ProviderPreset(
        id = "bytedance",
        name = "火山引擎",
        type = "openai",
        apiUrl = "https://ark.cn-beijing.volces.com/api/v3",
        iconAsset = "bytedance-color.svg"
    ),
    ProviderPreset(
        id = "moonshot",
        name = "月之暗面",
        type = "openai",
        apiUrl = "https://api.moonshot.cn/v1",
        iconAsset = "moonshot.svg"
    ),
    ProviderPreset(
        id = "zhipu",
        name = "智谱AI开放平台",
        type = "openai",
        apiUrl = "https://open.bigmodel.cn/api/paas/v4",
        iconAsset = "zhipu-color.svg"
    ),
    ProviderPreset(
        id = "stepfun",
        name = "阶跃星辰",
        type = "openai",
        apiUrl = "https://api.stepfun.com/v1",
        iconAsset = "stepfun-color.svg"
    ),
    ProviderPreset(
        id = "juhenext",
        name = "JuheNext",
        type = "openai",
        apiUrl = "https://api.juheai.top/v1",
        iconAsset = "juhenext.png"
    ),
    ProviderPreset(
        id = "302ai",
        name = "302.AI",
        type = "openai",
        apiUrl = "https://api.302.ai/v1",
        iconAsset = "302ai.svg"
    ),
    ProviderPreset(
        id = "hunyuan",
        name = "腾讯Hunyuan",
        type = "openai",
        apiUrl = "https://api.hunyuan.cloud.tencent.com/v1",
        iconAsset = "hunyuan-color.svg"
    ),
    ProviderPreset(
        id = "xai",
        name = "xAI",
        type = "openai",
        apiUrl = "https://api.x.ai/v1",
        iconAsset = "xai.svg"
    ),
    ProviderPreset(
        id = "ackai",
        name = "AckAI",
        type = "openai",
        apiUrl = "https://ackai.fun/v1",
        iconAsset = "openai.svg"
    ),
)

fun findProviderPreset(presetId: String?): ProviderPreset? {
    val id = presetId?.trim().orEmpty()
    if (id.isBlank()) return null
    return DEFAULT_PROVIDER_PRESETS.firstOrNull { it.id == id }
}

fun resolveProviderIconAsset(provider: ProviderConfig): String? {
    provider.iconAsset?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    provider.presetId?.let { presetId ->
        findProviderPreset(presetId)?.iconAsset?.let { return it }
    }
    return computeAIIconAssetByName(provider.name)
}

private fun computeAIIconAssetByName(name: String): String? {
    val lowerName = name.lowercase()
    return when {
        PATTERN_RIKKAHUB.containsMatchIn(lowerName) -> "rikkahub.svg"
        PATTERN_OPENAI.containsMatchIn(lowerName) -> "openai.svg"
        PATTERN_GEMINI.containsMatchIn(lowerName) -> "gemini-color.svg"
        PATTERN_DEEPSEEK.containsMatchIn(lowerName) -> "deepseek-color.svg"
        PATTERN_OPENROUTER.containsMatchIn(lowerName) -> "openrouter.svg"
        PATTERN_TOKENPONY.containsMatchIn(lowerName) -> "tokenpony.svg"
        PATTERN_ALIYUN.containsMatchIn(lowerName) -> "alibabacloud-color.svg"
        PATTERN_BYTEDANCE.containsMatchIn(lowerName) -> "bytedance-color.svg"
        PATTERN_SILLICON_CLOUD.containsMatchIn(lowerName) -> "siliconflow.svg"
        PATTERN_AIHUBMIX.containsMatchIn(lowerName) -> "aihubmix-color.svg"
        PATTERN_ZHIPU.containsMatchIn(lowerName) -> "zhipu-color.svg"
        PATTERN_MOONSHOT.containsMatchIn(lowerName) -> "moonshot.svg"
        PATTERN_STEP.containsMatchIn(lowerName) -> "stepfun-color.svg"
        PATTERN_JUHENEXT.containsMatchIn(lowerName) -> "juhenext.png"
        PATTERN_302.containsMatchIn(lowerName) -> "302ai.svg"
        PATTERN_HUNYUAN.containsMatchIn(lowerName) -> "hunyuan-color.svg"
        PATTERN_XAI.containsMatchIn(lowerName) -> "xai.svg"
        else -> null
    }
}

private val PATTERN_RIKKAHUB = Regex("rikka|auto")
private val PATTERN_OPENAI = Regex("(gpt|openai|o\\d)")
private val PATTERN_GEMINI = Regex("(gemini|nano-banana)")
private val PATTERN_DEEPSEEK = Regex("deepseek")
private val PATTERN_OPENROUTER = Regex("openrouter")
private val PATTERN_ZHIPU = Regex("zhipu|智谱|glm")
private val PATTERN_BYTEDANCE = Regex("bytedance|火山")
private val PATTERN_ALIYUN = Regex("aliyun|阿里云|百炼")
private val PATTERN_SILLICON_CLOUD = Regex("silicon|硅基")
private val PATTERN_AIHUBMIX = Regex("aihubmix")
private val PATTERN_HUNYUAN = Regex("hunyuan|tencent")
private val PATTERN_XAI = Regex("xai")
private val PATTERN_JUHENEXT = Regex("juhenext")
private val PATTERN_MOONSHOT = Regex("moonshot|月之暗面")
private val PATTERN_302 = Regex("302")
private val PATTERN_STEP = Regex("step|阶跃")
private val PATTERN_TOKENPONY = Regex("tokenpony|小马算力")

