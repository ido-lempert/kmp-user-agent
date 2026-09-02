package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Stateless entry point for building a User-Agent string from structured data --
 * the inverse of [UserAgentParser.parse].
 *
 * This story extends Story 2.1's `browser`/`engine`-only generation with OS and
 * (for Android) device segments. [generate] never throws: a `null` or
 * unrecognized `browser`/`engine`/`os`/`device` simply omits the corresponding
 * segment rather than emitting a placeholder.
 *
 * Unlike the parse direction, there is no vendored generate-direction data
 * source -- `uap-core` only ships detection patterns -- so the four per-family
 * templates below are small, hand-authored, and were verified (during planning)
 * to round-trip correctly through the real compiled `UserAgentParser` logic for
 * the same four representative families used throughout parsing: Chrome,
 * Firefox, Safari, Edge.
 *
 * `@JsExport` makes this existing object visible to JS/TS consumers; it's a
 * no-op on every other target and changes no generation behavior.
 */
@JsExport
object UserAgentGenerator {

    private const val BASE = "Mozilla/5.0"

    fun generate(info: UserAgentInfo): String {
        val browser = info.browser
        val engine = info.engine

        when (browser?.name) {
            "Chrome" -> {
                // Chrome's browser and engine versions are structurally the same
                // token in a real Chrome UA -- a real UA can't express them
                // independently, so browser.version is authoritative, with
                // engine.version as a fallback only when engine is plausibly
                // Chrome's own (Blink) -- never borrow an unrelated engine's
                // version (e.g. a self-inconsistent Chrome+Gecko input).
                val engineFallback = engine?.version?.takeIf { engine.name == "Blink" }
                val bv = browser.version?.takeIf { it.isNotBlank() } ?: engineFallback
                if (!bv.isNullOrBlank()) {
                    val osToken = generateOsToken(info.os, info.device, browserFamily = "Chrome")
                    val osPrefix = if (osToken != null) "($osToken) " else ""
                    return "$BASE $osPrefix" + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$bv Safari/537.36"
                }
            }
            "Firefox" -> {
                val engineFallback = engine?.version?.takeIf { engine.name == "Gecko" }
                val bv = browser.version?.takeIf { it.isNotBlank() } ?: engineFallback
                if (!bv.isNullOrBlank()) {
                    // Firefox shares one parenthetical between the OS token and
                    // `rv:`, unlike the leading-parenthetical families below.
                    val osToken = generateOsToken(info.os, info.device, browserFamily = "Firefox")
                    val paren = if (osToken != null) "($osToken; rv:$bv)" else "(rv:$bv)"
                    return "$BASE $paren Gecko/20100101 Firefox/$bv"
                }
            }
            "Safari" -> {
                // Safari's browser (Version/) and engine (AppleWebKit/) tokens are
                // independent in a real UA, unlike Chrome/Firefox above.
                val bv = browser.version
                if (!bv.isNullOrBlank()) {
                    val ev = engine?.version?.takeIf { it.isNotBlank() } ?: bv
                    val osToken = generateOsToken(info.os, info.device, browserFamily = "Safari")
                    val osPrefix = if (osToken != null) "($osToken) " else ""
                    return "$BASE $osPrefix" + "AppleWebKit/$ev (KHTML, like Gecko) Version/$bv Safari/$ev"
                }
            }
            "Edge" -> {
                val bv = browser.version
                if (!bv.isNullOrBlank()) {
                    val ev = engine?.version?.takeIf { it.isNotBlank() } ?: bv
                    val osToken = generateOsToken(info.os, info.device, browserFamily = "Edge")
                    val osPrefix = if (osToken != null) "($osToken) " else ""
                    return "$BASE $osPrefix" + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ev Safari/537.36 Edg/$bv"
                }
            }
        }

        // No recognized browser drove the string above (browser is null, its
        // name isn't one of the four MVP families, or its version -- after
        // fallback -- was missing/blank). Still honor os/device when possible so
        // this case doesn't silently discard them entirely. Fall back to an
        // engine-only token when the engine alone is enough to signal a
        // recognizable family; otherwise the bare base string (with the OS token
        // if there is one). Blink has no standalone token of its own -- the only
        // tokens that signal it (Chrome/Chromium/CriOS/HeadlessChrome) also drive
        // browser detection -- so a Blink-only string isn't constructible and
        // falls through like an unrecognized/absent browser.
        val osToken = generateOsToken(info.os, info.device, browserFamily = null)
        val osPrefix = if (osToken != null) " ($osToken)" else ""
        val ev = engine?.version?.takeIf { it.isNotBlank() }
        if (ev != null) {
            when (engine.name) {
                "Gecko" -> return "$BASE$osPrefix Gecko/20100101 rv:$ev"
                "WebKit" -> return "$BASE$osPrefix AppleWebKit/$ev (KHTML, like Gecko)"
                "Trident" -> return "$BASE$osPrefix Trident/$ev"
                "Presto" -> return "$BASE$osPrefix Presto/$ev"
            }
        }

        return "$BASE$osPrefix"
    }

