package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Stateless factory returning a function that builds a User-Agent string from
 * structured data -- the inverse of [UserAgentParser].
 *
 * [packs] is a variadic list of [UserAgentTypePack]s. Passing none returns a
 * function that always produces the bare [userAgentBase] string -- it does
 * **not** fall back to [UserAgentAllTypes]. Consumers who want full
 * generation must pass [UserAgentAllTypes] explicitly. (See
 * [UserAgentParser]'s doc comment, and the spec's Spec Change Log, for why:
 * an implicit fallback here would give every `UserAgentGenerator` call site
 * a static reachability edge to every built-in pack under standard JS
 * bundlers, regardless of which packs it actually passes.)
 *
 * When packs are passed, the returned function tries each pack's
 * [UserAgentTypePack.applyToGenerate] in the order given and returns the
 * first non-null result; if every pack contributes nothing (including the
 * empty-packs case above), it falls back to the bare [userAgentBase] string.
 * [UserAgentAllTypes]'s own `applyToGenerate` ([generateFullUserAgentString])
 * reproduces this same fallback chain internally (browser segment, then
 * engine segment, then OS segment, then bare base), so passing just
 * [UserAgentAllTypes] behaves identically to composing the four narrower
 * built-in packs.
 *
 * The returned function never throws: a `null` or unrecognized
 * `browser`/`engine`/`os`/`device` on the input [UserAgentInfo], or an
 * exception thrown by a (possibly third-party) pack's `applyToGenerate`,
 * simply omits that pack's contribution rather than propagating.
 *
 * `@JsExport` makes this function visible to JS/TS consumers; it's a no-op
 * on every other target and changes no generation behavior.
 */
@JsExport
fun UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String {
    return { info ->
        packs.firstNotNullOfOrNull { pack ->
            try {
                pack.applyToGenerate(info)
            } catch (_: Throwable) {
                null
            }
        } ?: userAgentBase
    }
}

/**
 * The literal User-Agent string prefix every generated string starts with.
 * A true compile-time constant (`const val` of a primitive/String), so --
 * unlike the top-level `val`s elsewhere in this package -- it's inlined at
 * each use site with no runtime storage or lazy-init gate of its own.
 */
internal const val userAgentBase = "Mozilla/5.0"

/**
 * Builds the OS parenthetical content used by [generateBrowserSegment]/
 * [generateEngineSegment]/[generateOsSegment], or `null` when there's
 * nothing to say (no `os`, an unrecognized `os.name`, a version this story
 * doesn't support, or a (`browserFamily`, `os`) combination known to
 * collide with an unrelated [UserAgentParser] browser-detection rule -- see
 * the `unsafeCombination` check below).
 *
 * Device data is folded in here rather than given its own code path: real
 * UAs embed device info *inside* the OS parenthetical for mobile (e.g.
 * `"Linux; Android 12; Pixel 6"`), so Android is the only family that needs
 * `device` explicitly -- Mac desktop and iPhone/iPad already surface
 * `device` incidentally via [UserAgentParser]'s catch-all `device_parsers`
 * rules once their OS token text is present. This is also why
 * [UserAgentDeviceTypes] has no `applyToGenerate` of its own: device data
 * alone has no independent generate-direction token.
 */
internal fun generateOsToken(os: Component?, device: Device?, browserFamily: String?): String? {
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

/**
 * [UserAgentBrowserTypes]'s generate-direction contribution: the four MVP
 * per-family templates (Chrome/Firefox/Safari/Edge), each folding in an OS
 * (and, for Android, device) token via [generateOsToken]. Returns `null`
 * when `info.browser` is absent, isn't one of the four recognized families,
 * or (after the Chrome/Firefox Blink/Gecko-engine-version fallback) has no
 * usable version.
 */
internal fun generateBrowserSegment(info: UserAgentInfo): String? {
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
                return "$userAgentBase $osPrefix" + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$bv Safari/537.36"
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
                return "$userAgentBase $paren Gecko/20100101 Firefox/$bv"
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
                return "$userAgentBase $osPrefix" + "AppleWebKit/$ev (KHTML, like Gecko) Version/$bv Safari/$ev"
            }
        }
        "Edge" -> {
            val bv = browser.version
            if (!bv.isNullOrBlank()) {
                val ev = engine?.version?.takeIf { it.isNotBlank() } ?: bv
                val osToken = generateOsToken(info.os, info.device, browserFamily = "Edge")
                val osPrefix = if (osToken != null) "($osToken) " else ""
                return "$userAgentBase $osPrefix" +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ev Safari/537.36 Edg/$bv"
            }
        }
    }
    return null
}

/**
 * [UserAgentEngineTypes]'s generate-direction contribution: an engine-only
 * token (`rv:`/`AppleWebKit/`/`Trident/`/`Presto/`) plus an OS prefix, used
 * when no recognized browser drove the string. Returns `null` when
 * `info.engine` is absent, has no usable version, or isn't one of the four
 * recognized engine names -- in which case [generateFullUserAgentString]
 * falls through to [generateOsSegment] instead, matching this story's
 * pre-pack behavior of silently dropping an unrecognized engine.
 */
internal fun generateEngineSegment(info: UserAgentInfo): String? {
    val osToken = generateOsToken(info.os, info.device, browserFamily = null)
    val osPrefix = if (osToken != null) " ($osToken)" else ""
    val engine = info.engine ?: return null
    val ev = engine.version?.takeIf { it.isNotBlank() } ?: return null
    return when (engine.name) {
        "Gecko" -> "$userAgentBase$osPrefix Gecko/20100101 rv:$ev"
        "WebKit" -> "$userAgentBase$osPrefix AppleWebKit/$ev (KHTML, like Gecko)"
        "Trident" -> "$userAgentBase$osPrefix Trident/$ev"
        "Presto" -> "$userAgentBase$osPrefix Presto/$ev"
        else -> null
    }
}

/**
 * [UserAgentOsTypes]'s generate-direction contribution: the bare base string
 * with just an OS parenthetical, used when neither a recognized browser nor
 * a recognized engine drove the string. Returns `null` (rather than the bare
 * base) when there's no OS token either, so the outer [UserAgentGenerator]
 * composition still falls back to a single canonical [userAgentBase] result.
 */
internal fun generateOsSegment(info: UserAgentInfo): String? {
    val osToken = generateOsToken(info.os, info.device, browserFamily = null) ?: return null
    return "$userAgentBase ($osToken)"
}

/**
 * [UserAgentAllTypes]'s generate-direction contribution, and the exact
 * behavior of this library's pre-pack `UserAgentGenerator.generate`: try the
 * recognized-browser template first, then the engine-only fallback, then a
 * bare-base-plus-OS-token fallback, then finally the unadorned base string.
 */
internal fun generateFullUserAgentString(info: UserAgentInfo): String {
    generateBrowserSegment(info)?.let { return it }
    generateEngineSegment(info)?.let { return it }
    return generateOsSegment(info) ?: userAgentBase
}
