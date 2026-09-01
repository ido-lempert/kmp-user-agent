- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: uap-core's `regex_flag: 'i'` (case-insensitive matching) is not read or applied anywhere in the codegen or parser.
  evidence: Confirmed zero occurrences of `regex_flag` in the vendored `user_agent_parsers` section (Story 1.1's only data source), so no currently-parsed browser rule is affected today. However 65 occurrences exist elsewhere in the same vendored `regexes.yaml` file, in the `os_parsers`/`device_parsers` sections that Stories 1.2/1.3 will vendor and parse — those stories must read and apply `regex_flag` or their rules will silently fail to match differently-cased input.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: The hand-rolled YAML parser in `library/build.gradle.kts` (`UapCoreCodegen.parseUserAgentParsers`/`extractSingleQuotedValue`) assumes every scalar is single-quoted on one line, has no fallback for double-quoted values or an inline comment after the closing quote, and does not sanity-check that a plausible number of rules were parsed.
  evidence: Not demonstrated against the currently vendored file (which is well-formed and yields 434 rules), but a future re-vendor from upstream could introduce a differently formatted line and silently produce a truncated or near-empty rule table with no build-time signal.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: `UserAgentParser` catches `Throwable` in four places (compiling rules, matching, group lookup, engine detection), which also swallows `OutOfMemoryError`/`StackOverflowError` from pathological regex backtracking instead of surfacing them.
  evidence: Intentional today per the spec's "parse() never throws" requirement, but broad `Throwable` catches mask non-recoverable JVM errors rather than just expected regex-compile/match failures.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: `UserAgentParser.parse()` has no length cap or timeout guard before running an untrusted input string through hundreds of vendored regexes sequentially.
  evidence: Not required by this story's acceptance criteria, but `parse()` is a public API intended to eventually process real HTTP `User-Agent` headers, which are attacker-controlled; uap-core-derived rule sets have a history of catastrophic-backtracking patterns.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: Version composition (`listOfNotNull(v1, v2).joinToString(".")`) doesn't special-case `v1 == null && v2 != null`, which would render `v2` alone as if it were a complete version.
  evidence: Not demonstrated against any currently vendored rule's actual capture-group ordering; theoretical edge case worth a regression test if it's ever observed in practice.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: There is no documented process or script for refreshing the vendored `vendor/uap-core/regexes.yaml` snapshot, beyond recording the pinned commit SHA in `library/NOTICE`.
  evidence: Maintaining a security/compatibility-relevant third-party dataset by hand invites silent drift between the pinned commit and the actual vendored content.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: Consider enabling Kotlin's `explicitApi()` mode on the `library` module to compiler-enforce the intended public API surface (`UserAgentInfo`, `Component`, `Device`, `UserAgentParser`) as the module grows in later stories.
  evidence: Not required for Story 1.1, which has a small, already-correct public surface, but becomes more valuable as more stories add code to `commonMain`.
