package site.lempert.useragent

import kotlin.js.JsExport

/**
 * A composable unit of User-Agent detection (parse direction) and/or
 * templating (generate direction).
 *
 * [detect] receives the raw User-Agent string and must return a *partially
 * populated* [UserAgentInfo]: only the field(s) this pack is responsible for
 * should be non-null/non-empty; every other field should be left at its
 * default (`null`/`emptyMap()`). [UserAgentParser] runs every pack passed to
 * it and merges their [detect] results field-by-field, taking the first
 * non-null (or, for [UserAgentInfo.custom], first-key-wins) value seen, in
 * the order the packs were given.
 *
 * [applyToGenerate] receives a fully-populated [UserAgentInfo] and may
 * return a complete User-Agent string built from whatever subset of fields
 * this pack knows how to render; `null` means "this pack has nothing to
 * contribute for this input". [UserAgentGenerator] tries each pack in order
 * and uses the first non-null result, falling back to the bare
 * [userAgentBase] string if every pack returns `null`. It defaults to
 * `{ null }` for packs that only contribute to parsing.
 *
 * This constructor is public (not `internal`) specifically so consumers can
 * build their own packs -- following this same shape -- to add detection or
 * generation categories without forking the library; JS/npm consumers must
 * be able to construct one too.
 *
 * A plain class with no top-level companion state on purpose: this type is
 * shared by every built-in pack file (see `UserAgentBrowserTypePack.kt` and
 * its siblings), and a class declaration alone -- unlike a top-level `val`
 * -- carries no Kotlin/JS lazy-initialization gate, so referencing this
 * class doesn't force-initialize any pack that isn't otherwise reachable.
 */
@JsExport
class UserAgentTypePack(
    val id: String,
    val detect: (String) -> UserAgentInfo,
    val applyToGenerate: (UserAgentInfo) -> String? = { null },
)
