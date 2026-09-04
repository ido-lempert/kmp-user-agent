package site.lempert.useragent

import site.lempert.useragent.generated.browserRules
import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.browser], using the generated uap-core
 * `user_agent_parsers` rule table (regex + `$1`/`$2` template substitution,
 * first match wins). Contributes [generateBrowserSegment] on the generate
 * side.
 *
 * Deliberately the only file that references `browserRules`/[detectBrowser]:
 * keeping this pack's compiled rule table and detector out of every other
 * pack's file means a JS bundler that tree-shakes away an unused
 * `UserAgentBrowserTypes` also drops the browser rule table (434 entries as
 * of the currently vendored data) entirely.
 */
@JsExport
val UserAgentBrowserTypes: UserAgentTypePack = UserAgentTypePack(
    id = "browser",
    detect = { userAgent -> UserAgentInfo(browser = detectBrowser(userAgent)) },
    applyToGenerate = ::generateBrowserSegment,
)

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
            // skipped rather than crashing every future parse call.
            null
        }
    }
}

/** Used by both [UserAgentBrowserTypes] and [UserAgentAllTypes]. */
internal fun detectBrowser(userAgent: String): Component? {
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
