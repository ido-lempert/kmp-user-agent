- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-parse-device-info-from-a-user-agent-string.md`
  summary: `regexFlag`/`RegexOption.IGNORE_CASE` handling (added by Story 1.3) only special-cases the literal value `"i"`; any other `regex_flag` value would silently compile with no flags at all rather than failing loudly.
  evidence: Confirmed the only value present anywhere in the currently vendored `regexes.yaml` (all three sections) is `'i'` (65 occurrences, all in `device_parsers`), so this isn't demonstrable as a live bug today — but a future re-vendor introducing a different flag value would silently change matching semantics with no build-time or test signal. (This entry originally tracked "regex_flag isn't implemented at all," which Story 1.3 resolved.)

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: The hand-rolled YAML parsers in `library/build.gradle.kts` (`UapCoreCodegen.parseUserAgentParsers`/`parseOsParsers`/`parseDeviceParsers`/`extractSingleQuotedValue`) assume every scalar is single-quoted on one line, have no fallback for double-quoted values or an inline comment after the closing quote, and don't sanity-check that a plausible number of rules were parsed (e.g. by comparing the parsed count to the number of `- regex:` lines in the source section).
  evidence: Not demonstrated against the currently vendored file (which is well-formed and yields 434 browser rules, 204 OS rules, and 633 device rules), but a future re-vendor from upstream could introduce a differently formatted line and silently produce a truncated or near-empty rule table with no build-time signal. Story 1.3 completed the third near-identical section-parsing loop (predicted in this note after Story 1.2) — now a good point to extract a shared helper rather than duplicating a fourth time.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: `UserAgentParser` catches `Throwable` in every rule-compiling/matching site (browser, OS, device, engine), which also swallows `OutOfMemoryError`/`StackOverflowError` from pathological regex backtracking instead of surfacing them.
  evidence: Intentional today per the spec's "parse() never throws" requirement, but broad `Throwable` catches mask non-recoverable JVM errors rather than just expected regex-compile/match failures.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: `UserAgentParser.parse()` has no length cap or timeout guard before running an untrusted input string through hundreds of vendored regexes sequentially.
  evidence: Not required by this story's acceptance criteria, but `parse()` is a public API intended to eventually process real HTTP `User-Agent` headers, which are attacker-controlled; uap-core-derived rule sets have a history of catastrophic-backtracking patterns. Story 1.2 added a second full linear scan (`os_parsers`, 204 rules) alongside the browser scan; Story 1.3 added a third (`device_parsers`, 633 rules) — `parse()` now runs well over 1,000 regexes per call in the worst case.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-parse-device-info-from-a-user-agent-string.md`
  summary: `detectDevice` can in principle return a non-null `Device(brand = null, model = null, name = null)` if a matching rule's replacement fields all resolve to null (e.g. an optional capture group didn't participate in the match), which is indistinguishable from "no device detected" but is surfaced as a match rather than falling through to the next rule.
  evidence: Not demonstrable against the current vendored data — confirmed via a script over all 633 `device_parsers` rules that none omits all three replacement fields — but worth a defensive `continue` if it's ever observed, mirroring how `detectBrowser`/`detectOs` already `continue` when their (mandatory) name field can't be resolved.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-parse-device-info-from-a-user-agent-string.md`
  summary: A malformed `- regex:` line in any of the three vendored sections (one whose value fails single-quote extraction) is silently dropped from the generated rule table with no build-time warning, rather than failing the codegen task loudly.
  evidence: Not demonstrated against the current vendored file (well-formed), but a future re-vendor with an unexpected line format would silently shrink a rule table instead of surfacing the problem.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: Version composition (`listOfNotNull(v1, v2, v3).joinToString(".")` in both `detectBrowser` and `detectOs`) doesn't special-case a null segment followed by a non-null one (e.g. `v1 == null && v2 != null`), which would render the later segment(s) alone as if they were a complete version from position 1.
  evidence: Not demonstrated against any currently vendored rule's actual capture-group ordering in either `user_agent_parsers` or `os_parsers`; theoretical edge case worth a regression test if it's ever observed in practice.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: There is no documented process or script for refreshing the vendored `vendor/uap-core/regexes.yaml` snapshot, beyond recording the pinned commit SHA in `library/NOTICE`.
  evidence: Maintaining a security/compatibility-relevant third-party dataset by hand invites silent drift between the pinned commit and the actual vendored content.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: Consider enabling Kotlin's `explicitApi()` mode on the `library` module to compiler-enforce the intended public API surface (`UserAgentInfo`, `Component`, `Device`, `UserAgentParser`, now also `UserAgentGenerator`) as the module grows in later stories.
  evidence: Not required for Story 1.1, which has a small, already-correct public surface, but becomes more valuable as more stories add code to `commonMain`.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: `UserAgentGenerator`'s four family templates only recognize exact-match names `"Chrome"`/`"Firefox"`/`"Safari"`/`"Edge"`; real-world `UserAgentParser` output for variant families (e.g. `"Chrome Mobile"`, `"Mobile Safari"`, `"Chromium"`, `"CriOS"`, `"Edge Mobile"`) silently falls through to the bare base string, dropping the browser segment entirely.
  evidence: Consistent with this story's stated v1 scope (the same four representative desktop families used throughout parsing), and not demonstrated as a defect since generate never claims to cover mobile-variant families — but worth widening once real-world round-trip coverage (Story 2.3 and beyond) surfaces which variants matter most.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: `UserAgentGenerator.generate` does not sanitize `browser.version`/`engine.version` content — a version string containing delimiter or structural characters (spaces, slashes, parentheses, newlines) would splice unintended tokens into the generated UA string or fail to round-trip through `UserAgentParser.parse` (whose version-capturing regexes are digit/dot-only).
  evidence: Not reachable via any real `UserAgentParser.parse()` output (which only ever produces digit/dot version strings), so only exploitable via a directly hand-constructed `UserAgentInfo` with malformed data; the API's own callers are trusted (not attacker-controlled HTTP input the way `parse()`'s UA string is), so this is low priority for v1.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: `UserAgentGenerator`'s Windows OS-token support (added by Story 2.2) recognizes only the exact version `"10"`; Windows 11 (which reports the same `"Windows NT 10.0"` UA convention as Windows 10 in real browsers) and older versions (8.1/7/XP/Vista) produce no OS token at all.
  evidence: Consistent with the same "four representative families, pragmatic v1 coverage" scope already established for browsers; not a defect, but worth widening once real-world usage shows which Windows versions matter most for generation.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: `UserAgentGenerator.generate`'s existing lack of content sanitization for `browser.version`/`engine.version` (already deferred) extends to `device.model` (added by Story 2.2) — a model string containing `;`, `(`, or `)` would corrupt the generated OS parenthetical's structure.
  evidence: Same trust boundary as the existing entry — not reachable via real `UserAgentParser.parse()` output, only via a directly hand-constructed `UserAgentInfo`/`Device`; low priority for v1.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-2-generate-a-user-agent-string-including-os-device-data.md`
  summary: Generating `os = Component("Android", version)` with `device = null` produces a token (e.g. `"Linux; Android 12"`) that still contains the bare word "Android", which a vendored `device_parsers` catch-all rule matches, incidentally producing `Device("Generic", "Smartphone", "Generic Smartphone")` on parse-back even though `device` was `null` in the original input.
  evidence: Mirrors the already-intentional "Mac/iPhone incidentally recover `device`" behavior this story documents, but wasn't itself documented or asserted for the Android-without-device case; confirmed via review, not yet demonstrated as harmful (the AC only requires `device` to survive round-trip when it was actually supplied), but worth an explicit test/doc note if it ever needs to change.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-validate-parsing-across-all-four-targets.md`
  summary: `.github/workflows/ci.yml` runs everything in a single job with no per-target matrix, so a failure on one of the four MVP targets (jvmTest/testAndroidHostTest/jsTest/iosSimulatorArm64Test) shows up buried in one combined log rather than being individually attributed.
  evidence: Matches this story's frozen "single job" boundary; worth splitting into a matrix once CI run time or failure-triage friction actually becomes a problem in practice.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-validate-parsing-across-all-four-targets.md`
  summary: No JDK is explicitly pinned in `.github/workflows/ci.yml` (no `actions/setup-java`) — the build relies entirely on `gradle/gradle-daemon-jvm.properties`' toolchain auto-provisioning (Azul JDK 21 via the foojay resolver) to fetch a working JDK on a fresh runner.
  evidence: Should work (this is exactly what that file is for), but hasn't been proven on an actual GitHub Actions runner yet since this session only verified locally and hasn't pushed; worth confirming on the first real CI run and adding an explicit `actions/setup-java` step if auto-provisioning ever fails there.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-validate-parsing-across-all-four-targets.md`
  summary: The newly `@JsExport`ed public API (`UserAgentInfo`, `Component`, `Device`, `UserAgentParser`, `UserAgentGenerator`) has no KDoc beyond what already existed for Kotlin consumers, even though these declarations are now the literal contract JS/TypeScript consumers see directly.
  evidence: Not required for this story's scope (a thin sample-app harness), but worth adding once the JS/npm consumption story (Epic 3) makes this a real, published, externally-consumed API surface.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-validate-parsing-across-all-four-targets.md`
  summary: `jvmApp`'s `Main.kt` only exercises one hardcoded UA string and one hardcoded `UserAgentInfo` for generation, with no `args: Array<String>` support to let a caller point it at arbitrary input.
  evidence: Consistent with "thin harness, not a real app" scope for all four sample apps in this story; worth adding if the sample apps are ever used for more than a one-glance proof-of-consumption check.

