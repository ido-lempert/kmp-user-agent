package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Stateless factory returning a function that parses a raw User-Agent string
 * into structured data, composed from [packs].
 *
 * Passing no packs returns a function that always produces an empty
 * [UserAgentInfo] (every field `null`/empty) -- it does **not** fall back to
 * [UserAgentAllTypes]. Consumers who want full detection must pass
 * [UserAgentAllTypes] explicitly. (This is a deliberate change from this
 * story's first implementation pass: an implicit `UserAgentAllTypes`
 * fallback baked into this shared factory body gave *every* call site --
 * including `UserAgentParser(UserAgentBrowserTypes)`, the primary documented
 * pattern -- a static reachability edge to every built-in pack under
 * standard JS bundlers, which tree-shake at the top-level-binding level, not
 * per-branch. Measured via a real `npm pack` + esbuild/Terser build: with
 * that fallback in place, `UserAgentParser(UserAgentBrowserTypes)(ua)` and
 * `UserAgentParser(UserAgentAllTypes)(ua)` bundled to within 30 bytes of
 * each other, defeating this story's whole purpose. See the spec's Spec
 * Change Log for the full account.)
 *
 * When multiple packs are passed, the returned function runs every pack's
 * [UserAgentTypePack.detect] against the input and merges the results
 * field-by-field: the first pack (in the order given) to produce a non-null
 * value for a field wins; [UserAgentInfo.custom] entries merge by key with
 * the same first-pack-wins rule per key.
 *
 * The returned function never throws: unrecognized input (including an
 * empty string), or an exception thrown by a (possibly third-party) pack's
 * `detect`, simply results in that field/pack contributing nothing rather
 * than propagating.
 *
 * `@JsExport` makes this function visible to JS/TS consumers; it's a no-op
 * on every other target and changes no parsing behavior.
 */
@JsExport
fun UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo {
    return { userAgent ->
        var browser: Component? = null
        var engine: Component? = null
        var os: Component? = null
        var device: Device? = null
        var bot: Component? = null
        var aiAgent: Component? = null
        var custom: Map<String, Component> = emptyMap()

        for (pack in packs) {
            val partial = try {
                pack.detect(userAgent)
            } catch (_: Throwable) {
                null
            } ?: continue

            if (browser == null) browser = partial.browser
            if (engine == null) engine = partial.engine
            if (os == null) os = partial.os
            if (device == null) device = partial.device
            if (bot == null) bot = partial.bot
            if (aiAgent == null) aiAgent = partial.aiAgent
            if (partial.custom.isNotEmpty()) {
                for ((key, value) in partial.custom) {
                    if (key !in custom) custom = custom + (key to value)
                }
            }
        }

        UserAgentInfo(
            browser = browser,
            engine = engine,
            os = os,
            device = device,
            bot = bot,
            aiAgent = aiAgent,
            custom = custom,
        )
    }
}
