This is a Kotlin Multiplatform project targeting Android, iOS, JVM, and Web with a
shared `library` module that parses and generates User-Agent strings.

## Adding the dependency

Once published, the library is available on Maven Central under:

```kotlin
implementation("site.lempert:user-agent:0.2.0")
```

For JS/Node consumers, the same library is published to npm separately:

```shell
npm install @lempert/user-agent
```

## API

The library exposes two factory functions, each composed from a variadic list of
**type packs**:

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

Built-in packs, each individually importable so a bundler can tree-shake out the
ones you don't reference:

* `UserAgentBrowserTypes` -- populates `UserAgentInfo.browser`
* `UserAgentEngineTypes` -- populates `UserAgentInfo.engine`
* `UserAgentOsTypes` -- populates `UserAgentInfo.os`
* `UserAgentDeviceTypes` -- populates `UserAgentInfo.device`
* `UserAgentAllTypes` -- convenience bundle of all of the above

**Passing no packs returns an always-empty result** -- every `UserAgentInfo` field
`null` on parse, or just the bare `"Mozilla/5.0"` base string on generate. There is
no implicit fallback to `UserAgentAllTypes`; a consumer wanting full detection
passes it explicitly. This keeps a single-pack import (e.g. only
`UserAgentBrowserTypes`) tree-shakeable in a JS build -- the unused packs' rule
tables and detection code are never bundled.

You can pass a subset of packs to only populate the fields you care about:

```kotlin
val parseBrowserOnly = UserAgentParser(UserAgentBrowserTypes)
val info = parseBrowserOnly(userAgentString) // only `browser` is populated; the rest are null
```

When more than one pack is passed, `UserAgentParser` merges their `detect` results
field-by-field: the first pack (in the order given) to produce a non-null value for
a field wins, and `UserAgentInfo.custom` entries merge by key with the same
first-pack-wins rule per key. `UserAgentGenerator` instead tries each pack's
`applyToGenerate` in order and uses the first non-null result, falling back to the
bare `"Mozilla/5.0"` base string if every pack returns `null`.

### JS/TypeScript usage

The same API is published to npm. Top-level pack constants are exported as getter
objects under Kotlin/JS's `@JsExport` lowering, so call `.get()` to retrieve the
actual `UserAgentTypePack` instance, and pass packs as a plain array rather than
varargs:

```ts
import { UserAgentBrowserTypes, UserAgentParser } from '@lempert/user-agent';

const parse = UserAgentParser([UserAgentBrowserTypes.get()]);
const info = parse(userAgentString); // only `browser` is populated; the rest are null
```

You can also author your own pack -- a `UserAgentTypePack` is just an `id`, a
`detect` function, and an optional `applyToGenerate` function -- to add detection
or generation categories without forking the library:

```kotlin
val myPack = UserAgentTypePack(
    id = "myThing",
    detect = { userAgent -> UserAgentInfo(custom = mapOf("myThing" to Component("Found", null))) },
    applyToGenerate = { info -> info.custom["myThing"]?.let { "MyThing/${it.name}" } },
)
val parse = UserAgentParser(UserAgentBrowserTypes, myPack)
val generate = UserAgentGenerator(UserAgentBrowserTypes, myPack)
```

A pack that throws during `detect`/`applyToGenerate` degrades gracefully -- it
just contributes nothing for that call, and never crashes a composed
`UserAgentParser`/`UserAgentGenerator` call.

`UserAgentInfo` also has `bot`/`aiAgent` fields reserved for a future release's
bot/AI-agent detection packs -- both are always `null` today, since no built-in
pack populates them yet.

### Migrating from 0.1.0

0.1.0's singleton API is gone in 0.2.0 -- replace it with the pack-based factory
functions above, passing `UserAgentAllTypes` to match the old, all-categories
behavior:

```kotlin
// 0.1.0
val info = UserAgentParser.parse(userAgentString)
val userAgentString = UserAgentGenerator.generate(info)

// 0.2.0
val info = UserAgentParser(UserAgentAllTypes)(userAgentString)
val userAgentString = UserAgentGenerator(UserAgentAllTypes)(info)
```

`UserAgentInfo` also gained two new trailing fields in 0.2.0, `bot: Component?` and
`aiAgent: Component?` (both default to `null`, unused until a future release's
bot/AI-agent packs). This matters if you construct `UserAgentInfo` positionally
(as `webApp`'s sample does from TypeScript) or serialize/deserialize it, since the
field count/order changed.

## Project structure

* [/library](./library/src) is the multiplatform library itself -- `UserAgentParser`,
  `UserAgentGenerator`, the built-in type packs, and the shared data model live in
  [commonMain](./library/src/commonMain/kotlin), with the shared cross-target test
  corpus in [commonTest](./library/src/commonTest/kotlin). Every production source
  set depends only on the Kotlin stdlib.

* [/androidApp](./androidApp), [/iosApp](./iosApp/iosApp), [/jvmApp](./jvmApp),
  and [/webApp](./webApp) are thin per-target sample apps -- one per MVP target
  (Android, iOS, JVM, Web) -- that each depend on `:library` and call
  `UserAgentParser(UserAgentAllTypes)`/`UserAgentGenerator(UserAgentAllTypes)` to
  prove the library works as a consumed dependency. They are harnesses, not real
  app experiences.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- JVM app: `./gradlew :jvmApp:run`
- Web app:
  1. Install [Node.js](https://nodejs.org/en/download) (which includes `npm`)
  2. Build and run the web application:
     ```shell
     npm run build:shared
     npm install
     npm run start
     ```
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- All targets at once: `./gradlew :library:allTests`
- Android tests: `./gradlew :library:testAndroidHostTest`
- JVM tests: `./gradlew :library:jvmTest`
- Web tests: `./gradlew :library:jsTest`
- iOS tests: `./gradlew :library:iosSimulatorArm64Test`

CI (`.github/workflows/ci.yml`) runs `./gradlew build` on every push and pull
request, which exercises all four targets and compiles the sample apps.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
