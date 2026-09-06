# Core Concepts

`kmp-user-agent` exposes the same model and the same composition pattern on
every platform it ships for (Android, iOS, JVM, JS). This page explains that
model once; each platform guide only needs to show how to call it from that
platform's language bindings.

## The `UserAgentInfo` model

Parsing produces, and generating consumes, a single data shape:

```kotlin
data class UserAgentInfo(
    val browser: Component? = null,
    val engine: Component? = null,
    val os: Component? = null,
    val device: Device? = null,
    val bot: Component? = null,
    val aiAgent: Component? = null,
    val custom: Map<String, Component> = emptyMap(),
)

data class Component(
    val name: String?,
    val version: String?,
)

data class Device(
    val brand: String?,
    val model: String?,
    val name: String?,
)
```

Every field is nullable/empty by default. When a piece of data can't be
determined from the User-Agent string -- or the packs you passed simply
weren't asked to detect it -- the field is `null`, never a sentinel value
like `"unknown"`.

## `UserAgentParser`/`UserAgentGenerator`: composed from packs

Parsing and generating are both factory functions that take a variadic list
of **type packs** and return the actual parse/generate function:

```kotlin
fun UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo
fun UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String
```

Call the factory with the packs you want, then call the function it returns:

```kotlin
val parse = UserAgentParser(UserAgentAllTypes)
val info = parse(userAgentString) // UserAgentInfo(browser = ..., engine = ..., os = ..., device = ...)

val generate = UserAgentGenerator(UserAgentAllTypes)
val userAgentString = generate(info)
```

### Built-in packs

Each is individually importable, so a bundler can tree-shake out detection
categories you don't use:

* `UserAgentBrowserTypes` -- populates `UserAgentInfo.browser`
* `UserAgentEngineTypes` -- populates `UserAgentInfo.engine`
* `UserAgentOsTypes` -- populates `UserAgentInfo.os`
* `UserAgentDeviceTypes` -- populates `UserAgentInfo.device`
* `UserAgentBotTypes` -- populates `UserAgentInfo.bot`
* `UserAgentAIAgentTypes` -- populates `UserAgentInfo.aiAgent`
* `UserAgentAllTypes` -- convenience bundle of all six of the above

`UserAgentBotTypes` and `UserAgentAIAgentTypes` are intentionally
non-exhaustive starter lists, not a complete bot/AI-agent catalog -- add your
own entries via a custom `UserAgentTypePack` (see below) for anything not
covered.

You can pass a subset of packs to only populate the fields you care about:

```kotlin
val parseBrowserOnly = UserAgentParser(UserAgentBrowserTypes)
```

When more than one pack is passed, `UserAgentParser` merges their `detect`
results field-by-field: the first pack (in the order given) to produce a
non-null value for a field wins, and `UserAgentInfo.custom` entries merge by
key with the same first-pack-wins rule per key. `UserAgentGenerator` instead
tries each pack's `applyToGenerate` in order and uses the first non-null
result, falling back to the bare `"Mozilla/5.0"` base string if every pack
returns `null`. `UserAgentAllTypes`'s own `applyToGenerate` internally chains
the same fallback order -- browser, then engine, then OS, then bot, then AI
agent, then the bare base string -- so passing `UserAgentAllTypes` alone for
generate is equivalent to composing the six narrower built-in packs.

### Passing no packs means an empty result -- not full detection

`UserAgentParser()`/`UserAgentGenerator()` called with **no** packs do not
fall back to `UserAgentAllTypes`. `UserAgentParser()` returns a function that
always produces an empty `UserAgentInfo` (every field `null`/empty), and
`UserAgentGenerator()` returns a function that always produces the bare
`"Mozilla/5.0"` string. If you want full detection/generation, pass
`UserAgentAllTypes` explicitly. This is deliberate: an implicit fallback
baked into the shared factory would give every call site -- including the
narrowest single-pack ones -- a static reachability edge to every built-in
pack, defeating tree-shaking in JS builds.

## The `custom` extension point

`UserAgentTypePack` is the same public shape every built-in pack is built
from, so you can author your own without forking the library:

```kotlin
class UserAgentTypePack(
    val id: String,
    val detect: (String) -> UserAgentInfo,
    val applyToGenerate: (UserAgentInfo) -> String? = { null },
)
```

`id` is a human-readable identifier for the pack itself -- it plays no role
in merging: `UserAgentParser` never reads a pack's `id`, only the keys inside
`UserAgentInfo.custom` decide how `custom` entries merge across packs.

