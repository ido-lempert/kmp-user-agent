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
// See library/vendor/uap-core/regexes.yaml and library/NOTICE. The
// `user_agent_parsers`, `os_parsers`, and `device_parsers` sections are all
// consumed here. This is a hand-rolled parser rather than a YAML library
// dependency: uap-core's regexes.yaml has a very regular structure (a flat
// list of maps, each a single-line single-quoted `regex` scalar plus
// optional single-line single-quoted `*_replacement`/`regex_flag` scalars),
// so a small line-based parser is sufficient.
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

    data class OsRule(
        val pattern: String,
        val osReplacement: String?,
        val v1Replacement: String?,
        val v2Replacement: String?,
        val v3Replacement: String?,
    )

    data class DeviceRule(
        val pattern: String,
        val regexFlag: String?,
        val deviceReplacement: String?,
        val brandReplacement: String?,
        val modelReplacement: String?,
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

    /** Parses the `os_parsers:` section of a vendored uap-core `regexes.yaml`. */
    fun parseOsParsers(yamlText: String): List<OsRule> {
        val lines = yamlText.lines()
        val startIndex = lines.indexOfFirst { it.trim() == "os_parsers:" }
        require(startIndex >= 0) { "Could not find 'os_parsers:' section in regexes.yaml" }

        val rules = mutableListOf<OsRule>()
        var pattern: String? = null
        var os: String? = null
        var v1: String? = null
        var v2: String? = null
        var v3: String? = null

        fun flush() {
            val currentPattern = pattern
            if (currentPattern != null) {
                rules += OsRule(currentPattern, os, v1, v2, v3)
            }
            pattern = null
            os = null
            v1 = null
            v2 = null
            v3 = null
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
                trimmed.startsWith("os_replacement:") ->
                    os = extractSingleQuotedValue(line, "os_replacement:")
                trimmed.startsWith("os_v1_replacement:") ->
                    v1 = extractSingleQuotedValue(line, "os_v1_replacement:")
                trimmed.startsWith("os_v2_replacement:") ->
                    v2 = extractSingleQuotedValue(line, "os_v2_replacement:")
                trimmed.startsWith("os_v3_replacement:") ->
                    v3 = extractSingleQuotedValue(line, "os_v3_replacement:")
            }
        }
        flush()
        return rules
    }

    /** Parses the `device_parsers:` section of a vendored uap-core `regexes.yaml`. */
    fun parseDeviceParsers(yamlText: String): List<DeviceRule> {
        val lines = yamlText.lines()
        val startIndex = lines.indexOfFirst { it.trim() == "device_parsers:" }
        require(startIndex >= 0) { "Could not find 'device_parsers:' section in regexes.yaml" }

        val rules = mutableListOf<DeviceRule>()
        var pattern: String? = null
        var regexFlag: String? = null
        var device: String? = null
        var brand: String? = null
        var model: String? = null

        fun flush() {
            val currentPattern = pattern
            if (currentPattern != null) {
                rules += DeviceRule(currentPattern, regexFlag, device, brand, model)
            }
            pattern = null
            regexFlag = null
            device = null
            brand = null
            model = null
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
                trimmed.startsWith("regex_flag:") ->
                    regexFlag = extractSingleQuotedValue(line, "regex_flag:")
                trimmed.startsWith("device_replacement:") ->
                    device = extractSingleQuotedValue(line, "device_replacement:")
                trimmed.startsWith("brand_replacement:") ->
                    brand = extractSingleQuotedValue(line, "brand_replacement:")
                trimmed.startsWith("model_replacement:") ->
                    model = extractSingleQuotedValue(line, "model_replacement:")
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
        // `\-` is valid ECMAScript `u`-flag syntax *inside* a character class (where a bare
        // `-` would otherwise form a range) but not outside one, where a bare `-` is always
        // literal. Stripping it unconditionally -- as earlier versions of this function did --
        // is correct outside a class but corrupts the class's intended member set when the
        // hyphen isn't already at a class boundary (e.g. `[ \-\.]` becoming `[ -\.]`, which
        // silently widens to an unintended ascending range). Track class context so `\-` is
        // only stripped where doing so is safe.
        var insideCharacterClass = false
        while (index < pattern.length) {
            val current = pattern[index]
            if (current == '\\' && index + 1 < pattern.length) {
                val next = pattern[index + 1]
                if (next == '-' && insideCharacterClass) {
                    builder.append(current).append(next)
                } else if (next.isDigit() || next in allowedAfterBackslash) {
                    builder.append(current).append(next)
                } else {
                    // Not a recognized escape: drop the backslash, keep the literal character.
                    builder.append(next)
                }
                index += 2
            } else {
                if (current == '[') insideCharacterClass = true
                if (current == ']') insideCharacterClass = false
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

    fun generateSource(
        browserRules: List<BrowserRule>,
        osRules: List<OsRule>,
        deviceRules: List<DeviceRule>,
    ): String = buildString {
        appendLine("// Generated by the :library:generateUserAgentRules Gradle task. Do not edit by hand.")
        appendLine(
            "// Source: vendor/uap-core/regexes.yaml (user_agent_parsers/os_parsers/device_parsers sections), " +
                "Apache-2.0 -- see NOTICE.",
        )
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
        for (rule in browserRules) {
            val normalizedPattern = normalizePatternForAllTargets(rule.pattern)
            append("    BrowserRule(")
            append(kotlinStringLiteral(normalizedPattern)).append(", ")
            append(kotlinNullableStringLiteral(rule.familyReplacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v1Replacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v2Replacement))
            appendLine("),")
        }
        appendLine(")")
        appendLine()
        appendLine("internal class OsRule(")
        appendLine("    val pattern: String,")
        appendLine("    val osReplacement: String?,")
        appendLine("    val v1Replacement: String?,")
        appendLine("    val v2Replacement: String?,")
        appendLine("    val v3Replacement: String?,")
        appendLine(")")
        appendLine()
        appendLine("internal val osRules: List<OsRule> = listOf(")
        for (rule in osRules) {
            val normalizedPattern = normalizePatternForAllTargets(rule.pattern)
            append("    OsRule(")
            append(kotlinStringLiteral(normalizedPattern)).append(", ")
            append(kotlinNullableStringLiteral(rule.osReplacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v1Replacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v2Replacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.v3Replacement))
            appendLine("),")
        }
        appendLine(")")
        appendLine()
        appendLine("internal class DeviceRule(")
        appendLine("    val pattern: String,")
        appendLine("    val regexFlag: String?,")
        appendLine("    val deviceReplacement: String?,")
        appendLine("    val brandReplacement: String?,")
        appendLine("    val modelReplacement: String?,")
        appendLine(")")
        appendLine()
        appendLine("internal val deviceRules: List<DeviceRule> = listOf(")
        for (rule in deviceRules) {
            val normalizedPattern = normalizePatternForAllTargets(rule.pattern)
            append("    DeviceRule(")
            append(kotlinStringLiteral(normalizedPattern)).append(", ")
            append(kotlinNullableStringLiteral(rule.regexFlag)).append(", ")
            append(kotlinNullableStringLiteral(rule.deviceReplacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.brandReplacement)).append(", ")
            append(kotlinNullableStringLiteral(rule.modelReplacement))
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
        val yamlText = regexesFile.asFile.readText()
        val browserRules = UapCoreCodegen.parseUserAgentParsers(yamlText)
        val osRules = UapCoreCodegen.parseOsParsers(yamlText)
        val deviceRules = UapCoreCodegen.parseDeviceParsers(yamlText)
        val source = UapCoreCodegen.generateSource(browserRules, osRules, deviceRules)

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
        all {
            // @JsExport (on the public API in commonMain) is a no-op on every
            // target but JS; every source set compiling commonMain must opt in
            // to the same experimental annotation it does.
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
        }
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
