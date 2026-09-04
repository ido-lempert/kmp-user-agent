package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.aiAgent]: a small, explicitly non-exhaustive,
 * hand-authored table of well-known AI/LLM crawler and agent User-Agent
 * tokens. As with [UserAgentBotTypes], there is no uap-core section to
 * vendor this from, so every entry below is hand-transcribed from that
 * operator's own public documentation (OpenAI, Perplexity, Anthropic,
 * Common Crawl, Amazon, Meta, Mistral AI, DuckDuckGo, Google, Moonshot AI
 * / Kimi, Timpi, Webz.io, You.com) -- never copied from a third-party
 * commercial bot-detection dataset (Story 4.4 used DataDome's site only as
 * a name checklist while planning which agents to add, never as a source
 * for any token/regex). `Google-Extended` is deliberately excluded (no
 * distinct UA string per Google's own docs -- undetectable this way), as
 * is `anthropic-ai`/`Claude-Web` (deprecated). Story 4.4 also
 * *considered* and *dropped* `FacebookBot`, `DeepSeekBot`, and
 * `cohere-ai`/`cohere-training-data-crawler` -- see the spec's Spec
 * Change Log for why (in short: none could be confidently pinned down
 * from that operator's own current documentation). First match wins,
 * table order. Contributes [generateAiAgentSegment] on the generate side.
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
// agent, first match wins. `token` is the literal UA substring the real
// agent is documented to send, separate from the display `name` for the
// Story 4.4 entries where they differ (e.g. Meta-ExternalAgent's real
// token is lowercase `meta-externalagent`) -- mirrors `BotRule`'s
// `token`/`name` split in `UserAgentBotTypePack.kt`. `token` defaults to
// `name` for every Story 4.3 entry, where the two were always identical.
// A plain `Regex.find` + `groupValues.getOrNull` is used instead of the
// uap-core-oriented template-substitution machinery in
// `UserAgentRuleMatching.kt`: every rule here has at most one capture
// group and no replacement template, so that machinery would be overkill.
// -------------------------------------------------------------------

private enum class AiAgentVersionMode { NONE, REQUIRED }

private data class AiAgentRule(
    val name: String,
    val regex: Regex,
    val versionMode: AiAgentVersionMode,
    val token: String = name,
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

    // ---------------------------------------------------------------
    // Story 4.4: broadened roster, "confirmed" tier -- each sourced
    // directly from that operator's own public documentation.
    // ---------------------------------------------------------------
    AiAgentRule("Amazonbot", Regex("Amazonbot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule(
        name = "Meta-ExternalAgent",
        regex = Regex("meta-externalagent/([0-9.]+)"),
        versionMode = AiAgentVersionMode.REQUIRED,
        token = "meta-externalagent",
    ),
    AiAgentRule(
        name = "Meta-ExternalFetcher",
        regex = Regex("meta-externalfetcher/([0-9.]+)"),
        versionMode = AiAgentVersionMode.REQUIRED,
        token = "meta-externalfetcher",
    ),
    AiAgentRule("MistralAI-User", Regex("MistralAI-User/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("DuckAssistBot", Regex("DuckAssistBot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),

    // ---------------------------------------------------------------
    // Story 4.4: broadened roster, "corroborated" tier -- operator
    // identity confirmed via the ai.robots.txt MIT-licensed registry,
    // exact token pinned down from that operator's own current
    // documentation (Google-CloudVertexBot, Kimi-User, Amzn-User) or,
    // where no first-party page could be found quickly, from multiple
    // independent, clearly-attributed corroborating sources (Timpibot,
    // omgili, YouBot). `FacebookBot`, `DeepSeekBot`, and
    // `cohere-ai`/`cohere-training-data-crawler` were considered and
    // dropped -- see the spec's Spec Change Log.
    // ---------------------------------------------------------------
    AiAgentRule("Google-CloudVertexBot", Regex("Google-CloudVertexBot"), AiAgentVersionMode.NONE),
    AiAgentRule("Kimi-User", Regex("Kimi-User/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("Timpibot", Regex("Timpibot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("omgili", Regex("omgili/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("YouBot", Regex("YouBot/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
    AiAgentRule("Amzn-User", Regex("Amzn-User/([0-9.]+)"), AiAgentVersionMode.REQUIRED),
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
        AiAgentVersionMode.NONE -> rule.token
        AiAgentVersionMode.REQUIRED -> {
            val version = aiAgent.version?.takeIf { it.isNotBlank() } ?: return null
            "${rule.token}/$version"
        }
    }
    return "$userAgentBase $token"
}
