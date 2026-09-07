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
  evidence: Story 4.1's spec came out over the 900–1600 token SCOPE STANDARD target (~2000+ tokens) because it bundled two separable goals: the composable pack-API redesign for the existing browser/engine/os/device categories (a redesign of what already ships), and net-new bot/AI-agent detection (an entirely new capability with its own hand-seeded data source). Split at the human's explicit direction ("give me small fast solution... then extend to more abilities") — Story 4.1 now covers only the pack-API redesign; this entry tracks the deferred bot/AI-agent pack work, which still needs its own scoped spec before implementation. `UserAgentInfo`'s `bot`/`aiAgent` fields and the `UserAgentTypePack` extensibility contract from Story 4.1 should make this a comparatively small follow-up once picked up. **Update, 2026-09-04:** picked up as Story 4.3 (`epics.md`); rule data source resolved — hand-authored from each bot/crawler operator's own public documentation (Google, Microsoft, OpenAI, Anthropic, Perplexity, Common Crawl, etc.), never copied from DataDome's proprietary commercial dataset the human initially pointed at (confirmed incompatible with AD-6/NFR1's permissive-license-only constraint; the human agreed to use it only as a category/name checklist, not a data source).

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

- source_spec: `_bmad-output/implementation-artifacts/spec-4-3-add-bot-and-ai-agent-detection-packs.md`
  summary: `UserAgentBotTypePack.kt`'s `Slackbot` rule only matches the `Slackbot-LinkExpanding` token; Slack's own docs also describe a plain `Slackbot 1.0 (+https://api.slack.com/robots)` form used in other contexts, which this rule doesn't match at all, and even the matched form's `versionMode` is hardcoded `NONE` even though that plain form does carry a version — worth re-verifying against Slack's current docs and either adding a second rule or capturing the version.
  evidence: Review-surfaced (blind-hunter layer). Consistent with this story's "small, explicitly non-exhaustive" scope (AD-1) — not a regression, just incomplete coverage for one entry worth revisiting if Slack detection accuracy matters to a consumer.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-3-add-bot-and-ai-agent-detection-packs.md`
  summary: `generateBotSegment`/`generateAiAgentSegment` don't sanitize a hand-constructed `Component.version` containing non-`[0-9.]` characters (would produce a token that fails to round-trip), and a `NONE`-versionMode rule (Bytespider/StatusCake/Slackbot/ClaudeBot/Claude-User/Claude-SearchBot) silently discards any caller-supplied version rather than surfacing it.
  evidence: Review-surfaced (edge-case-hunter layer). Only reachable via a directly hand-constructed `UserAgentInfo`/`Component` — real `detectBot`/`detectAiAgent` output never produces a non-numeric version (the regexes only capture `[0-9.]+`) and never produces a version at all for `NONE`-mode rules (no capture group exists). Same trust-boundary/low-priority category as the already-tracked Story 2.1 entry on `browser.version`/`engine.version` sanitization — extends that same pattern to the new bot/AI-agent packs rather than introducing a new one.

