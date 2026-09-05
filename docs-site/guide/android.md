# Android Usage

`site.lempert:user-agent` is the Android build of `kmp-user-agent`, published
to Maven Central straight from the library's own Kotlin/Android target -- the
same detection/generation logic used by every other platform.

## Install

Add the dependency to your module's `build.gradle.kts` (Maven Central is
already in most projects' default repositories):

```kotlin
dependencies {
    implementation("site.lempert:user-agent:0.2.0")
}
```

> The `0.2.0` above is a hand-pinned version, not something Gradle resolves
> for you -- bump it whenever you upgrade to a newer release, checking the
> [Maven Central listing](https://central.sonatype.com/artifact/site.lempert/user-agent)
> for the latest published version.

No manifest changes or runtime permissions are required -- parsing and
generating are pure string logic. The library targets `minSdk = 24` and
above, so it works on any Android version your app already supports if your
own `minSdk` is 24 or higher. If your app's `minSdk` is lower than 24, AGP
will fail the build with an error like `uses-sdk:minSdkVersion N cannot be
smaller than version 24 declared in library`; the fix is to raise your app's
`minSdk` to 24.

## Parse

Kotlin's vararg support means you call `UserAgentParser`/`UserAgentGenerator`
directly with the packs you want -- no `.get()` or array-wrapping, unlike the
JS bindings.

A real UA string to parse is available without any permission via
`System.getProperty("http.agent")`, which returns the device's default
User-Agent. This property can return `null` in some environments (unit
tests, some non-standard JVMs), so the `?: ""` fallback below is a
deliberate guard, not an arbitrary default:

```kotlin
import site.lempert.useragent.UserAgentAllTypes
import site.lempert.useragent.UserAgentParser

val userAgent = System.getProperty("http.agent") ?: ""

val info = UserAgentParser(UserAgentAllTypes)(userAgent)

println(info.browser) // e.g. Component(name=Chrome, version=128.0)
println(info.engine)
println(info.os)
println(info.device)
println(info.bot)
println(info.aiAgent)
```

The function returned by `UserAgentParser(...)` (and likewise
`UserAgentGenerator(...)`) is cheap to construct, but it's still worth
building once and reusing it across calls rather than rebuilding it every
time -- the same way the repo's own `androidApp` sample caches it with
Compose's `remember { }`. See Core Concepts'
["Cost of calling the factories"](./core-concepts#cost-of-calling-the-factories)
for details.

## Generate

```kotlin
import site.lempert.useragent.Component
import site.lempert.useragent.UserAgentAllTypes
import site.lempert.useragent.UserAgentGenerator
import site.lempert.useragent.UserAgentInfo

val userAgentString = UserAgentGenerator(UserAgentAllTypes)(
    UserAgentInfo(
        browser = Component("Chrome", "128.0"),
        engine = Component("Blink", "128.0"),
        os = Component("Android", "14"),
        device = null,
    ),
)

println(userAgentString)
```

## Next steps

For the platform-agnostic `UserAgentInfo` model and the built-in type packs
behind `UserAgentAllTypes` used above, see [Core Concepts](./core-concepts).
