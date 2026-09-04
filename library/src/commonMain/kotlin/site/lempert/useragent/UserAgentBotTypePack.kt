package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.bot]: a small, explicitly non-exhaustive,
 * hand-authored table of well-known bot/crawler User-Agent tokens. Unlike
 * [UserAgentBrowserTypes]/[UserAgentOsTypes]/[UserAgentDeviceTypes], there is
 * no uap-core section to vendor this from, so every entry below is
 * hand-transcribed from that bot/crawler operator's own public
 * documentation (Google, Microsoft, DuckDuckGo, Baidu, ByteDance,
 * UptimeRobot, Pingdom, StatusCake, Meta, Slack, Postman) -- never copied
 * from a third-party commercial bot-detection dataset. First match wins,
 * table order. Contributes [generateBotSegment] on the generate side.
 *
 * Deliberately the only file that references [botRules]/[detectBot] -- see
 * [UserAgentBrowserTypes]'s doc comment for why this per-pack file
 * separation matters for JS tree-shaking.
 */
@JsExport
val UserAgentBotTypes: UserAgentTypePack = UserAgentTypePack(
    id = "bot",
    detect = { userAgent -> UserAgentInfo(bot = detectBot(userAgent)) },
    applyToGenerate = ::generateBotSegment,
)

// -------------------------------------------------------------------
// Bot detection: a small hand-authored table (no uap-core section exists
// for this category), one rule per well-known bot/crawler, first match
// wins. `token`/`versionSeparator` are the literal substring(s) the real
// bot is documented to send -- sometimes differing in case or shape from
// `name`'s display casing (e.g. Bingbot's real token is lowercase
// "bingbot"; Pingdom's real token has no "/" separator before the
// version) -- used on the generate side to render a string that round-trips
// back through [detectBot]. A plain `Regex.find` + `groupValues.getOrNull`
// is used instead of the uap-core-oriented template-substitution machinery
// in `UserAgentRuleMatching.kt`: every rule here has at most one capture
// group and no replacement template, so that machinery would be overkill.
// -------------------------------------------------------------------

private enum class BotVersionMode { NONE, OPTIONAL, REQUIRED }

private data class BotRule(
    val name: String,
    val regex: Regex,
    val token: String,
    val versionSeparator: String = "/",
    val versionMode: BotVersionMode,
)

private val botRules: List<BotRule> = listOf(
    BotRule("Googlebot", Regex("Googlebot/([0-9.]+)"), "Googlebot", versionMode = BotVersionMode.REQUIRED),
    BotRule("Bingbot", Regex("bingbot/([0-9.]+)"), "bingbot", versionMode = BotVersionMode.REQUIRED),
    BotRule("DuckDuckBot", Regex("DuckDuckBot/([0-9.]+)"), "DuckDuckBot", versionMode = BotVersionMode.REQUIRED),
    BotRule("YandexBot", Regex("YandexBot(?:/([0-9.]+))?"), "YandexBot", versionMode = BotVersionMode.OPTIONAL),
    BotRule("Baiduspider", Regex("Baiduspider/([0-9.]+)"), "Baiduspider", versionMode = BotVersionMode.REQUIRED),
    BotRule("Bytespider", Regex("Bytespider"), "Bytespider", versionMode = BotVersionMode.NONE),
    BotRule("UptimeRobot", Regex("UptimeRobot/([0-9.]+)"), "UptimeRobot", versionMode = BotVersionMode.REQUIRED),
    BotRule(
        name = "Pingdom",
        regex = Regex("Pingdom\\.com_bot_version_([0-9.]+)"),
        token = "Pingdom.com_bot_version_",
        versionSeparator = "",
        versionMode = BotVersionMode.REQUIRED,
    ),
    BotRule("StatusCake", Regex("StatusCake"), "StatusCake", versionMode = BotVersionMode.NONE),
    BotRule(
        name = "facebookexternalhit",
        regex = Regex("facebookexternalhit/([0-9.]+)"),
        token = "facebookexternalhit",
        versionMode = BotVersionMode.REQUIRED,
    ),
    BotRule(
        name = "Slackbot",
        regex = Regex("Slackbot-LinkExpanding"),
        token = "Slackbot-LinkExpanding",
        versionMode = BotVersionMode.NONE,
    ),
    BotRule("PostmanRuntime", Regex("PostmanRuntime/([0-9.]+)"), "PostmanRuntime", versionMode = BotVersionMode.REQUIRED),
)

/** Used by both [UserAgentBotTypes] and [UserAgentAllTypes]. */
internal fun detectBot(userAgent: String): Component? {
    for (rule in botRules) {
        val match = try {
            rule.regex.find(userAgent)
        } catch (_: Throwable) {
            null
        } ?: continue

        // A non-participating optional group (e.g. YandexBot's version)
        // yields "" in `groupValues`, not null -- normalize that to null so
        // an absent version is never a sentinel empty string.
        val version = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return Component(name = rule.name, version = version)
    }
    return null
}

/**
 * [UserAgentBotTypes]'s generate-direction contribution: renders the
 * minimal literal token (plus version, for a rule that has one) that
 * round-trips back through [detectBot]. Returns `null` when `info.bot` is
 * absent, doesn't match a known rule by name, or (for a rule whose version
 * is required) has no usable version.
 */
internal fun generateBotSegment(info: UserAgentInfo): String? {
    val bot = info.bot ?: return null
    val rule = botRules.firstOrNull { it.name == bot.name } ?: return null
    val version = bot.version?.takeIf { it.isNotBlank() }

    val token = when (rule.versionMode) {
        BotVersionMode.NONE -> rule.token
        BotVersionMode.REQUIRED -> version?.let { "${rule.token}${rule.versionSeparator}$it" } ?: return null
        BotVersionMode.OPTIONAL -> version?.let { "${rule.token}${rule.versionSeparator}$it" } ?: rule.token
    }
    return "$userAgentBase $token"
}