- source_spec: none
  summary: `library/build.gradle.kts`'s `stagePublishJsPackage` task (pre-existing, uncommitted work already in the working tree before Story 4.1 started, unrelated to it and explicitly left untouched per that spec's Boundaries) has three real bugs, surfaced incidentally because the review diff included all of `library/build.gradle.kts`: (1) its `args(...)` list never actually includes `"stage"`, so despite the task's name/description/comments describing a two-phase stage-then-approve flow, it performs an immediate live `npm publish --access=public` instead; (2) its own doc comment says the bundled npm (11.6.1) doesn't support `stage` and that `npx` should fetch a current one, but the code never uses `npx` — it resolves the same insufficient bundled binary; (3) `File(npmBinFile.parentFile, "npm")` hardcodes the bare Unix executable name with no `npm.cmd`/`npm.ps1` handling for Windows.
  evidence: Review-surfaced (blind-hunter/edge-case-hunter layers) against code that predates this story's `baseline_commit` and was already uncommitted in the working tree at session start. Needs attention from whoever owns that task before it's ever actually run — as written it would either fail outright (missing `stage` subcommand support) or silently skip the staged-approval safety property its own comments describe.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-4-expand-bot-and-ai-agent-rosters.md`
  summary: Several `NONE`-versionMode entries added in this story (`Twitterbot`, `LinkedInBot`, `Discordbot`, `PetalBot`, `Diffbot`, `ImagesiftBot`, `AdsBot-Google`, `GoogleOther`, `YandexAdditionalBot`, `Google-CloudVertexBot`) use a bare-token regex with no capture group at all, even though a couple of their own test fixtures include a plausible version number in the UA string (e.g. `"Discordbot/2.0"`, `"Diffbot/0.1"`) that then gets silently discarded (asserted as `null`). Worth a follow-up pass re-verifying each against its operator's own docs to confirm whether a version token is actually real and documented (upgrade to `REQUIRED`/`OPTIONAL` if so) or genuinely undocumented (in which case the test fixtures should drop the misleading version suffix rather than imply one exists).
  evidence: Review-surfaced (blind-hunter layer). Consistent with the frozen spec's own "(no version)" categorization for these entries (a decision made when the spec was written, not introduced by the implementer) — not a regression, just an opportunity for more precise data once someone has time to re-verify per-entry rather than guess.

- source_spec: `_bmad-output/implementation-artifacts/spec-4-4-expand-bot-and-ai-agent-rosters.md`
  summary: `AdsBot-Google`'s and `GoogleOther`'s bare regexes (`Regex("AdsBot-Google")`, `Regex("GoogleOther")`) also match Google's distinct `AdsBot-Google-Mobile` and `GoogleOther-Image`/`GoogleOther-Video` crawler variants, collapsing them into the same `Component` rather than distinguishing the variant.
  evidence: Review-surfaced (edge-case-hunter layer). Not incorrect (the collapsed variants are still genuinely Google crawlers of the stated family), just less granular than possible; worth adding negative-lookahead exclusions or separate entries for the variants if that granularity ever matters to a consumer.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-1-walking-skeleton-site-live-with-browser-node-js-guide.md`
  summary: The JS guide's "Next steps" section mentions passing a narrower pack (e.g. `UserAgentBrowserTypes`) instead of `UserAgentAllTypes` to keep a bundle smaller, but no code example in the guide actually demonstrates it — every sample uses `UserAgentAllTypes`.
  evidence: Review-surfaced (blind-hunter layer). Not incorrect, just under-demonstrated; a natural addition once the guide gets a revision pass, not required for this story's walking-skeleton scope.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-1-walking-skeleton-site-live-with-browser-node-js-guide.md`
  summary: The deployed site's VitePress theme configures none of `editLink`, `lastUpdated`, `logo`, or `head` entries for favicon/Open Graph social-preview metadata, and the build generates no `sitemap.xml`/`robots.txt`.
  evidence: Review-surfaced (blind-hunter layer). Cosmetic/SEO polish appropriate once more content exists (post Stories 5.2-5.7); out of scope for a walking-skeleton story whose only content is one guide page.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-1-walking-skeleton-site-live-with-browser-node-js-guide.md`
  summary: `docs-site/package.json` has no `engines` field constraining the Node version for local docs development/contribution, even though the shipped guide content is precise about Node version cutoffs for library consumers.
  evidence: Review-surfaced (blind-hunter layer). Low-priority DX nicety; GitHub Actions pins the CI runtime explicitly regardless, so this only affects local contributor consistency.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-1-walking-skeleton-site-live-with-browser-node-js-guide.md`
  summary: `docs-site/guide/js.md`'s "full pack list and API reference" link points to a `#api` anchor in the root `README.md`, a cross-file reference with no automated link-check in CI -- if that heading is ever renamed, the link breaks silently.
  evidence: Review-surfaced (blind-hunter layer). Low-priority robustness gap; overlaps with the already-accepted "no pre-merge CI/lint" non-goal for this epic's v1 (see ARCHITECTURE-SPINE.md Deferred), so not worth a dedicated fix in isolation.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-2-core-concepts-guide.md`
  summary: The root `README.md`'s "API" section and the new `docs-site/guide/core-concepts.md` now independently explain the same pack-composition/merge-semantics/custom-pack content, with no cross-link either direction -- two copies likely to drift out of sync on a future edit to either one.
  evidence: Review-surfaced (blind-hunter layer). Fixing this cleanly means either cross-linking or having one summarize-and-point-to the other, which touches `README.md` -- outside this story's `docs-site/` scope and not something to do without explicit direction.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-2-core-concepts-guide.md`
  summary: `core-concepts.md`'s custom-pack example shows writing into `UserAgentInfo.custom` (via `detect`) but never shows a consumer reading it back out (e.g. `info.custom["myThing"]`), leaving the round-trip usage incomplete.
  evidence: Review-surfaced (blind-hunter layer). Minor completeness gap, not misleading; a natural addition on a future content pass over this guide.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: The live parse demo's Bot and AI Agent fields can never show real data with the currently pinned `@lempert/user-agent@^0.2.0` dependency -- verified by running the actual installed package against real bot/AI-crawler UA strings (`Googlebot/2.1`, `GPTBot/1.0`), both return `null`. Bot/AI-agent detection exists in this repo's source (Stories 4.3/4.4) but was never published to npm under a version `docs-site` depends on, so every visitor sees "Not detected" for those two rows regardless of their actual User-Agent.
  evidence: Review-surfaced (blind-hunter layer), independently reproduced. Not a demo code defect -- the component correctly renders whatever the real published package returns. Needs a new npm version published (a deliberate, separate action per architecture AD-2) and `docs-site/package.json`'s dependency bumped to it. Flagged directly to the human, not silently patched around.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: No TypeScript type-checking (`tsc`/`vue-tsc`) runs anywhere in the build or CI pipeline for `ParseDemo.vue` -- Vite/esbuild strips `<script setup lang="ts">` types without checking them, so a wrong import, prop type, or API misuse would silently pass CI.
  evidence: Review-surfaced (blind-hunter layer). Overlaps with the already-accepted "no pre-merge CI/lint" non-goal for this epic's v1 (ARCHITECTURE-SPINE.md Deferred); not worth a dedicated fix in isolation.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: No component-level automated test (e.g. Vitest + `@vue/test-utils`) exists for `ParseDemo.vue` -- `formatComponent`/`formatDevice` and the three template branches (loading/error/success) have no unit or e2e coverage; a formatting regression (e.g. dropping a null filter, producing `"Chrome undefined"`) would ship with a fully green CI run, since the two build-safety CI checks only inspect pre-hydration static HTML, not client-side-only rendering logic.
  evidence: Review-surfaced (verification-gap and blind-hunter layers, independently). Matches the architecture spine's already-accepted "testing convention for demo components... not fixed here" Deferred item from Story 5.1's own review -- not a new gap, a recurrence of an already-tracked one.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: The two `docs-deploy.yml` CI safety checks added in this story are coupled to `ParseDemo.vue`'s exact current copy via untied magic strings ("Detecting your browser", "Node.js/") with nothing keeping them in sync -- a future wording change to the placeholder text would silently defang the CI gate without anyone noticing until the next real regression ships.
  evidence: Review-surfaced (blind-hunter layer). Real but no clean shared-constant mechanism exists across a `.vue` file and a `.yml` workflow in this stack; better addressed if/when a real test framework is introduced for `docs-site` (see the testing-convention deferral above).

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: The crash-style CI grep (`ReferenceError|TypeError|SyntaxError|is not defined`) scans the whole build log for the whole site rather than being scoped to a specific component, and the dist-content check only inspects `docs-site/.vitepress/dist/index.html` -- neither generalizes if a future page reuses a similar client-only-global pattern elsewhere on the site.
  evidence: Review-surfaced (blind-hunter layer, across two consecutive rounds). The dist-content check (this story's actual load-bearing fix) already closes the concrete regression found; broadening further has diminishing returns and real false-positive costs already weighed once this loop.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: The live parse demo shows no indicator of which `@lempert/user-agent` version produced the result, despite the page's copy leaning on "the real, published package" as its credibility hook -- useful both for visitor trust and for reproducing a user-reported discrepancy.
  evidence: Review-surfaced (blind-hunter layer). Nice-to-have, not required by this story's acceptance criteria.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-3-live-parse-demo.md`
  summary: `docs-site/guide/js.md` documents the same `UserAgentParser([UserAgentAllTypes.get()])` call shape the live demo uses, but neither page links to the other -- a reader of the guide isn't pointed to the live demo as a working sanity check, and the demo doesn't link back to the guide for the full API.
  evidence: Review-surfaced (blind-hunter layer). Nice-to-have navigation improvement, low urgency.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-4-live-generate-demo.md`
  summary: In `GenerateDemo.vue`, the Engine dropdown has no visible effect when Browser is Chrome/Firefox/Edge (those families ignore `engine` except as a fallback that never triggers, since browser.version is always set) -- a visitor's Engine choice is silently ignored for 3 of 4 browsers with no indication. Similarly, Device is silently ignored for Windows/Mac/Linux OS choices, and only ever affects output for Android/iOS.
  evidence: Review-surfaced (blind-hunter and edge-case-hunter layers, independently). Not a crash or invalid output -- matches the already-established "respect the visitor's own choice even if the result is odd" principle (same as the Firefox+Android case) -- but worth a copy update or UI affordance (e.g. disabling/graying out inapplicable filters) on a future pass.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-4-live-generate-demo.md`
  summary: The Device dropdown in `GenerateDemo.vue` only ever shows 2 options ("Google Pixel 8", "iPad") because option derivation filters out presets with `device: null` -- "iPhone" (the `safari-iphone` preset's implicit default) is never directly selectable, only reachable via "Any (random)".
  evidence: Review-surfaced (blind-hunter layer). Minor completeness gap, not a defect; would need a sentinel "iPhone (default)" entry distinct from the existing "Any (random)" option to fix cleanly.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-4-live-generate-demo.md`
  summary: `generateSupportMatrix.ts`'s `unsafeCombinations` list is a hand-transcribed duplicate of `UserAgentGenerator.kt`'s `unsafeCombination` boolean logic, with no shared source of truth or generation step -- a future change to the real check has no forcing function to keep the TS copy in sync (beyond the file's own header comment instructing a manual re-check on version bumps).
  evidence: Review-surfaced (blind-hunter layer). Same category as the already-accepted AD-2 version-bump-and-recheck discipline; no better mechanism exists across the Kotlin/TS boundary without codegen, which is out of scope here.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-4-live-generate-demo.md`
  summary: All preset browser/OS versions in `generateSupportMatrix.ts` are fixed (always "Chrome 128.0", "Windows 10", etc.) even though the real generator accepts other non-blank versions for most families -- the demo's copy doesn't mention that version variety was deliberately left out of the curated preset table.
  evidence: Review-surfaced (blind-hunter layer). Intentional simplification for a curated, verified-safe preset set; a content nicety to mention explicitly on a future pass, not a defect.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-5-android-usage-guide.md`
  summary: `android.md` doesn't mention the required Java/JVM toolchain compatibility (the library's Android target compiles with JVM 11) or the minimum AGP/Gradle version needed to correctly resolve this KMP-published AAR's variant metadata (built via the newer `com.android.kotlin.multiplatform.library` AGP plugin) -- both plausible, if uncommon, sources of an obscure build failure for a consumer on older tooling.
  evidence: Review-surfaced (blind-hunter layer). Real but esoteric; most modern Android projects already meet both bars, and pinning an exact minimum AGP version would need separate verification work.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-5-android-usage-guide.md`
  summary: `android.md` doesn't caveat that `System.getProperty("http.agent")` reflects the platform's default `HttpURLConnection`/WebView User-Agent, not necessarily what a given HTTP client (OkHttp, Retrofit, etc.) an app actually uses will send on the wire -- a likely point of confusion for anyone trying to detect their own app's real outgoing UA.
  evidence: Review-surfaced (blind-hunter layer). Real nuance, but tangential to the library itself (an HTTP-client behavior note, not a library-usage note); worth adding on a future content pass.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-5-android-usage-guide.md`
  summary: The site nav keeps a generic "Guide" label pointing at `/guide/js` alongside the new, explicitly-named "Android" entry -- reads as an inconsistency now that multiple named platform guides exist side by side, rather than a considered nav hierarchy.
  evidence: Review-surfaced (blind-hunter layer). Best addressed once all platform guides exist (after Stories 5.6/5.7 add iOS/JVM) rather than renamed incrementally per story.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-5-android-usage-guide.md`
  summary: No "Testing" guidance for Android consumers (e.g. exercising parse/generate results in host-side unit tests) and no clarification that the guide's instructions apply equally inside a Kotlin Multiplatform project's own `androidMain` source set, not just a plain single-platform Android app module.
  evidence: Review-surfaced (blind-hunter layer). Real nice-to-haves; the guide's current generic "your module's build.gradle.kts" framing already technically covers the KMP case without change, and a testing section would be new scope beyond parity with the JS guide.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-5-android-usage-guide.md`
  summary: The install snippet doesn't remind readers that most projects already have Maven Central in their default repositories but some may not (`repositories { mavenCentral() }` needed) -- low-probability given Maven Central is the default in virtually all modern Android project templates.
  evidence: Review-surfaced (edge-case-hunter layer). Low-value boilerplate the site doesn't otherwise include elsewhere; not worth the added length for this story's guide.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-6-ios-usage-guide.md`
  summary: No CI or repo-local automation exercises the XCFramework assembly (`assembleLibraryReleaseXCFramework`) this story wires up -- `./gradlew build` (the repo's only CI gate) never invokes it, so a future regression in the `xcf.add(this)` wiring (e.g. dropping a slice, renaming the framework) could silently ship in a later release with a fully green CI run, undetected until an SPM consumer's build breaks.
  evidence: Review-surfaced (verification-gap layer), well-demonstrated: confirmed via `./gradlew :library:build --dry-run` that the XCFramework-assembly tasks aren't in the default `build` lifecycle, and grepped both workflow files for zero references. Matches this project's already-accepted "manual release process, no CI automation" pattern for Maven Central/npm, but flagged prominently here since the regression path is concretely demonstrated rather than theoretical -- worth a dedicated CI/pre-release check (inspect the assembled XCFramework's slice directories, not just trust a green build) if this becomes a recurring release rather than a one-off.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-6-ios-usage-guide.md`
  summary: No documented maintainer process exists for cutting the next SPM release (rebuild the XCFramework, re-zip, recompute checksum, edit `Package.swift`, commit before tagging, then tag/push/release) -- today it's entirely manual and undocumented, discovered ad hoc during this story.
  evidence: Review-surfaced (blind-hunter layer). Matches the established manual-publish pattern already accepted for Maven Central/npm in this project; worth writing down (e.g. in a CONTRIBUTING note) once a second real release happens, not before.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-6-ios-usage-guide.md`
  summary: The XCFramework has no `iosX64` (Intel simulator) slice -- a pre-existing limitation of `library/build.gradle.kts`'s iOS target list (predates this story), now user-visible for the first time via real SPM distribution rather than being invisible KMP-Gradle-only scope.
  evidence: Confirmed via `library/build.gradle.kts`; not caused by this story. Now documented as a caveat in `docs-site/guide/ios.md`. Adding `iosX64` would require extending the library's own MVP target list, a separate decision beyond this story's scope.

- source_spec: `_bmad-output/implementation-artifacts/spec-google-analytics-consent-mode.md`
  summary: Add a persistent "change your consent" control (e.g. a footer link) so visitors can revisit/withdraw their GA consent choice after the initial decision, plus a version/timestamp on the stored choice so a future privacy-policy or tracker change can force re-prompting returning visitors.
  evidence: Review-surfaced (blind-hunter layer). GDPR guidance expects withdrawing consent to be as easy as giving it; the current implementation only asks once and offers no way back short of manually clearing browser storage. Deliberately out of this spec's frozen scope (`Never: do not build a full CMP`), but a minimal revisit link is a smaller ask than a full CMP and worth a follow-up.

- source_spec: `_bmad-output/implementation-artifacts/spec-google-analytics-consent-mode.md`
  summary: Track VitePress's client-side SPA route changes as GA `page_view` events -- currently `gtag('config', ...)` fires once on the initial hard page load only, so in-app navigation between docs pages (VitePress's normal navigation mode) is invisible to analytics.
  evidence: Review-surfaced (blind-hunter and edge-case-hunter layers, independently). Fixing this means wiring VitePress's router `onAfterRouteChanged` hook to fire manual `page_view` events, which is a small design decision (event shape, whether to disable GA's automatic pageview) rather than a mechanical fix -- left for a follow-up rather than patched into this spec's minimal scope.

- source_spec: `_bmad-output/implementation-artifacts/spec-google-analytics-consent-mode.md`
  summary: Respect browser-level privacy opt-out signals (Global Privacy Control / Do Not Track) by treating their presence as an implicit "reject" -- skipping the banner and never granting consent -- since some jurisdictions (e.g. California CPRA) require honoring GPC automatically regardless of on-page UI.
  evidence: Review-surfaced (blind-hunter layer). Current implementation only gates on the visitor's own banner click, with no GPC/DNT check anywhere in `ConsentBanner.vue`.

- source_spec: `_bmad-output/implementation-artifacts/spec-google-analytics-consent-mode.md`
  summary: Link the consent banner's text to the site's privacy/analytics disclosure page once it exists, and document there that Consent Mode v2 runs in "Advanced" mode -- `gtag.js` loads and Google receives cookieless conversion-modeling pings even pre-consent, which is a real, undocumented tradeoff of the current setup.
  evidence: Review-surfaced (blind-hunter layer). The banner currently has no link explaining what's collected or why; this ties directly into the separate legal-disclaimers/privacy-notice feature already planned as this session's next item, which is the natural place to close this gap.

- source_spec: `_bmad-output/implementation-artifacts/spec-google-analytics-consent-mode.md`
  summary: Add cross-tab consent-choice sync (a `window` `storage` event listener in `ConsentBanner.vue`) so accepting or rejecting in one open tab updates `gtag` consent state and banner visibility in the site's other open tabs, instead of only reading the stored choice once on mount per tab.
  evidence: Review-surfaced (edge-case-hunter layer). Minor UX inconsistency (a visitor with two tabs open could see the banner in one after already accepting in the other), not a compliance risk since Consent Mode still defaults safely to denied in the un-synced tab.

- source_spec: `_bmad-output/implementation-artifacts/spec-google-analytics-consent-mode.md`
  summary: Verify (or extend) the consent-banner injection point if a future docs-site page ever opts out of the default VitePress layout via `layout: false` frontmatter, since `ConsentBanner` is currently injected only through `DefaultTheme.Layout`'s `layout-bottom` slot and such a page would never render it.
  evidence: Review-surfaced (edge-case-hunter layer). No page in the site currently sets `layout: false`, so this is latent rather than a live gap today.

- source_spec: `_bmad-output/implementation-artifacts/spec-github-release-publish-workflow.md`
  summary: Add the npm publish step (and its half of the tag-vs-version consistency check) to the GitHub Actions release workflow, once the Maven Central half exists.
  evidence: Split at the human's direction (2026-09-07) after the combined spec came out to ~3,200 tokens, roughly double the 1,600-token scope target -- Maven Central and npm are each independently shippable/reviewable publish targets sharing the same tag-triggered entry point, so the split follows the natural fault line already visible in the codebase (separate `mavenPublishing {}`/`npmPublish {}` blocks in `library/build.gradle.kts`, separate spec-3-1/spec-3-2 stories for the original one-time publishes). Do NOT wire the existing `stagePublishJsPackage` Gradle task into this workflow when picked up -- it has known, already-logged bugs (see the deferred-work.md entry beginning "library/build.gradle.kts's stagePublishJsPackage task"); use the org.jetbrains.kotlin.npm-publish plugin's own standard publish task instead (referenced in `library/build.gradle.kts`'s `otp.set(...)` comment as `publishJsPackageToNpmjsRegistry` -- confirm the exact name via `./gradlew :library:tasks --group publishing`). Also note: unattended CI publish requires an npm "Automation"-type token (exempt from OTP/2FA) -- a token tied to interactive 2FA will fail in CI.

- source_spec: `_bmad-output/implementation-artifacts/spec-github-release-publish-workflow.md`
  summary: `release.yml`'s publish job doesn't gate on `ci.yml`'s build/test job succeeding for the same tag -- the two workflows trigger independently, so a tag pushed with failing tests could still reach the Maven Central publish step.
  evidence: Review-surfaced (blind-hunter layer). Closing this cleanly needs a real design decision (a `workflow_run`-triggered gate, a reusable workflow, or duplicating the test run inside `release.yml`), not a mechanical fix -- left for a follow-up.

- source_spec: `_bmad-output/implementation-artifacts/spec-github-release-publish-workflow.md`
  summary: `release.yml` has no `workflow_dispatch` trigger, so if the publish step fails for a transient reason (network blip, Central Portal outage) after a real tag is already pushed, the only recovery path is deleting and re-pushing the tag.
  evidence: Review-surfaced (blind-hunter layer). Adding manual re-run needs its own design (what ref/version a dispatch run publishes, since `GITHUB_REF_NAME` wouldn't be a tag on a manual run) -- not a trivial addition to a workflow with live publish access.

- source_spec: `_bmad-output/implementation-artifacts/spec-github-release-publish-workflow.md`
  summary: Consider gating `release.yml`'s publish job behind a GitHub Environment (e.g. `environment: release`) with required reviewers, scoping the five signing/publish secrets to that environment instead of the whole repo.
  evidence: Review-surfaced (blind-hunter layer). This is real added safety (an approval checkpoint before secrets are used) but requires repo-side configuration (Settings -> Environments) and a decision on who the required reviewers are -- a maintainer action, not something to wire blind.

- source_spec: `_bmad-output/implementation-artifacts/spec-github-release-publish-workflow.md`
  summary: The `v*` tag trigger and the version-check step's `[0-9.]*`-only capture have no supported path for a pre-release tag (e.g. `v0.3.0-beta01`) -- it would just fail the version-check step with a generic "could not extract a version" error rather than being handled explicitly (accepted, rejected with a clear message, or routed differently).
  evidence: Review-surfaced (blind-hunter layer). Not a live bug (no pre-release version is in use today), but worth an explicit decision whenever pre-release publishing is actually wanted.

- source_spec: `_bmad-output/implementation-artifacts/spec-github-release-publish-workflow.md`
  summary: No idempotency check for re-pushing the same version tag after it was already staged (or fully released) on Maven Central -- the publish step would just fail with Central Portal's own opaque error rather than a clear repo-side message explaining the tag was already published.
  evidence: Review-surfaced (edge-case-hunter layer). Central Portal itself rejects re-publishing an already-released version, so this is a UX/clarity gap rather than a correctness risk -- worth a friendlier pre-check if it turns out to bite a real release attempt. **Update (spec-npm-release-publish.md review):** the same gap now also applies to npm, which likewise rejects republishing an existing version with its own opaque error.

- source_spec: `_bmad-output/implementation-artifacts/spec-npm-release-publish.md`
  summary: `release.yml`'s publish job still doesn't gate on `ci.yml`'s build/test job succeeding for the same tag (already tracked against the Maven-only version of this workflow) -- now that npm publishing has been added to the same job, an untested tag can ship to two registries instead of one before this gap is closed.
  evidence: Review-surfaced (blind-hunter layer). Same root cause and fix shape as the existing entry; noting the increased blast radius here rather than duplicating the entry.

- source_spec: `_bmad-output/implementation-artifacts/spec-npm-release-publish.md`
  summary: No `workflow_dispatch`/manual re-run trigger (already tracked) -- now more painful with two live, network-dependent publish steps in the same job: a transient failure on either one after the other has already succeeded leaves "delete and re-push the tag" as the only recovery path, with no guidance for safely retrying just the failed half.
  evidence: Review-surfaced (blind-hunter and edge-case-hunter layers). Needs a real design decision (partial-release detection, a targeted re-run mechanism) beyond a mechanical fix.

- source_spec: `_bmad-output/implementation-artifacts/spec-npm-release-publish.md`
  summary: No pre-release-tag support (e.g. `v0.3.0-beta01`) in the version-check regex (already tracked against the Maven-only check) -- the same `[0-9.]*`-only capture now also silently applies to the new npm version check.
  evidence: Review-surfaced (blind-hunter layer). Not a live issue (no pre-release version in use today); noting the npm check inherited the same limitation rather than tracking it twice.

- source_spec: `_bmad-output/implementation-artifacts/spec-npm-release-publish.md`
  summary: Consider whether the release workflow's growing secret surface (now 6 secrets across two live package-registry publishes in one job) makes the already-tracked "gate behind a GitHub Environment with required reviewers" recommendation more worth doing sooner rather than later.
  evidence: Review-surfaced (blind-hunter layer). Same recommendation already logged against the Maven-only workflow; flagging increased urgency rather than duplicating the entry.

- source_spec: `_bmad-output/implementation-artifacts/spec-ios-release-automation.md`
  summary: The release job now runs Maven Central + npm publishing (irreversible-ish, immediately live-adjacent) and the iOS checksum-verify + GitHub Release creation (safe to retry) as sequential steps in one job, with `permissions: contents: write` scoped at the job level (GitHub Actions doesn't support step-level permissions in this job syntax) even though only the iOS steps need write access. If the iOS checksum step fails, Maven/npm have already published with no separate signal beyond one red job, and there's no documented recovery path for that partial-release state (tags are immutable, so fixing `Package.swift` requires a new tag, which Maven Central/npm would likely reject as a duplicate version of what already published).
  evidence: Review-surfaced (blind-hunter and verification-gap layers). Splitting into separate jobs (or adding a partial-release notification/summary step) is a real design decision, not a mechanical fix -- same category as the already-tracked "gate behind a GitHub Environment" and "no workflow_dispatch for partial retry" entries above.

- source_spec: `_bmad-output/implementation-artifacts/spec-ios-release-automation.md`
  summary: `runs-on: macos-latest` for the iOS checksum-verify step means the XCFramework rebuild's reproducibility depends on whatever Xcode/macOS version GitHub's default image resolves to at release time, with no explicit toolchain pin -- if the maintainer's local build environment (used to compute the checksum they commit) ever diverges from that, the safety-net step could fail closed even for a legitimate release, with no diagnosis path documented.
  evidence: Review-surfaced (blind-hunter layer). Matches this project's already-accepted floating-`macos-latest`-label convention (same tradeoff already reviewed and accepted for the Maven/npm steps in this same job); worth an explicit pin only if this actually causes a false-mismatch in practice.

- source_spec: `_bmad-output/implementation-artifacts/spec-ios-release-automation.md`
  summary: No `--prerelease` handling in the `gh release create` call for a pre-release tag (e.g. `v0.3.0-beta.1`) -- always creates a full release. Extends the already-tracked "no pre-release tag support" gap (originally logged against the Maven/npm version-check regex) to the iOS release-creation step too.
  evidence: Review-surfaced (blind-hunter layer). Not a live issue (no pre-release version in use today); same category as the existing entry, noted here rather than duplicated.

- source_spec: `_bmad-output/implementation-artifacts/spec-ios-release-automation.md`
  summary: `docs-site/guide/ios.md`'s new prose about the `0.2.0` (no `v`) vs `v0.3.0`-going-forward tag convention isn't cross-checked against `release.yml`'s actual `tags: ['v*']` trigger pattern by any test -- if the trigger pattern is ever changed without updating this doc, a maintainer following the doc's stated convention could push a tag that silently never triggers the release automation.
  evidence: Review-surfaced (verification-gap layer). Explicitly assessed as low priority there given how rarely a tag trigger pattern changes once established; a cheap grep-based cross-check would close it if it's ever worth the effort.

- source_spec: `_bmad-output/implementation-artifacts/spec-legal-disclaimers-page.md`
  summary: `docs-site/legal.md`'s privacy section doesn't state data retention/erasure guidance for already-collected GA4 analytics data (how long Google retains it, what happens if a visitor withdraws consent after previously accepting) -- specific retention periods depend on the actual GA4 property's admin-panel settings, which this session never configured or inspected.
  evidence: Review-surfaced (blind-hunter layer). Asserting a specific retention period without verifying the real GA4 property's configuration would risk stating something false; the maintainer should check Google Analytics' own Admin > Data Settings > Data Retention for this property and add an accurate statement once known.

- source_spec: `_bmad-output/implementation-artifacts/spec-legal-disclaimers-page.md`
  summary: `docs-site/legal.md`'s privacy section describes the consent-gating mechanism but not GA4's actual cookie-level details (cookie names like `_ga`/`_ga_<container-id>`, their duration, first-party status) once a visitor accepts.
  evidence: Review-surfaced (blind-hunter layer). This session never ran a real browser against the live, deployed GA4 property with a real measurement ID actively collecting, so the exact cookies GA4 sets couldn't be verified firsthand rather than assumed from generic GA4 documentation.

- source_spec: `_bmad-output/implementation-artifacts/spec-legal-disclaimers-page.md`
  summary: No automated check ties `legal.md`'s specific prose claims (e.g. "all four signals default denied," "Advanced Consent Mode," the localStorage-based persistence description) to the actual consent mechanism's behavior -- a future change to `consentDecision.ts` or `config.ts`'s head script (e.g. switching to Consent Mode "Basic," changing the storage key) could leave the prose factually wrong while every existing CI check (build, the two verify-*.mjs scripts) stays green.
  evidence: Review-surfaced (verification-gap layer, "Other findings"). Harder to close than the link/anchor-validation gaps patched in this same review round -- asserting text-content correctness via automated test is unusual and would need careful scoping (e.g. asserting specific keywords/values appear in both places) to avoid being either too weak or too brittle; left for a follow-up rather than rushed into this round's patches.

- source_spec: `_bmad-output/implementation-artifacts/spec-platform-logos.md`
  summary: The "React Native" homepage card and sidebar entry use Simple Icons' `react` (React) logo, since Simple Icons has no distinct "React Native" mark -- this sits a little oddly next to the site's own repeated messaging that "React Native isn't a fifth target here, it's the same JS build."
  evidence: Review-surfaced (blind-hunter layer). React Native's own official branding does use the same atom logo, so this is a reasonable real-world choice given the icon-set constraint, not a factual error -- worth reconsidering only if a distinct RN mark becomes available or the visual confusion is reported as a real point of reader confusion.

- source_spec: `_bmad-output/implementation-artifacts/spec-platform-logos.md`
  summary: Three near-identically-worded "not an endorsement/partnership" trademark disclaimers now exist independently on the homepage (the new Supported-platforms section, the pre-existing detection-showcase section) and on `license.md`, rather than one canonical statement the others point to.
  evidence: Review-surfaced (blind-hunter layer). Cosmetic consistency nit, not a factual or legal problem -- each copy is independently accurate.

- source_spec: `_bmad-output/implementation-artifacts/spec-platform-logos.md`
  summary: `license.md`'s "Next steps" paragraph lists platform guides but omits React Native (pre-existing gap, not introduced by this session's work -- the React Native guide itself predates this change).
  evidence: Review-surfaced (blind-hunter layer). Pre-existing issue surfaced incidentally by this review, not caused by the platform-logos change itself.

- source_spec: `_bmad-output/implementation-artifacts/spec-platform-logos.md`
  summary: The new homepage cards' redundant `title`/`alt` text (announcing the same platform name twice to screen readers) and the inline-style duplication across every card/sidebar-icon call site (instead of a shared CSS class) both match a pattern already established and twice-reviewed on this same page's pre-existing detection-showcase section -- fixing only the new additions would create inconsistency with the rest of the page.
  evidence: Review-surfaced (edge-case-hunter and blind-hunter layers). Worth addressing site-wide in one pass (a shared CSS class, and reconsidering whether `title`+`alt` should both carry the same text) rather than partially in just this round's new additions.

- source_spec: `_bmad-output/implementation-artifacts/spec-platform-logos.md`
  summary: Sidebar icon vertical alignment relies on a hardcoded `vertical-align:-5px` verified only in this session's desktop-viewport browser check -- not confirmed across the mobile sidebar drawer's different layout/font-size context.
  evidence: Review-surfaced (blind-hunter layer). This session's real-browser verification (via claude-in-chrome) confirmed correct rendering and active-page highlighting at desktop width; a narrow-viewport/mobile-drawer pass was not performed.

- source_spec: `_bmad-output/implementation-artifacts/spec-platform-logos.md`
  summary: No hover/focus-visible styling on the new homepage platform cards beyond the browser's default outline -- no visual affordance that they're interactive beyond the cursor changing.
  evidence: Review-surfaced (blind-hunter layer). Minor UX polish, not a correctness issue.

- source_spec: none
  summary: Add a React Native usage guide to docs-site, following the same per-platform pattern as the Android/iOS/JVM guides.
  evidence: Split from a combined "docs-site expansion" ask (2026-09-06) covering 8 distinct deliverables; this one carries its own open question -- whether the published `@lempert/user-agent` npm package actually resolves and runs under Metro/Hermes -- which needs verifying before the guide's content (or its viability) can be written, unlike the already-planned JVM guide picked as this round's first goal.

- source_spec: none
  summary: Add a docs-site snippet demonstrating a custom `UserAgentTypePack` (either a full custom pack, or extending a built-in pack with an extra entry such as a new browser name or OS).
  evidence: Split from the same combined ask; likely belongs as an addition to the existing core-concepts guide (Story 5.2) rather than a new page, but that placement decision and the example content itself are unscoped work distinct from the JVM guide.

- source_spec: none
  summary: Add a full reference page to docs-site listing every built-in browser/engine/os/device/bot/AI-agent entry across all type packs.
  evidence: Split from the same combined ask; needs a sourcing decision (generated from `UserAgentBotTypePack.kt`/`UserAgentAIAgentTypePack.kt`/the uap-core-derived rule tables vs. hand-curated) to avoid the page drifting from the actual shipped data -- unscoped, separate work from the JVM guide.

- source_spec: none
  summary: Add a license page to docs-site surfacing the repo's MIT `LICENSE` and the vendored uap-core Apache-2.0 `NOTICE` attribution.
  evidence: Split from the same combined ask; small and self-contained, but still a separate deliverable from the JVM guide with no dependency on it.

- source_spec: none
  summary: Add logos/icons for well-known OSes, browsers, bots, and AI agents the library detects, across relevant docs-site pages.
  evidence: Split from the same combined ask; carries real trademark/redistribution risk on third-party brand assets that needs resolving before implementation (the user has a standing aversion to vendoring third-party assets as a data source due to supply-chain risk -- same caution likely applies to logo assets), and is cross-cutting across multiple pages rather than a single deliverable.

- source_spec: none
  summary: Rewrite the docs-site intro/landing page for a stronger hook, clearer value prop, and better visual hierarchy.
  evidence: Split from the same combined ask; a pure copy/design pass with no new technical content, independently shippable from the JVM guide.

- source_spec: none
  summary: Add a competitor-comparison page/section (vs. other popular UA-parsing libraries) covering bundle size, parse speed, platform support, and detection coverage, to make the case for adopting this library.
  evidence: Split from the same combined ask; explicitly flagged as the highest-risk item -- the user wants it to be persuasive enough to prompt a switch, so every claim needs real, verifiable sourcing (actual benchmarks/measurements) rather than marketing-style assertions, which is a materially different kind of work (empirical measurement) than writing guide content.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-7-jvm-usage-guide.md`
  summary: `docs-site/guide/jvm.md` names Ktor/Spring/Javalin/servlets as the place to pull a real incoming User-Agent from, but shows no complete, runnable example wired into an actual request handler for any of them -- every code sample still parses a hardcoded literal string with a comment telling the reader to substitute their own.
  evidence: Review-surfaced (blind-hunter layer). A full framework-specific example is a real improvement but requires picking one (or more) specific frameworks to demonstrate -- a scope decision beyond this one-shot guide's boundary, not a trivial fix.

- source_spec: `_bmad-output/implementation-artifacts/spec-5-7-jvm-usage-guide.md`
  summary: `docs-site/guide/jvm.md` has no "Testing" section (e.g. asserting parse/generate results in JUnit/`kotlin.test`), mirroring the same already-tracked gap on `android.md` (see the Story 5.5 deferred-work entry above); the top-nav "Guide" label ambiguity that entry also predicted would need revisiting once Stories 5.6/5.7 landed was fixed directly in this round instead of deferred again.
  evidence: Review-surfaced (blind-hunter layer). Consistent with the Android guide's existing gap; worth a shared "Testing" pattern across platform guides once a testing convention for `docs-site` itself is decided (already tracked as a broader deferred item from Story 5.3's review).

- source_spec: none
  summary: `library/NOTICE`'s attribution text states the vendored uap-core files live at `vendor/uap-core/LICENSE` and `vendor/uap-core/regexes.yaml`, but `git ls-files` confirms their real paths both carry a `library/` prefix (`library/vendor/uap-core/LICENSE`, `library/vendor/uap-core/regexes.yaml`) -- the paths in `NOTICE` are stale.
  evidence: Review-surfaced (blind-hunter layer, spec-license-page.md review) while cross-checking `docs-site/license.md` against its source file verbatim. `library/NOTICE` itself is out of scope for a docs-site-only story; the docs page was written with the corrected path, but the source-of-truth file still needs a one-line fix.

- source_spec: none
  summary: `docs-site/license.md` covers only the library's own code/data licensing (MIT + vendored Apache-2.0 uap-core data); it doesn't address what license (if any) covers the docs site's own content -- the guide prose and the Vue demo components under `docs-site/src/demo/`.
  evidence: Review-surfaced (blind-hunter layer). Deliberately not answered by inventing a license claim -- that's an ownership/licensing decision for the human to make, not one to guess at on a page whose whole purpose is legal accuracy.

- source_spec: none
  summary: `docs-site/guide/core-concepts.md`'s "Extending a named field instead of `custom`" example (`acmeBrowserPack`) makes a specific claim -- that a bare `Safari/537.36` token gets matched by `UserAgentBrowserTypes`' generic Safari rule ahead of a custom pack passed second -- that depends on the exact rule order in the vendored `uap-core` table, which is regenerated by `library/build.gradle.kts`'s codegen task from a third-party file that can change on a future re-vendor. Verified correct today (both empirically, by running the exact snippet via a temporary `jvmApp` swap, and by inspecting `library/vendor/uap-core/regexes.yaml` directly), but nothing in the repo ties this doc claim to the generated table, so a future uap-core bump could silently make the worked example wrong with no test to catch it.
  evidence: Review-surfaced (blind-hunter layer, spec-custom-type-pack-snippet.md review). Adding an automated check would mean a `library/src/commonTest` test asserting a documentation example's specific behavior -- a cross-cutting addition beyond a docs-only one-shot story's boundary, not a trivial fix.

- source_spec: none
  summary: `library/build.gradle.kts`'s `coordinates("site.lempert", "user-agent", "0.2.0")` and `npmPublish`'s `version.set("0.2.0")` are still `0.2.0`, but `CHANGELOG.md` documents two releases past that (`0.3.0`: `UserAgentBotTypes`/`UserAgentAIAgentTypes` added; `## Unreleased (targeting 0.4.0)`: broadened rosters) whose code already exists in `commonMain` (confirmed: `UserAgentBotTypePack.kt`/`UserAgentAIAgentTypePack.kt` both contain the full `0.4.0`-targeted rosters right now). The version literals were never bumped to match, and per the already-tracked Story 5.3 deferred-work entry above, nothing at `0.3.0`/`0.4.0` has actually been published to Maven Central or npm -- so every consumer installing the real, currently-published `0.2.0` package gets `bot`/`aiAgent` fields that are always `null`, with no built-in pack to populate them at all.
  evidence: Review-surfaced (blind-hunter layer, spec-type-reference-page.md review), confirmed directly against `CHANGELOG.md` and the actual `commonMain` source. This isn't a docs bug -- it's a real gap between what's in the repo and what's published, which `docs-site/reference.md` and `docs-site/index.md` were rewritten to caveat honestly rather than paper over. The actual fix (cutting and publishing a real `0.3.0`/`0.4.0` release) is a deliberate, human-owned action outside any automated session's boundary, per this project's own established publish-boundary convention.

- source_spec: none
  summary: `docs-site/guide/react-native.md`'s "Bundle size and tree-shaking under Metro" caveat documents that Metro's default config doesn't tree-shake unused packs, but offers no mitigation for a size-sensitive RN app (e.g. Re.Pack, a custom Metro serializer, or manually splitting entry points) beyond stating the limitation.
  evidence: Review-surfaced (blind-hunter layer, spec-react-native-guide.md review). Deliberately not filled in with an unverified recommendation -- none of the possible mitigations were tested this session, and suggesting one without verifying it would repeat the exact mistake this session's whole verification discipline exists to avoid.

- source_spec: none
  summary: `docs-site/guide/react-native.md`'s Metro/Hermes compatibility verification ran a standalone `metro build` + real Hermes binary execution outside a full React Native app shell (no bridge, no native runtime, no on-device/simulator run) -- the guide states this scope explicitly, but no one has confirmed the package works inside an actual running RN app (Expo Go, a bare RN app on a simulator/device) end to end.
  evidence: Not a doc inaccuracy (the guide's own wording already scopes the claim correctly), but worth a real on-device/simulator smoke test if this guide's confidence level is ever questioned -- setting up a full RN app (Xcode/Android SDK/emulators) was intentionally avoided this session as unnecessarily heavy for what the verification needed to answer.
