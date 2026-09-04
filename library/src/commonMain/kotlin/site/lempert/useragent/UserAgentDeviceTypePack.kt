package site.lempert.useragent

import site.lempert.useragent.generated.deviceRules
import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.device], using the generated uap-core
 * `device_parsers` rule table. Device data has no independent
 * generate-direction token (it's folded into the OS token -- see
 * [generateOsToken]'s docs), so this pack contributes nothing to generate.
 *
 * Deliberately the only file that references `deviceRules`/[detectDevice]
 * (the largest of the three vendored rule tables, 633 entries as of the
 * currently vendored data) -- see [UserAgentBrowserTypes]'s doc comment for
 * why this per-pack file separation matters for JS tree-shaking.
 */
@JsExport
val UserAgentDeviceTypes: UserAgentTypePack = UserAgentTypePack(
    id = "device",
    detect = { userAgent -> UserAgentInfo(device = detectDevice(userAgent)) },
)

// -------------------------------------------------------------------
// Device detection: matches the generated uap-core `device_parsers` rule
// table, in file order, first match wins -- same matching/template-
// substitution mechanic as browser/OS detection, but with one deliberate
// divergence: none of name/brand/model falls back to a positional capture
// group when its replacement field is absent. uap-core's `device_parsers`
// data never relies on such a default (unlike
// `family_replacement`/`os_replacement`), so adding one here would
// fabricate values the vendored data never intends.
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
            // skipped rather than crashing every future parse call.
            null
        }
    }
}

/** Used by both [UserAgentDeviceTypes] and [UserAgentAllTypes]. */
internal fun detectDevice(userAgent: String): Device? {
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
