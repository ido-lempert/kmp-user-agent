package site.lempert.useragent

import site.lempert.useragent.generated.browserRules

/**
 * Stateless entry point for parsing a raw User-Agent string into structured data.
 *
 * [parse] never throws: unrecognized input (including an empty string) simply
 * results in `null` fields.
 *
 * `os`/`device` are always `null` in this story -- OS and device detection are
 * added in later stories -- but the returned [UserAgentInfo] already carries
 * the full fixed shape so the public API never breaks when they're populated.
 */
object UserAgentParser {

    fun parse(userAgent: String): UserAgentInfo {
        return UserAgentInfo(
            browser = detectBrowser(userAgent),
            engine = detectEngine(userAgent),
            os = null,
            device = null,
        )
    }

    // -------------------------------------------------------------------
    // Browser detection: matches the generated uap-core `user_agent_parsers`
    // rule table, in file order, first match wins.
    // -------------------------------------------------------------------

    private data class CompiledBrowserRule(
        val regex: Regex,
        val familyReplacement: String?,
        val v1Replacement: String?,
        val v2Replacement: String?,
    )

    // A read-only, lazily compiled rule table -- no mutable shared state, and
    // no caching beyond this immutable compiled form of the generated data.
    private val compiledBrowserRules: List<CompiledBrowserRule> by lazy {
        browserRules.mapNotNull { rule ->
            try {
                CompiledBrowserRule(
                    regex = Regex(rule.pattern),
                    familyReplacement = rule.familyReplacement,
                    v1Replacement = rule.v1Replacement,
                    v2Replacement = rule.v2Replacement,
                )
            } catch (_: Throwable) {
                // A pattern that somehow fails to compile on this target is
                // skipped rather than crashing every future parse() call.
                null
            }
        }
    }

    private fun detectBrowser(userAgent: String): Component? {
        for (rule in compiledBrowserRules) {
            val match = try {
                rule.regex.find(userAgent)
            } catch (_: Throwable) {
                null
            } ?: continue

            val family = rule.familyReplacement?.let { applyGroupReplacement(it, match) }
                ?: groupValueOrNull(match, 1)
                ?: continue

            val v1 = rule.v1Replacement ?: groupValueOrNull(match, 2)
            val v2 = rule.v2Replacement ?: groupValueOrNull(match, 3)
            val version = listOfNotNull(v1, v2).takeIf { it.isNotEmpty() }?.joinToString(".")

            return Component(name = family, version = version)
        }
        return null
    }

    /** Substitutes `$1`, `$2`, ... in a uap-core replacement template with match groups. */
    private fun applyGroupReplacement(template: String, match: MatchResult): String? {
        if ('$' !in template) return template
        val result = buildString {
            var index = 0
            while (index < template.length) {
                val c = template[index]
                if (c == '$' && index + 1 < template.length && template[index + 1].isDigit()) {
                    var digitsEnd = index + 1
                    while (digitsEnd < template.length && template[digitsEnd].isDigit()) digitsEnd++
                    val groupIndex = template.substring(index + 1, digitsEnd).toInt()
                    append(groupValueOrNull(match, groupIndex).orEmpty())
                    index = digitsEnd
                } else {
                    append(c)
                    index += 1
                }
            }
        }
        return result.trim().ifEmpty { null }
    }

    private fun groupValueOrNull(match: MatchResult, index: Int): String? = try {
        match.groups[index]?.value
    } catch (_: Throwable) {
        null
    }

    // -------------------------------------------------------------------
    // Engine detection: uap-core's own regexes.yaml has no engine parser
    // section (only user_agent_parsers/os_parsers/device_parsers), so the
    // rendering engine is derived here from well-known UA tokens instead of
    // vendored data -- ordered, first match wins, same evaluation shape as
    // browser detection above.
    // -------------------------------------------------------------------

    private val trident = Regex("Trident/([0-9.]+)")
    private val chromiumBlink = Regex("(?:HeadlessChrome|Chromium|CriOS|Chrome)/([0-9.]+)")
    private val geckoToken = Regex("Gecko/[0-9]+")
    private val geckoVersionFromRv = Regex("rv:([0-9.]+)")
    private val geckoVersionFromToken = Regex("Gecko/([0-9.]+)")
    private val presto = Regex("Presto/([0-9.]+)")
    private val webKit = Regex("AppleWebKit/([0-9.]+)")

    private fun detectEngine(userAgent: String): Component? = try {
        detectEngineOrNull(userAgent)
    } catch (_: Throwable) {
        null
    }

    private fun detectEngineOrNull(userAgent: String): Component? {
        trident.find(userAgent)?.let { return Component("Trident", it.groupValues.getOrNull(1)) }
        chromiumBlink.find(userAgent)?.let { return Component("Blink", it.groupValues.getOrNull(1)) }
        if (geckoToken.containsMatchIn(userAgent)) {
            val version = geckoVersionFromRv.find(userAgent)?.groupValues?.getOrNull(1)
                ?: geckoVersionFromToken.find(userAgent)?.groupValues?.getOrNull(1)
            return Component("Gecko", version)
        }
        presto.find(userAgent)?.let { return Component("Presto", it.groupValues.getOrNull(1)) }
        webKit.find(userAgent)?.let { return Component("WebKit", it.groupValues.getOrNull(1)) }
        return null
    }
}