`detect` receives the raw User-Agent string and should return a *partially
populated* `UserAgentInfo` -- only the field(s) your pack is responsible for
set, everything else left at its default. A pack that doesn't fit any of the
named fields (`browser`/`engine`/`os`/`device`/`bot`/`aiAgent`) can instead
populate `UserAgentInfo.custom`, keyed by whatever id you choose:

```kotlin
val myPack = UserAgentTypePack(
    id = "myThing",
    detect = { userAgent -> UserAgentInfo(custom = mapOf("myThing" to Component("Found", null))) },
)

val parse = UserAgentParser(UserAgentBrowserTypes, myPack)
```

`myPack` above only defines `detect`, so it has nothing to contribute on the
generate side -- pairing it with `UserAgentGenerator` would just fall through
to `applyToGenerate`'s `{ null }` default. A pack that also wants to
contribute to generation defines `applyToGenerate` too, e.g. returning a
literal token when `info.custom["myThing"]` is set.

Read a `custom` entry back out the same way you'd read any map:

```kotlin
val info = parse(userAgentString)
val myThing = info.custom["myThing"] // Component? -- null if myPack didn't match
println(myThing?.name)
```

## Extending a named field instead of `custom`

Reach for `custom` when what you're adding doesn't fit any named field, or
when you don't need it to interact with built-in packs at all. If instead
you want to override or add detection for one of the named fields
themselves (`browser`/`engine`/`os`/`device`/`bot`/`aiAgent`) -- e.g. giving
a proper name to an in-house browser fork that a built-in pack would
otherwise misdetect -- populate that field directly instead of `custom`:

```kotlin
private val acmeBrowserRegex = Regex("AcmeBrowser/([0-9.]+)") // compiled once, not per detect() call

val acmeBrowserPack = UserAgentTypePack(
    id = "acmeBrowser",
    detect = { userAgent ->
        val match = acmeBrowserRegex.find(userAgent)
        if (match != null) {
            UserAgentInfo(browser = Component("AcmeBrowser", match.groupValues[1]))
        } else {
            UserAgentInfo() // no match -- contribute nothing, let other packs decide
        }
    },
    // No applyToGenerate given, so it defaults to `{ null }` -- same as `myPack`
    // above, this contributes nothing on the generate side.
)
```

**Pack order controls priority**, since for each field, the first pack (in
the order passed) to produce a non-null value wins -- every pack's `detect`
still runs on every call, but only one pack's result per field survives.
This applies to any named field, not just `browser`. It matters concretely
here because most WebKit/Chromium-derived browsers -- not just obscure forks
-- keep a legacy `Safari/<version>` compatibility token in their UA string,
which `UserAgentBrowserTypes`' own generic Safari rule matches on its own,
with nothing else required:

```kotlin
val userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
    "AcmeBrowser/3.1 Safari/537.36"

val customFirst = UserAgentParser(acmeBrowserPack, UserAgentBrowserTypes)(userAgent)
println(customFirst.browser) // Component(name=AcmeBrowser, version=3.1) -- acmeBrowserPack's result won

val builtInFirst = UserAgentParser(UserAgentBrowserTypes, acmeBrowserPack)(userAgent)
println(builtInFirst.browser) // Component(name=Safari, version=null) -- the built-in
// pack's generic Safari rule produced a non-null result first, so
// acmeBrowserPack's result lost even though its own detect() still ran
```

Put a custom pack **before** the built-in pack it's meant to take priority
over; put it **after** if you only want it to fill in gaps the built-in pack
leaves `null`.

`applyToGenerate` receives a fully-populated `UserAgentInfo` and may return a
complete User-Agent string built from whatever subset of fields your pack
knows how to render, or `null` to say "this pack has nothing to contribute
for this input." It defaults to `{ null }` for packs that only contribute to
parsing.

A pack that throws during `detect`/`applyToGenerate` degrades gracefully --
it simply contributes nothing for that call rather than failing the whole
`UserAgentParser`/`UserAgentGenerator` call.

## Cost of calling the factories

`UserAgentParser(...)`/`UserAgentGenerator(...)` are cheap to call: each just
closes over the packs you pass. The compiled rule tables the built-in packs
use initialize lazily, once per process, regardless of how many times you
call either factory -- so calling `UserAgentParser(UserAgentAllTypes)` once
and reusing the returned function, or calling it fresh on every parse, are
both fine; neither re-does the underlying detection setup work.

## Where to go next

Pick the platform guide for the language you're calling this from -- each
one shows the exact install steps and language bindings for that platform,
building on the model and composition pattern explained above: the
[Browser & Node.js guide](./js), the [Android guide](./android), the
[iOS guide](./ios), or the [JVM guide](./jvm).