- source_spec: `_bmad-output/implementation-artifacts/spec-3-1-publish-the-library-to-maven-central.md`
  summary: The release version `"0.1.0"` is a hardcoded literal in `mavenPublishing { coordinates(...) }`, with no versioning strategy addressed for subsequent releases (e.g. `-SNAPSHOT` for non-tagged CI builds, or a single source of truth like a git tag or `gradle.properties` value feeding both the coordinate and a CHANGELOG).
  evidence: Fine for this story's single, human-confirmed v1 release; Maven Central rejects re-publishing an already-released version, so this needs a real strategy before a second release (0.2.0 etc.) is ever cut — worth addressing in whichever future story handles the next release. **Update, Story 4.2:** the predicted second release happened (0.1.0 → 0.2.0) and the version was again bumped by hand in the same two spots (`library/build.gradle.kts`'s `coordinates(...)` and `npmPublish { packages { named("js") { version.set(...) } } }`), still with no single source of truth and no automated check that the two stay in lockstep — review-surfaced (blind-hunter layer) as a real, now twice-repeated, still-unguarded gap.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-2-validate-type-pack-api-parity-across-all-four-targets-and-republish.md`
  summary: `CHANGELOG.md`'s `0.1.0` entry (added by this story, retroactively documenting the initial release) doesn't mention the Maven artifactId rename from `kmp-user-agent` to `user-agent` (commit `84eea6b`, 2026-09-02), which happened within that same pre-0.2.0 window — a reader who found the old coordinates has no note in the changelog explaining the rename.
  evidence: Review-surfaced (blind-hunter layer). Minor and historical; out of scope for a story whose job was documenting the 0.2.0 breaking change, not backfilling every pre-CHANGELOG repo event — worth a one-line addition to the 0.1.0 entry whenever someone next touches `CHANGELOG.md`.

- source_spec: `_bmad-output/implementation-artifacts/spec-3-1-publish-the-library-to-maven-central.md`
  summary: There is no automated regression check (e.g. a Gradle task that unzips the built jar/aar and asserts `META-INF/LICENSE`/`META-INF/NOTICE` are present) protecting the NOTICE/LICENSE-bundling wiring — this session verified it by manually unzipping the built artifacts, which offers no lasting protection against a future refactor quietly breaking it.
  evidence: The bundling mechanism itself needed a real fix during this story (the `packaging.resources.excludes` DSL alone wasn't sufficient for `com.android.kotlin.multiplatform.library`'s AAR output, confirmed by direct inspection) — precisely the kind of silent regression an automated check would catch early. Worth adding as a lightweight test once the publish pipeline stabilizes.

- source_spec: `_bmad-output/implementation-artifacts/spec-3-1-publish-the-library-to-maven-central.md`
  summary: No CI workflow exists for actually running a real Maven Central publish (tag-triggered or otherwise) — this story only wires up local configuration and verification (`publishToMavenLocal`); the live release step remains an entirely manual, untracked action run from the maintainer's own terminal.
  evidence: Intentional and out of scope for this story (see its frozen Boundaries — no live publish from an automated session, and the GPG signing key setup here requires an interactive passphrase prompt this session's tooling can't satisfy anyway); worth revisiting once a stable release cadence justifies automating it.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: Bot and AI-agent detection (the `UserAgentBotTypes`/`UserAgentAIAgentTypes` built-in packs and their hand-seeded rule tables, per Epic 4's original scope and ARCHITECTURE-SPINE.md's amended AD-1/AD-2) is deferred out of Story 4.1 into a follow-up story.
  evidence: Story 4.1's spec came out over the 900–1600 token SCOPE STANDARD target (~2000+ tokens) because it bundled two separable goals: the composable pack-API redesign for the existing browser/engine/os/device categories (a redesign of what already ships), and net-new bot/AI-agent detection (an entirely new capability with its own hand-seeded data source). Split at the human's explicit direction ("give me small fast solution... then extend to more abilities") — Story 4.1 now covers only the pack-API redesign; this entry tracks the deferred bot/AI-agent pack work, which still needs its own scoped spec before implementation. `UserAgentInfo`'s `bot`/`aiAgent` fields and the `UserAgentTypePack` extensibility contract from Story 4.1 should make this a comparatively small follow-up once picked up.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: The new pack-API's tree-shaking property (the whole point of Story 4.1) is verified only by manual, one-off `npm pack` + esbuild/Terser experiments recorded in code comments and the spec's Verification section — no automated test or CI step asserts that an unused built-in pack stays excluded from a real JS bundle.
  evidence: Review-surfaced (verification-gap layer). A future change (e.g. merging the per-pack generated files back together, or adding an import that couples pack initialization) would silently regress the exact property this story exists to deliver, with nothing in the repo catching it. Worth a lightweight bundle-size assertion script once the pack API stabilizes.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: No CI step builds `iosApp` (`xcodebuild`) or `webApp` (`npm run build`/`tsc`) — `.github/workflows/ci.yml` only runs `./gradlew build`, which covers `jvmApp`/`androidApp` but not the Xcode project or the npm workspace, so neither sample app's compile/type-check status is verified by CI at all, before or after this story.
  evidence: Review-surfaced (verification-gap layer): confirmed by inspecting `.github/workflows/ci.yml`, `settings.gradle.kts` (only includes `:androidApp`, `:library`, `:jvmApp`), and the repo layout. Pre-existing gap, not introduced by this story, but it's why `iosApp`/`webApp` breaking against the new API (Story 4.2's known, accepted interim state) doesn't show up as a CI failure the way `jvmApp`/`androidApp` would have.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: `generateUserAgentRules`'s `outputDir.deleteRecursively()` (added when the codegen task was split to emit one file per pack instead of one shared file) ignores the method's boolean return value, so a partial/failed deletion (e.g. a locked file) would silently leave a stale generated file alongside the newly written ones instead of failing the build loudly.
  evidence: Review-surfaced (blind-hunter/edge-case-hunter layers). Not demonstrated as a live failure (deletion succeeds in every observed run), but a `check(outputDir.deleteRecursively()) { "..." }` would fail loudly instead of risking a stale-file duplicate-declaration compile error on some future run.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: `UserAgentInfo`'s data class constructor gained `bot`/`aiAgent`/`custom` fields with defaults, but the class has no `@JvmOverloads`, so JVM/Android Java callers (this library publishes to Maven Central for those targets) still can't omit the new trailing defaulted parameters — they must pass all fields explicitly, same as before the story, rather than gaining a shorter overload.
  evidence: Review-surfaced (blind-hunter layer). Not a break (existing Java call sites, if any, are unaffected since the required-field prefix is unchanged), just a missed ergonomic improvement now that defaults exist; a one-line `@JvmOverloads` addition would fix it whenever a Java-consumer story picks it up.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: `UserAgentTypePack` (the new public custom-pack extensibility type) has no `equals`/`hashCode`/`toString` overrides, and nothing detects or warns when two packs passed to the same `UserAgentParser(...)`/`UserAgentGenerator(...)` call share an `id` — the documented "first pack wins" merge behavior then depends on argument order with no diagnostic if that's accidental.
  evidence: Review-surfaced (blind-hunter layer). Not required by this story's spec (which only requires a documented, working custom-pack contract, both satisfied), but worth adding once real consumer usage shows duplicate-id mistakes are actually happening.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md`
  summary: `UserAgentParser`'s per-pack `catch (_: Throwable) { null }` (extending the pre-existing broad-catch pattern already tracked from Story 1.1 into the new pack-composition loop) makes a pack that throws indistinguishable from one that legitimately found nothing, with no logging/telemetry hook — a consumer can't tell a third-party or built-in pack silently failed versus correctly returned no match.
  evidence: Review-surfaced (blind-hunter layer), extending the already-tracked Story 1.1 entry (broad `Throwable` catch swallowing `OutOfMemoryError`/`StackOverflowError`) into the new per-pack composition code path this story introduces. Intentional per this story's "never throws" contract; worth a debug-only diagnostic hook if silent pack failures ever turn out to be a real support burden.

- source_spec: none
  summary: `library/build.gradle.kts`'s `stagePublishJsPackage` task (pre-existing, uncommitted work already in the working tree before Story 4.1 started, unrelated to it and explicitly left untouched per that spec's Boundaries) has three real bugs, surfaced incidentally because the review diff included all of `library/build.gradle.kts`: (1) its `args(...)` list never actually includes `"stage"`, so despite the task's name/description/comments describing a two-phase stage-then-approve flow, it performs an immediate live `npm publish --access=public` instead; (2) its own doc comment says the bundled npm (11.6.1) doesn't support `stage` and that `npx` should fetch a current one, but the code never uses `npx` — it resolves the same insufficient bundled binary; (3) `File(npmBinFile.parentFile, "npm")` hardcodes the bare Unix executable name with no `npm.cmd`/`npm.ps1` handling for Windows.
  evidence: Review-surfaced (blind-hunter/edge-case-hunter layers) against code that predates this story's `baseline_commit` and was already uncommitted in the working tree at session start. Needs attention from whoever owns that task before it's ever actually run — as written it would either fail outright (missing `stage` subcommand support) or silently skip the staged-approval safety property its own comments describe.
