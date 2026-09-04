# Changelog

All notable changes to this project are documented in this file.

This project is pre-1.0: per semantic versioning's pre-1.0 convention, a minor
version bump (e.g. 0.1.0 -> 0.2.0) signals a breaking change, since there is no
major version above 0 left to bump for that purpose.

## 0.2.0 - 2026-09-04

### Breaking change: pack-based factory API replaces the singleton API

The old fixed-shape singleton API is gone:

```kotlin
UserAgentParser.parse(userAgentString)
UserAgentGenerator.generate(info)
```

It's replaced by two factory functions, each composed from a variadic list of
**type packs**:

```kotlin
UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo
UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String
```

Built-in packs: `UserAgentBrowserTypes`, `UserAgentEngineTypes`, `UserAgentOsTypes`,
`UserAgentDeviceTypes`, and the convenience bundle `UserAgentAllTypes`. Passing no
packs returns an always-empty result (all `UserAgentInfo` fields `null`, or the bare
`"Mozilla/5.0"` base string) -- there is **no** implicit fallback to
`UserAgentAllTypes`. This lets a JS/npm consumer who only wants browser detection
import just `UserAgentBrowserTypes` and have a bundler tree-shake out the
OS/engine/device rule tables entirely.

Consumers can also author their own `UserAgentTypePack` to add detection or
generation categories without forking the library.

`UserAgentInfo` gains two new fields, `bot: Component?` and `aiAgent: Component?`,
reserved for a future release's bot/AI-agent detection packs -- both are always
`null` in this release, since no built-in pack populates them yet.

See the README's ["Migrating from 0.1.0"](./README.md#migrating-from-010) section
for the old-call -> new-call mapping for both parse and generate.

## 0.1.0 - 2026-09-02

Initial release: `UserAgentParser.parse(String)` / `UserAgentGenerator.generate(UserAgentInfo)`
singleton API, covering browser/engine/OS/device detection and generation across
Android, iOS, JVM, and JS.
