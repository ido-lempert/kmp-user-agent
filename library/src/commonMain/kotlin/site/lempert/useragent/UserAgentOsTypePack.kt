package site.lempert.useragent

import site.lempert.useragent.generated.osRules
import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.os], using the generated uap-core `os_parsers`
 * rule table. Contributes [generateOsSegment] on the generate side.
 *
 * Deliberately the only file that references `osRules`/[detectOs] -- see
 * [UserAgentBrowserTypes]'s doc comment for why this per-pack file
 * separation matters for JS tree-shaking.
 */
@JsExport
val UserAgentOsTypes: UserAgentTypePack = UserAgentTypePack(
    id = "os",
    detect = { userAgent -> UserAgentInfo(os = detectOs(userAgent)) },
    applyToGenerate = ::generateOsSegment,
)

// -------------------------------------------------------------------
// OS detection: matches the generated uap-core `os_parsers` rule table,
// in file order, first match wins -- same matching/template-substitution
// mechanic as browser detection, with one extra optional version segment
// (v3).
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
            // skipped rather than crashing every future parse call.
            null
        }
    }
}

/** Used by both [UserAgentOsTypes] and [UserAgentAllTypes]. */
internal fun detectOs(userAgent: String): Component? {
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
