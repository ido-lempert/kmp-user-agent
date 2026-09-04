# Changelog

All notable changes to this project are documented in this file.

This project is pre-1.0: per semantic versioning's pre-1.0 convention, a minor
version bump (e.g. 0.1.0 -> 0.2.0) signals a breaking change, since there is no
major version above 0 left to bump for that purpose.

## 0.3.0 - 2026-09-04

### Added: bot and AI-agent detection packs

Two new built-in type packs populate the `bot`/`aiAgent` fields on `UserAgentInfo`
that shipped, always `null`, in 0.2.0:

* `UserAgentBotTypes` -- Googlebot, Bingbot, DuckDuckBot, YandexBot, Baiduspider,
  Bytespider, UptimeRobot, Pingdom, StatusCake, facebookexternalhit, Slackbot,
  PostmanRuntime
* `UserAgentAIAgentTypes` -- GPTBot, ChatGPT-User, OAI-SearchBot, PerplexityBot,
  Perplexity-User, ClaudeBot, Claude-User, Claude-SearchBot, CCBot

Both are small, explicitly non-exhaustive, hand-authored tables sourced only from
each bot/crawler operator's own public documentation (Google, Microsoft,
DuckDuckGo, Baidu, ByteDance, UptimeRobot, Pingdom, StatusCake, Meta, Slack,
Postman, OpenAI, Perplexity, Anthropic, Common Crawl) -- never copied from a
third-party commercial bot-detection dataset. `UserAgentAllTypes` now includes
both, so parsing a bot/AI-agent User-Agent string with it populates `bot`/
`aiAgent` alongside `browser`/`engine`/`os`/`device`.

This is purely additive -- no existing pack, field, or call site changes
behavior -- so it's a minor bump rather than the pre-1.0 breaking-change
convention described above.

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
