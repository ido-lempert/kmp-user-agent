import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinNpmPublish)
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

    private fun sourceHeader(sourceSection: String): String = buildString {
        appendLine("// Generated by the :library:generateUserAgentRules Gradle task. Do not edit by hand.")
        appendLine(
            "// Source: vendor/uap-core/regexes.yaml ($sourceSection section), Apache-2.0 -- see NOTICE.",
        )
        appendLine("package site.lempert.useragent.generated")
        appendLine()
    }

    // Each rule table is generated into its own file (browser/os/device), each
    // with its own top-level `internal val`, rather than one shared file with
    // three top-level vals. This matters beyond organization: Kotlin/JS groups
    // all top-level property initializers *in the same file* behind one shared
    // lazy-init gate, so referencing any one of them (e.g. `browserRules`, via
    // `UserAgentBrowserTypes`) would eagerly initialize the other two as well
    // (`osRules`/`deviceRules`) even when a consumer only imports the browser
    // pack -- silently defeating the whole point of Story 4.1's per-pack
    // tree-shaking (confirmed empirically: bundling `UserAgentBrowserTypes`
    // alone with esbuild still pulled in device-rule code when all three
    // tables lived in one generated file). One file per table gives each its
    // own independent init gate.
    fun generateBrowserRulesSource(browserRules: List<BrowserRule>): String = buildString {
        append(sourceHeader("user_agent_parsers"))
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
    }

    fun generateOsRulesSource(osRules: List<OsRule>): String = buildString {
        append(sourceHeader("os_parsers"))
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
    }

    fun generateDeviceRulesSource(deviceRules: List<DeviceRule>): String = buildString {
        append(sourceHeader("device_parsers"))
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

        val outputDir = outputDirectory.get().asFile
        // Story 4.1 switched this task from writing one combined
        // UserAgentRuleTable.kt to three separate files. This task isn't a
        // `Sync`-style task, so a rerun doesn't automatically prune files an
        // earlier run wrote that the current run no longer produces --
        // deleting the directory first prevents a stale UserAgentRuleTable.kt
        // (from a build/ directory that predates this change) from sitting
        // alongside the new files and causing duplicate `browserRules`/
        // `osRules`/`deviceRules` declarations.
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        // One file per rule table -- see the comment on UapCoreCodegen's
        // generate*RulesSource functions for why this (not one shared file)
        // is required for per-pack JS tree-shaking.
        File(outputDir, "UserAgentBrowserRuleTable.kt")
            .writeText(UapCoreCodegen.generateBrowserRulesSource(browserRules))
        File(outputDir, "UserAgentOsRuleTable.kt")
            .writeText(UapCoreCodegen.generateOsRulesSource(osRules))
        File(outputDir, "UserAgentDeviceRuleTable.kt")
            .writeText(UapCoreCodegen.generateDeviceRulesSource(deviceRules))
    }
}

// =============================================================================
// NOTICE + LICENSE bundling: ship the root MIT LICENSE and the vendored
// uap-core Apache-2.0 attribution (NOTICE) inside the published JVM and
// Android artifacts under META-INF/.
//
// There's no single Gradle mechanism that reaches both the JVM `Jar` task and
// the Android AAR's resource merging, so this copies the two canonical files
// (repo-root `LICENSE`, `library/NOTICE`) into two build-generated
// directories and registers each as an extra `resources` source directory on
// the relevant KMP source set -- `processJvmMainResources`/`jvmJar` picks up
// the JVM one, and `mergeAndroidMainJavaResource` (which feeds the published
// AAR) picks up the Android one. The iOS klib/framework artifact
// intentionally does not embed either file: embedding text files inside a
// compiled Kotlin/Native binary isn't standard practice, so iOS consumers
// instead get both via the published POM's SCM/license metadata pointing
// back to this source repo (same reasoning applies to the JS target's own
// Maven publication, distinct from the npm package Story 3.2 covers).
// =============================================================================