    /**
     * Builds the OS parenthetical content for [generate]'s per-family
     * templates, or `null` when there's nothing to say (no `os`, an
     * unrecognized `os.name`, a version this story doesn't support, or a
     * (`browserFamily`, `os`) combination known to collide with an unrelated
     * `UserAgentParser` browser-detection rule -- see the `unsafeCombination`
     * check below).
     *
     * Device data is folded in here rather than given its own code path: real
     * UAs embed device info *inside* the OS parenthetical for mobile (e.g.
     * `"Linux; Android 12; Pixel 6"`), so Android is the only family that needs
     * `device` explicitly -- Mac desktop and iPhone/iPad already surface
     * `device` incidentally via `UserAgentParser`'s catch-all `device_parsers`
     * rules once their OS token text is present.
     */
    private fun generateOsToken(os: Component?, device: Device?, browserFamily: String?): String? {
        if (os == null) return null

        // Confirmed by replaying UserAgentParser's real compiled browserRules
        // table: a generic "(iCab|Lunascape|Opera|Android|...) (version)" rule
        // sits ahead of the Firefox/Safari-specific rules but behind the
        // Chrome/Edge ones, so combining Firefox or Safari with an Android OS
        // token (which contains the literal text "Android <version>") causes
        // the whole string to misparse as browser "Android", not Firefox/
        // Safari. Firefox's iOS token is likewise unsafe: its template has no
        // "Version/" token, so the "iPhone; ..." text alone matches an earlier,
        // versionless catch-all instead of the intended browser rule. Chrome
        // and Edge are unaffected (verified) because their own tokens are
        // matched first regardless of OS text present elsewhere in the string.
        val unsafeCombination = (browserFamily == "Firefox" && (os.name == "Android" || os.name == "iOS")) ||
            (browserFamily == "Safari" && os.name == "Android")
        if (unsafeCombination) return null

        return when (os.name) {
            "Windows" -> if (os.version == "10") "Windows 10" else null
            "Mac OS X" -> os.version?.takeIf { it.isNotBlank() }?.let { "Macintosh; Intel Mac OS X $it" }
            "iOS" -> os.version?.takeIf { it.isNotBlank() }?.let {
                val deviceToken = if (device?.model == "iPad") "iPad; CPU iPad OS" else "iPhone; CPU iPhone OS"
                "$deviceToken ${it.replace('.', '_')} like Mac OS X"
            }
            "Android" -> os.version?.takeIf { it.isNotBlank() }?.let { version ->
                val model = device?.model?.takeIf { it.isNotBlank() }
                if (model != null) "Linux; Android $version; $model" else "Linux; Android $version"
            }
            "Linux" -> "X11; Linux x86_64"
            else -> null
        }
    }
}
