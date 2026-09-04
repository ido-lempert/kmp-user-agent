package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.aiAgent]: a small, explicitly non-exhaustive,
 * hand-authored table of well-known AI/LLM crawler and agent User-Agent
 * tokens. As with [UserAgentBotTypes], there is no uap-core section to
 * vendor this from, so every entry below is hand-transcribed from that
 * operator's own public documentation (OpenAI, Perplexity, Anthropic,
 * Common Crawl) -- never copied from a third-party commercial
 * bot-detection dataset. `Google-Extended` is deliberately excluded (no
 * distinct UA string per Google's own docs -- undetectable this way), as
 * is `anthropic-ai`/`Claude-Web` (deprecated). First match wins, table
 * order. Contributes [generateAiAgentSegment] on the generate side.
 *
 * Deliberately the only file that references [aiAgentRules]/
 * [detectAiAgent] -- see [UserAgentBrowserTypes]'s doc comment for why this
 * per-pack file separation matters for JS tree-shaking.
 */
@JsExport
val UserAgentAIAgentTypes: UserAgentTypePack = UserAgentTypePack(
    id = "aiAgent",
    detect = { userAgent -> UserAgentInfo(aiAgent = detectAiAgent(userAgent)) },
    applyToGenerate = ::generateAiAgentSegment,
)

// -------------------------------------------------------------------
// AI-agent detection: a small hand-authored table (no uap-core section
// exists for this category), one rule per well-known AI/LLM crawler or
// agent, first match wins. Every entry's literal token matches its display
// `name` exactly (unlike a couple of UserAgentBotTypes's entries), so
// `name` doubles as the generate-side token. A plain `Regex.find` +
// `groupValues.getOrNull` is used instead of the uap-core-oriented
// template-substitution machinery in `UserAgentRuleMatching.kt`: every
// rule here has at most one capture group and no replacement template, so
// that machinery would be overkill.
// -------------------------------------------------------------------

private enum class AiAgentVersionMode { NONE, REQUIRED }

private data class AiAgentRule(
    val name: String,
    val regex: Regex,
    val versionMode: AiAgentVersionMode,
)

private val aiAgentRules: List<AiAgentRule> = listOf(
    AiAgentRule("GPTBot", Regex("GPTBot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("ChatGPT-User", Regex("ChatGPT-User/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("OAI-SearchBot", Regex("OAI-SearchBot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("PerplexityBot", Regex("PerplexityBot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("Perplexity-User", Regex("Perplexity-User/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("ClaudeBot", Regex("ClaudeBot"), AiAgentVersionMode.NONE),
    AiAgentRule("Claude-User", Regex("Claude-User"), AiAgentVersionMode.NONE),
    AiAgentRule("Claude-SearchBot", Regex("Claude-SearchBot"), AiAgentVersionMode.NONE),
    AiAgentRule("CCBot", Regex("CCBot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
)

/** Used by both [UserAgentAIAgentTypes] and [UserAgentAllTypes]. */
internal fun detectAiAgent(userAgent: String): Component? {
    for (rule in aiAgentRules) {
        val match = try {
            rule.regex.find(userAgent)
        } catch (_: Throwable) {
            null
        } ?: continue

        val version = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return Component(name = rule.name, version = version)
    }
    return null
}

/**
 * [UserAgentAIAgentTypes]'s generate-direction contribution: renders the
 * minimal literal token (plus version, for a rule that has one) that
 * round-trips back through [detectAiAgent]. Returns `null` when
 * `info.aiAgent` is absent, doesn't match a known rule by name, or (for a
 * rule whose version is required) has no usable version.
 */
internal fun generateAiAgentSegment(info: UserAgentInfo): String? {
    val aiAgent = info.aiAgent ?: return null
    val rule = aiAgentRules.firstOrNull { it.name == aiAgent.name } ?: return null

    val token = when (rule.versionMode) {
        AiAgentVersionMode.NONE -> rule.name
        AiAgentVersionMode.REQUIRED -> {
            val version = aiAgent.version?.takeIf { it.isNotBlank() } ?: return null
            "${rule.name}/$version"
        }
    }
    return "$userAgentBase $token"
}