// Declared as a function (not a top-level `val`) on purpose: a top-level
// `val` in a `.gradle.kts` script is a property of the script's own class,
// and referencing it from a task's `doLast` would capture the whole
// (unserializable) script object for the configuration cache -- same pitfall
// documented on `UapCoreCodegen` above, same fix (keep it out of any closure
// captured by a task action; call this to get a fresh, capture-safe value
// inside each task's own configuration block instead).
fun rootLicenseFile(): org.gradle.api.file.RegularFile = rootProject.layout.projectDirectory.file("LICENSE")

val prepareNoticeForJvm = tasks.register("prepareNoticeForJvm") {
    val noticeFile = layout.projectDirectory.file("NOTICE")
    val licenseFile = rootLicenseFile()
    val outputDirectory = layout.buildDirectory.dir("generated/notice/jvmMain")

    description = "Copies the library's LICENSE/NOTICE files into META-INF/ for the published JVM jar."
    group = "publishing"

    inputs.file(noticeFile)
    inputs.file(licenseFile)
    outputs.dir(outputDirectory)

    doLast {
        val metaInfDir = File(outputDirectory.get().asFile, "META-INF")
        metaInfDir.mkdirs()
        noticeFile.asFile.copyTo(File(metaInfDir, "NOTICE"), overwrite = true)
        licenseFile.asFile.copyTo(File(metaInfDir, "LICENSE"), overwrite = true)
    }
}

