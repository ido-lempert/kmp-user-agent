package site.lempert.useragent

/**
 * Shared regex-template-substitution helpers used by the browser, OS, and
 * device detectors ([UserAgentBrowserTypes]/[UserAgentOsTypes]/
 * [UserAgentDeviceTypes]'s `detect` implementations). Deliberately kept as
 * plain top-level functions (not classes/objects with any state): unlike a
 * top-level `val`, a plain function carries no lazy-initialization gate in
 * Kotlin/JS's compiled output, so sharing these across the per-pack files
 * doesn't undermine those files' independent tree-shakeability -- see the
 * comment on `UapCoreCodegen.generateBrowserRulesSource` in
 * `library/build.gradle.kts` for the fuller story on why per-pack file
 * separation matters here.
 */

/** Substitutes `$1`, `$2`, ... in a uap-core replacement template with match groups. */
internal fun applyGroupReplacement(template: String, match: MatchResult): String? {
    if ('$' !in template) return template
    val result = buildString {
        var index = 0
        while (index < template.length) {
            val c = template[index]
            if (c == '$' && index + 1 < template.length && template[index + 1].isDigit()) {
                var digitsEnd = index + 1
                while (digitsEnd < template.length && template[digitsEnd].isDigit()) digitsEnd++
                val groupIndex = template.substring(index + 1, digitsEnd).toIntOrNull()
                if (groupIndex != null) {
                    append(groupValueOrNull(match, groupIndex).orEmpty())
                } else {
                    // Digit run too large to fit an Int (never happens in practice for
                    // vendored data, but the returned parse function must never throw
                    // regardless of input).
                    append(template, index, digitsEnd)
                }
                index = digitsEnd
            } else {
                append(c)
                index += 1
            }
        }
    }
    return result.trim().ifEmpty { null }
}

internal fun groupValueOrNull(match: MatchResult, index: Int): String? = try {
    match.groups[index]?.value
} catch (_: Throwable) {
    null
}
