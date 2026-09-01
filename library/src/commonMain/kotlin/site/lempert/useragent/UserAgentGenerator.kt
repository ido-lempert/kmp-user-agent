package site.lempert.useragent

/**
 * Stateless entry point for building a User-Agent string from structured data --
 * the inverse of [UserAgentParser.parse].
 *
 * This story covers `browser`/`engine` only; OS/device segments are added in a
 * later story. [generate] never throws: a `null` or unrecognized `browser`/
 * `engine` simply omits the corresponding segment rather than emitting a
 * placeholder.
 *
 * Unlike the parse direction, there is no vendored generate-direction data
 * source -- `uap-core` only ships detection patterns -- so the four per-family
 * templates below are small, hand-authored, and were verified (during planning)
 * to round-trip correctly through the real compiled `UserAgentParser` logic for
 * the same four representative families used throughout parsing: Chrome,
 * Firefox, Safari, Edge.
 */
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
                    return "$BASE AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$bv Safari/537.36"
                }
            }
            "Firefox" -> {
                val engineFallback = engine?.version?.takeIf { engine.name == "Gecko" }
                val bv = browser.version?.takeIf { it.isNotBlank() } ?: engineFallback
                if (!bv.isNullOrBlank()) {
                    return "$BASE (rv:$bv) Gecko/20100101 Firefox/$bv"
                }
            }
            "Safari" -> {
                // Safari's browser (Version/) and engine (AppleWebKit/) tokens are
                // independent in a real UA, unlike Chrome/Firefox above.
                val bv = browser.version
                if (!bv.isNullOrBlank()) {
                    val ev = engine?.version?.takeIf { it.isNotBlank() } ?: bv
                    return "$BASE AppleWebKit/$ev (KHTML, like Gecko) Version/$bv Safari/$ev"
                }
            }
            "Edge" -> {
                val bv = browser.version
                if (!bv.isNullOrBlank()) {
                    val ev = engine?.version?.takeIf { it.isNotBlank() } ?: bv
                    return "$BASE AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ev Safari/537.36 Edg/$bv"
                }
            }
        }

        // No recognized browser drove the string above (browser is null, its
        // name isn't one of the four MVP families, or its version -- after
        // fallback -- was missing/blank). Fall back to an engine-only token when
        // the engine alone is enough to signal a recognizable family; otherwise
        // the bare base string. Blink has no standalone token of its own -- the
        // only tokens that signal it (Chrome/Chromium/CriOS/HeadlessChrome) also
        // drive browser detection -- so a Blink-only string isn't constructible
        // and falls through like an unrecognized/absent browser.
        val ev = engine?.version?.takeIf { it.isNotBlank() }
        if (ev != null) {
            when (engine.name) {
                "Gecko" -> return "$BASE Gecko/20100101 rv:$ev"
                "WebKit" -> return "$BASE AppleWebKit/$ev (KHTML, like Gecko)"
                "Trident" -> return "$BASE Trident/$ev"
                "Presto" -> return "$BASE Presto/$ev"
            }
        }

        return BASE
    }
}