val prepareNoticeForAndroid = tasks.register("prepareNoticeForAndroid") {
    val noticeFile = layout.projectDirectory.file("NOTICE")
    val licenseFile = rootLicenseFile()
    val outputDirectory = layout.buildDirectory.dir("generated/notice/androidMain")

    description = "Copies the library's LICENSE/NOTICE files into META-INF/ for the published Android AAR."
    group = "publishing"

    inputs.file(noticeFile)
    inputs.file(licenseFile)
    outputs.dir(outputDirectory)

    doLast {
        val metaInfDir = File(outputDirectory.get().asFile, "META-INF")
        metaInfDir.mkdirs()
        noticeFile.asFile.copyTo(File(metaInfDir, "NOTICE"), overwrite = true)
        licenseFile.asFile.copyTo(File(metaInfDir, "LICENSE"), overwrite = true)
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

        // AGP's default packaging excludes `META-INF/NOTICE` (and `NOTICE.txt`)
        // from merged Java resources, on the assumption it's noise from a
        // dependency. Here it's this library's own required Apache-2.0
        // attribution (AD-6) for the vendored uap-core data, so it must be
        // un-excluded to actually reach the published AAR.
        packaging {
            resources {
                excludes -= "/META-INF/NOTICE"
            }
        }
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
        jvmMain {
            resources.srcDir(prepareNoticeForJvm)
        }
        androidMain {
            resources.srcDir(prepareNoticeForAndroid)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateUserAgentRules)
}

// Safety net alongside the `resources.srcDir(...)` wiring above (mirrors the
// belt-and-suspenders pattern used for `generateUserAgentRules` above). These
// task names are created lazily by the Kotlin/Android Gradle plugins (in
// some cases only once the Android variant is fully configured), so this
// uses `tasks.configureEach` -- which matches by name as each task is
// registered, whenever that happens -- rather than `tasks.named`, which
// requires the task to already be registered at the point it's called.
tasks.configureEach {
    if (name == "processJvmMainResources") {
        dependsOn(prepareNoticeForJvm)
    }
    if (name == "mergeAndroidMainJavaResource") {
        dependsOn(prepareNoticeForAndroid)
    }
}

// Verified empirically: `NOTICE` reaches the intermediate merged-resources
// jar via the wiring above, but the `packaging.resources.excludes -=` in the
// `android { }` block is *not* sufficient on its own -- unzipping the actual
// produced `library/build/outputs/aar/library.aar` showed `NOTICE` missing.
// `com.android.kotlin.multiplatform.library`'s final AAR-bundling step
// doesn't fully respect that DSL the way the traditional `com.android.library`
// plugin does. `bundleAndroidMainAar` is itself a `Zip` task, so add `NOTICE`
// directly into its output as a second, independent path to the same
// end result -- confirmed fixed by re-unzipping the AAR after this was added.
tasks.withType<Zip>().configureEach {
    if (name == "bundleAndroidMainAar") {
        from(files(layout.projectDirectory.file("NOTICE"), rootLicenseFile())) {
            into("META-INF")
        }
    }
}

// =============================================================================
// Maven Central publishing (com.vanniktech.maven.publish)
//
// Central Portal host, GPG signing of all publications (signAllPublications()
// reads signingInMemoryKey/signingInMemoryKeyPassword/signingInMemoryKeyId
// from Gradle properties/env -- ORG_GRADLE_PROJECT_* -- automatically; no
// credential is hardcoded here). This module only configures the plugin;
// this story's verification is `publishToMavenLocal` only -- see the
// Boundaries in spec-3-1-publish-the-library-to-maven-central.md for why an
// actual `publishToMavenCentral`/`publishAndReleaseToMavenCentral` is never
// run from an automated session.
// =============================================================================

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("site.lempert", "user-agent", "0.2.0")

    pom {
        name.set("user-agent")
        description.set(
            "A Kotlin Multiplatform library that parses and generates User-Agent strings " +
                "behind one common API for Android, iOS, JVM, and JS.",
        )
        url.set("https://github.com/ido-lempert/kmp-user-agent")

        licenses {
            license {
                name.set("MIT")
                url.set("https://spdx.org/licenses/MIT.html")
                // Maven POM's <distribution> means "how to obtain it" (repo|manual),
                // not a URL -- "repo" is correct here since it's fetched from the
                // Maven repository itself, same as the artifact.
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("ido-lempert")
                name.set("Ido Lempert")
                email.set("il.mrbit@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/ido-lempert/kmp-user-agent")
            // GitHub disabled unauthenticated git:// access in 2022; https:// is the
            // correct anonymous-read protocol now (developerConnection's ssh:// is
            // unaffected and stays as-is).
            connection.set("scm:git:https://github.com/ido-lempert/kmp-user-agent.git")
            developerConnection.set("scm:git:ssh://git@github.com/ido-lempert/kmp-user-agent.git")
        }
    }
}

// =============================================================================
// npm publishing (org.jetbrains.kotlin.npm-publish -- JetBrains' continuation
// of dev.petuska.npm.publish; its classes/DSL still live under the
// dev.petuska.npm.publish.* package even though the plugin id/coordinates are
// now JetBrains-branded, confirmed by inspecting the resolved plugin jar).
//
// Publishes the js target's dist output as the scoped npm package
// @lempert/user-agent (organization "lempert" + packageName "user-agent",
// both human-confirmed -- see spec-3-2-publish-the-js-package-to-npm.md).
// This module only configures the plugin; this story's verification is the
// local packaging task only -- an actual live publish to the npmjs registry
// is never run from an automated session (see that spec's Boundaries).
// =============================================================================

npmPublish {
    organization.set("lempert")

    registries {
        npmjs {
            authToken.set(providers.environmentVariable("NPM_TOKEN"))
            // npmjs.org now requires a one-time password for publishing on
            // accounts where the configured token isn't exempt from 2FA (e.g.
            // "Automation" tokens are increasingly restricted to staging-only
            // publishes under npm's 2026 policy changes). Pass a fresh code
            // from your authenticator at invocation time -- it's only valid
            // for a short window, so it can't be a static env var:
            //   ./gradlew :library:publishJsPackageToNpmjsRegistry -PnpmOtp=123456
            // Omitting -PnpmOtp leaves this unset, which is fine for every
            // other task (packJsPackage, build, etc.) that never reads it.
            otp.set(providers.gradleProperty("npmOtp"))
        }
    }

    packages {
        named("js") {
            packageName.set("user-agent")
            version.set("0.2.0")
            readme.set(rootProject.layout.projectDirectory.file("README.md"))

            // NOTICE/LICENSE aren't part of the js target's own dist output, so
            // add them explicitly -- same NOTICE/LICENSE-bundling reasoning as
            // the JVM/Android META-INF handling above, but this plugin exposes a
            // first-class `files` collection for exactly this (additive to
            // whatever dist files it already wires in for this JS target).
            files.from(layout.projectDirectory.file("NOTICE"), rootLicenseFile())

            packageJson {
                // The plugin auto-populates "main" from the js target's output
                // module, but not "types" -- set it explicitly so TypeScript
                // consumers pick up the .d.mts file generateTypeScriptDefinitions()
                // already produces (confirmed missing from package.json otherwise
                // by unpacking a locally packed tarball).
                types.set("library.d.mts")
                license.set("MIT")
                homepage.set("https://github.com/ido-lempert/kmp-user-agent")
                description.set(
                    "A Kotlin Multiplatform library that parses and generates User-Agent strings " +
                        "behind one common API for Android, iOS, JVM, and JS.",
                )
                repository {
                    type.set("git")
                    url.set("https://github.com/ido-lempert/kmp-user-agent.git")
                }
            }
        }
    }
}

// =============================================================================
// npm "staged publishing" (npm CLI 11.15.0+, May 2026) via `npm stage publish`.
//
// org.jetbrains.kotlin.npm-publish 3.7.0 -- the latest release as of writing --
// doesn't support this yet (confirmed by decompiling the plugin jar: its
// publish task has the "publish" subcommand hardcoded, no "stage" variant).
// This task bypasses the plugin for just this one action and calls the same
// Gradle-provisioned npm binary directly against the already-assembled
// package directory. Per npm's own docs, `npm stage publish` requires the
// package to already exist on the registry and does not itself need 2FA; a
// human must separately run `npm stage approve <stage-id>` (CLI or
// npmjs.com), which does require 2FA -- neither approval nor a first-ever
// publish of a brand-new package is done by this task.
// =============================================================================

val prepareNpmStagePublishConfig = tasks.register("prepareNpmStagePublishConfig") {
    val npmrcFile = layout.buildDirectory.file("npmStagePublish/.npmrc")
    description = "Writes an .npmrc with an \${NPM_TOKEN}-interpolated auth line for stagePublishJsPackage " +
        "(the literal placeholder is written, not the token itself -- npm resolves it from the " +
        "process's own environment at publish time)."
    outputs.file(npmrcFile)
    doLast {
        val file = npmrcFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText("//registry.npmjs.org/:_authToken=\${NPM_TOKEN}\n")
    }
}

tasks.register<Exec>("stagePublishJsPackage") {
    group = "publishing"
    description = "Runs 'npm stage publish' against the assembled js package (staging only -- does not " +
        "make it public; requires the package to already exist on the registry)."
    dependsOn("assembleJsPackage", prepareNpmStagePublishConfig, ":kotlinNpmInstall")

    // Captured as Providers (not resolved values) here, at configuration time,
    // so nothing unserializable from this script is captured for the
    // configuration cache -- same reasoning as rootLicenseFile() above.
    // npmPublish.npmBin's *value* is only resolvable once kotlinNodeJsSetup has
    // actually run, though, so the Provider itself must not be .get() until
    // execution time either -- hence resolving all three only inside doFirst,
    // after dependsOn above has guaranteed that ordering.
    val npmBinProvider = npmPublish.npmBin
    val packageDirProvider = layout.buildDirectory.dir("packages/js")
    val npmrcFileProvider = layout.buildDirectory.file("npmStagePublish/.npmrc")

    doFirst {
        workingDir = packageDirProvider.get().asFile
        // The Kotlin/JS toolchain's provisioned Node (v24.10.0) bundles npm
        // 11.6.1, older than the 11.15.0 that added `npm stage publish`
        // ("Unknown command: stage" confirmed by actually running it against
        // the bundled npm first). `npx` ships alongside npm in the same bin
        // directory and can fetch a current npm on the fly without mutating
        // the shared toolchain install or the system's own npm.
        val npmBinFile = npmBinProvider.get().asFile
        executable = File(npmBinFile.parentFile, "npm").absolutePath
        args(
            "publish", "--access=public",
            "--userconfig", npmrcFileProvider.get().asFile.absolutePath,
        )
    }
}
