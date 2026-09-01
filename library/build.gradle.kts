import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

// =============================================================================
// Build-time code generation: vendor uap-core's regexes.yaml -> commonMain Kotlin
//
// See library/vendor/uap-core/regexes.yaml and library/NOTICE. Only the
// `user_agent_parsers` section is consumed by this story; `os_parsers` and
// `device_parsers` are added by later stories. This is a hand-rolled parser
// rather than a YAML library dependency: uap-core's regexes.yaml has a very
// regular structure (a flat list of maps, each a single-line single-quoted
// `regex` scalar plus optional single-line single-quoted `*_replacement`
// scalars), so a small line-based parser is sufficient.
// =============================================================================

// Declared as a top-level `object` (not top-level `fun`s) on purpose: top-level
// functions in a `.gradle.kts` script compile as members of the script's own
// class, so a task action referencing them captures the whole script object,
// which the configuration cache refuses to serialize. A plain object has no
// such dependency on the enclosing script instance.
object UapCoreCodegen {

    data class BrowserRule(
        val pattern: String,
        val familyReplacement: String?,
        val v1Replacement: String?,
        val v2Replacement: String?,
    )

    private fun unescapeYamlSingleQuoted(value: String): String = value.replace("''", "'")

    /** Extracts the single-quoted scalar value that follows [key] on [line], e.g. `key: 'value'`. */
    private fun extractSingleQuotedValue(line: String, key: String): String? {
        val keyIndex = line.indexOf(key)
        if (keyIndex < 0) return null
        val rest = line.substring(keyIndex + key.length)
        val firstQuote = rest.indexOf('\'')
        val lastQuote = rest.lastIndexOf('\'')
        if (firstQuote < 0 || lastQuote <= firstQuote) return null
        return unescapeYamlSingleQuoted(rest.substring(firstQuote + 1, lastQuote))
    }

    /** Parses the `user_agent_parsers:` section of a vendored uap-core `regexes.yaml`. */
    fun parseUserAgentParsers(yamlText: String): List<BrowserRule> {
        val lines = yamlText.lines()
        val startIndex = lines.indexOfFirst { it.trim() == "user_agent_parsers:" }
        require(startIndex >= 0) { "Could not find 'user_agent_parsers:' section in regexes.yaml" }

        val rules = mutableListOf<BrowserRule>()
        var pattern: String? = null
        var family: String? = null
        var v1: String? = null
        var v2: String? = null

        fun flush() {
            val currentPattern = pattern
            if (currentPattern != null) {
                rules += BrowserRule(currentPattern, family, v1, v2)
            }
            pattern = null
            family = null
            v1 = null
            v2 = null
        }

        for (index in (startIndex + 1) until lines.size) {
            val line = lines[index]
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            // A subsequent top-level `key:` (no leading whitespace) ends this section.
            if (!line.startsWith(" ") && !line.startsWith("\t")) break

            when {
                trimmed.startsWith("- regex:") -> {
                    flush()
                    pattern = extractSingleQuotedValue(line, "regex:")
                }
                trimmed.startsWith("family_replacement:") ->
                    family = extractSingleQuotedValue(line, "family_replacement:")
                trimmed.startsWith("v1_replacement:") ->
                    v1 = extractSingleQuotedValue(line, "v1_replacement:")
                trimmed.startsWith("v2_replacement:") ->
                    v2 = extractSingleQuotedValue(line, "v2_replacement:")
            }
        }
        flush()
        return rules
    }

