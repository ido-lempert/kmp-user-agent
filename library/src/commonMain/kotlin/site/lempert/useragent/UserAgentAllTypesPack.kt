package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Convenience bundle of every built-in detection category shipped today:
 * browser, engine, OS, device, bot, and AI agent. Consumers who want full
 * detection/generation must pass this pack explicitly -- [UserAgentParser]/
 * [UserAgentGenerator] do **not** fall back to it when called with no packs
 * (see their doc comments for why: an implicit fallback would defeat this
 * story's JS tree-shaking goal for every call site, not just the no-args
 * one).
 *
 * Deliberately kept in its own file, separate from the six narrower
 * built-in packs: referencing this pack legitimately needs everything (all
 * six detectors and the full generate algorithm), but it must not force
 * *those* packs' files to be eagerly initialized just because they happen
 * to share a file with this one -- see [UserAgentBrowserTypes]'s doc
 * comment for the full explanation of why file separation matters here.
 */
@JsExport
val UserAgentAllTypes: UserAgentTypePack = UserAgentTypePack(
    id = "all",
    detect = { userAgent ->
        UserAgentInfo(
            browser = detectBrowser(userAgent),
            engine = detectEngine(userAgent),
            os = detectOs(userAgent),
            device = detectDevice(userAgent),
            bot = detectBot(userAgent),
            aiAgent = detectAiAgent(userAgent),
        )
    },
    applyToGenerate = ::generateFullUserAgentString,
)
