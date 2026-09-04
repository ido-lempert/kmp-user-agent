package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Parses only [UserAgentInfo.engine]. uap-core's own `regexes.yaml` has no
 * engine-parser section, so this is derived from well-known UA tokens
 * (Trident/Blink/Gecko/Presto/WebKit) rather than vendored/codegen'd data.
 * Contributes [generateEngineSegment] on the generate side.
 *
 * Deliberately the only file that references the precompiled engine regexes
 * and [detectEngine] -- see [UserAgentBrowserTypes]'s doc comment for why
 * this per-pack file separation matters for JS tree-shaking.
 */
@JsExport
val UserAgentEngineTypes: UserAgentTypePack = UserAgentTypePack(
    id = "engine",
    detect = { userAgent -> UserAgentInfo(engine = detectEngine(userAgent)) },
    applyToGenerate = ::generateEngineSegment,
)

// -------------------------------------------------------------------
// Engine detection: uap-core's own regexes.yaml has no engine parser
// section (only user_agent_parsers/os_parsers/device_parsers), so the
// rendering engine is derived here from well-known UA tokens instead of
// vendored data -- ordered, first match wins, same evaluation shape as
// browser detection.
// -------------------------------------------------------------------

private val trident = Regex("Trident/([0-9.]+)")
private val chromiumBlink = Regex("(?:HeadlessChrome|Chromium|CriOS|Chrome)/([0-9.]+)")
private val geckoToken = Regex("Gecko/[0-9]+")
private val geckoVersionFromRv = Regex("rv:([0-9.]+)")
private val geckoVersionFromToken = Regex("Gecko/([0-9.]+)")
private val presto = Regex("Presto/([0-9.]+)")
private val webKit = Regex("AppleWebKit/([0-9.]+)")

/** Used by both [UserAgentEngineTypes] and [UserAgentAllTypes]. */
internal fun detectEngine(userAgent: String): Component? = try {
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