    /**
     * Kotlin/JS's `Regex` hardcodes the ECMAScript `u` (unicode-mode) flag, under
     * which a backslash may only escape a recognized regex metacharacter or a
     * "syntax character" (`^ $ . * + ? ( ) [ ] { } | /` and `\` itself) --
     * anything else (e.g. `\-`, `\!`, `\ `) throws `SyntaxError`, even though
     * JVM/Native accept it as a literal character. This strips the backslash from
     * such non-metacharacter escapes uniformly for every target; since JVM/Native
     * already treat e.g. `\-` as literal `-`, stripping the backslash there is a
     * semantics-preserving no-op.
     */
    fun normalizePatternForAllTargets(pattern: String): String {
        val allowedAfterBackslash = setOf(
            'd', 'D', 's', 'S', 'w', 'W', 'b', 'B', 'n', 'r', 't', 'f', 'v', '0',
            'x', 'u', 'c', 'k', 'p', 'P',
            '^', '$', '.', '*', '+', '?', '(', ')', '[', ']', '{', '}', '|', '/', '\\',
        )
        val builder = StringBuilder(pattern.length)
        var index = 0
        while (index < pattern.length) {
            val current = pattern[index]
            if (current == '\\' && index + 1 < pattern.length) {
                val next = pattern[index + 1]
                if (next.isDigit() || next in allowedAfterBackslash) {
                    builder.append(current).append(next)
                } else {
                    // Not a recognized escape: drop the backslash, keep the literal character.
                    builder.append(next)
                }
                index += 2
            } else {
                builder.append(current)
                index += 1
            }
        }
        return builder.toString()
    }

    /** Renders [value] as a Kotlin double-quoted string literal (non-raw, so `\` stays literal). */
    fun kotlinStringLiteral(value: String): String {
        val builder = StringBuilder(value.length + 2)
        builder.append('"')
        for (c in value) {
            when (c) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '$' -> builder.append("\\$")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(c)
            }
        }
        builder.append('"')
        return builder.toString()
    }

    fun kotlinNullableStringLiteral(value: String?): String =
        if (value == null) "null" else kotlinStringLiteral(value)

    fun generateSource(rules: List<BrowserRule>): String = buildString {
        appendLine("// Generated by the :library:generateUserAgentRules Gradle task. Do not edit by hand.")
        appendLine("// Source: vendor/uap-core/regexes.yaml (user_agent_parsers section), Apache-2.0 -- see NOTICE.")
        appendLine("package site.lempert.useragent.generated")
        appendLine()
        appendLine("internal class BrowserRule(")
        appendLine("    val pattern: String,")
        appendLine("    val familyReplacement: String?,")
        appendLine("    val v1Replacement: String?,")
        appendLine("    val v2Replacement: String?,")
        appendLine(")")
        appendLine()
        appendLine("internal val browserRules: List<BrowserRule> = listOf(")
        for (rule in rules) {
            val normalizedPattern = normalizePatternForAllTargets(rule.pattern)
            append("    BrowserRule(")
            append(kotlinStringLiteral(normalizedPattern)).append(", ")
            append(kotlinNullableStringLiteral(rule.familyReplacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v1Replacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v2Replacement))
            appendLine("),")
        }
        appendLine(")")
    }
}

val generateUserAgentRules = tasks.register("generateUserAgentRules") {
    // Declared as local `val`s of this task-configuration lambda, not as
    // top-level script `val`s: a top-level `val` in a `.gradle.kts` script is
    // a property of the script's own class, and referencing it from `doLast`
    // would capture the whole (unserializable) script object for the
    // configuration cache -- see the note on `UapCoreCodegen` above.
    val regexesFile = layout.projectDirectory.file("vendor/uap-core/regexes.yaml")
    val outputDirectory = layout.buildDirectory.dir("generated/userAgentRules/kotlin")

    description = "Generates the commonMain browser detection rule table from vendored uap-core data."
    group = "code generation"

    inputs.file(regexesFile)
    outputs.dir(outputDirectory)

    doLast {
        val rules = UapCoreCodegen.parseUserAgentParsers(regexesFile.asFile.readText())
        val source = UapCoreCodegen.generateSource(rules)

        val outputDir = outputDirectory.get().asFile
        outputDir.mkdirs()
        File(outputDir, "UserAgentRuleTable.kt").writeText(source)
    }
}

// =============================================================================
// KMP module configuration
// =============================================================================

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Library"
            isStatic = true
        }
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    js {
        outputModuleName = "library"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }

    android {
        namespace = "site.lempert.useragent"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {}
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateUserAgentRules)
            dependencies {
                // Production code depends only on the Kotlin stdlib.
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateUserAgentRules)
}
