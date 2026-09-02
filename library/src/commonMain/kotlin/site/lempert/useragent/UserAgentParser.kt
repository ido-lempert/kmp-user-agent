package site.lempert.useragent

import site.lempert.useragent.generated.browserRules
import site.lempert.useragent.generated.deviceRules
import site.lempert.useragent.generated.osRules
import kotlin.js.JsExport

/**
 * Stateless entry point for parsing a raw User-Agent string into structured data.
 *
 * [parse] never throws: unrecognized input (including an empty string) simply
 * results in `null` fields.
 *
 * `@JsExport` makes this existing object visible to JS/TS consumers; it's a
 * no-op on every other target and changes no parsing behavior.
 */
@JsExport
object UserAgentParser {

    fun parse(userAgent: String): UserAgentInfo {
        return UserAgentInfo(
            browser = detectBrowser(userAgent),
            engine = detectEngine(userAgent),
            os = detectOs(userAgent),
            device = detectDevice(userAgent),
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

            val v1 = rule.v1Replacement?.let { applyGroupReplacement(it, match) } ?: groupValueOrNull(match, 2)
            val v2 = rule.v2Replacement?.let { applyGroupReplacement(it, match) } ?: groupValueOrNull(match, 3)
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
                    val groupIndex = template.substring(index + 1, digitsEnd).toIntOrNull()
                    if (groupIndex != null) {
                        append(groupValueOrNull(match, groupIndex).orEmpty())
                    } else {
                        // Digit run too large to fit an Int (never happens in practice for
                        // vendored data, but parse() must never throw regardless of input).
                        append(template, index, digitsEnd)
                    }
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
    // OS detection: matches the generated uap-core `os_parsers` rule table,
    // in file order, first match wins -- same matching/template-substitution
    // mechanic as browser detection above, with one extra optional version
    // segment (v3).
    // -------------------------------------------------------------------

    private data class CompiledOsRule(
        val regex: Regex,
        val osReplacement: String?,
        val v1Replacement: String?,
        val v2Replacement: String?,
        val v3Replacement: String?,
    )

    private val compiledOsRules: List<CompiledOsRule> by lazy {
        osRules.mapNotNull { rule ->
            try {
                CompiledOsRule(
                    regex = Regex(rule.pattern),
                    osReplacement = rule.osReplacement,
                    v1Replacement = rule.v1Replacement,
                    v2Replacement = rule.v2Replacement,
                    v3Replacement = rule.v3Replacement,
                )
            } catch (_: Throwable) {
                // A pattern that somehow fails to compile on this target is
                // skipped rather than crashing every future parse() call.
                null
            }
        }
    }

    private fun detectOs(userAgent: String): Component? {
        for (rule in compiledOsRules) {
            val match = try {
                rule.regex.find(userAgent)
            } catch (_: Throwable) {
                null
            } ?: continue

            val name = rule.osReplacement?.let { applyGroupReplacement(it, match) }
                ?: groupValueOrNull(match, 1)
                ?: continue

            val v1 = rule.v1Replacement?.let { applyGroupReplacement(it, match) } ?: groupValueOrNull(match, 2)
            val v2 = rule.v2Replacement?.let { applyGroupReplacement(it, match) } ?: groupValueOrNull(match, 3)
            val v3 = rule.v3Replacement?.let { applyGroupReplacement(it, match) } ?: groupValueOrNull(match, 4)
            val version = listOfNotNull(v1, v2, v3).takeIf { it.isNotEmpty() }?.joinToString(".")

            return Component(name = name, version = version)
        }
        return null
    }

    // -------------------------------------------------------------------
    // Device detection: matches the generated uap-core `device_parsers` rule
    // table, in file order, first match wins -- same matching/template-
    // substitution mechanic as browser/OS detection above, but with one
    // deliberate divergence: none of name/brand/model falls back to a
    // positional capture group when its replacement field is absent. uap-core's
    // `device_parsers` data never relies on such a default (unlike
    // `family_replacement`/`os_replacement`), so adding one here would
    // fabricate values the vendored data never intends -- see the story's
    // Design Notes.
    // -------------------------------------------------------------------

    private data class CompiledDeviceRule(
        val regex: Regex,
        val deviceReplacement: String?,
        val brandReplacement: String?,
        val modelReplacement: String?,
    )

    private val compiledDeviceRules: List<CompiledDeviceRule> by lazy {
        deviceRules.mapNotNull { rule ->
            try {
                val options = if (rule.regexFlag == "i") setOf(RegexOption.IGNORE_CASE) else emptySet()
                CompiledDeviceRule(
                    regex = Regex(rule.pattern, options),
                    deviceReplacement = rule.deviceReplacement,
                    brandReplacement = rule.brandReplacement,
                    modelReplacement = rule.modelReplacement,
                )
            } catch (_: Throwable) {
                // A pattern that somehow fails to compile on this target is
                // skipped rather than crashing every future parse() call.
                null
            }
        }
    }

    private fun detectDevice(userAgent: String): Device? {
        for (rule in compiledDeviceRules) {
            val match = try {
                rule.regex.find(userAgent)
            } catch (_: Throwable) {
                null
            } ?: continue

            val name = rule.deviceReplacement?.let { applyGroupReplacement(it, match) }
            val brand = rule.brandReplacement?.let { applyGroupReplacement(it, match) }
            val model = rule.modelReplacement?.let { applyGroupReplacement(it, match) }

            return Device(brand = brand, model = model, name = name)
        }
        return null
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
