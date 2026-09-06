# JVM Usage

`site.lempert:user-agent` is the plain-JVM build of `kmp-user-agent`, published
to Maven Central straight from the library's own Kotlin/JVM target -- the same
detection/generation logic used by every other platform.

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

The bare `site.lempert:user-agent` coordinate above resolves correctly for
any Gradle consumer (Gradle understands the Gradle Module Metadata that picks
the right per-target artifact for you). A plain **Maven** (`pom.xml`)
consumer -- common for Spring-based projects -- doesn't understand that
metadata and needs the JVM target's own published artifact id instead:

```xml
<dependency>
    <groupId>site.lempert</groupId>
    <artifactId>user-agent-jvm</artifactId>
    <version>0.2.0</version>
</dependency>
```

The library's JVM target compiles against `JVM_11` bytecode, so your project
needs a Java 11+ runtime to use it. On an older runtime, loading it fails
with `java.lang.UnsupportedClassVersionError` naming a class file version
higher than your JVM supports (`55` corresponds to Java 11) -- the fix is to
upgrade the runtime, not the dependency.

## Parse

Kotlin's vararg support means you call `UserAgentParser`/`UserAgentGenerator`
directly with the packs you want -- no `.get()` or array-wrapping, unlike the
JS bindings.

Unlike Android or a browser, a plain JVM process has no OS-provided "this
device's User-Agent" to read -- `System.getProperty("http.agent")` returned
`null` in a fresh run against this library's own JDK (Azul JDK 21), and is
unset by default on most JVM distributions unless something (your own code,
or a prior `HttpURLConnection` call) has already set it. If you're writing a
JVM-based HTTP server
(Ktor, Spring, Javalin, a plain servlet, ...), the User-Agent string you'll
actually want to parse is the one on each incoming request -- e.g.
`call.request.headers["User-Agent"]` in Ktor, or
`request.getHeader("User-Agent")` in a servlet-based framework:

```kotlin
import site.lempert.useragent.UserAgentAllTypes
import site.lempert.useragent.UserAgentParser

// Substitute a real incoming request's header value in a server context.
val userAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/128.0.6613.120 Safari/537.36"

val info = UserAgentParser(UserAgentAllTypes)(userAgent)

println(info.browser) // Component(name=Chrome, version=128.0)
println(info.engine)
println(info.os)
println(info.device)
println(info.bot)
println(info.aiAgent)
```

The function returned by `UserAgentParser(...)` (and likewise
`UserAgentGenerator(...)`) is cheap to construct, but it's still worth
building once -- e.g. as a top-level `val`, or a singleton bean/property in
whatever DI or application-lifecycle mechanism your server framework uses --
and reusing it across requests rather than rebuilding it per request. See
Core Concepts'
["Cost of calling the factories"](./core-concepts#cost-of-calling-the-factories)
for details. The returned function holds no mutable state of its own (only
reads the library's precompiled, immutable rule tables), so it's safe to
call concurrently from multiple request-handling threads without extra
synchronization.

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
        os = Component("Windows", "10"),
        device = null,
    ),
)

println(userAgentString)
// Mozilla/5.0 (Windows 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36
```

## Next steps

For the platform-agnostic `UserAgentInfo` model and the built-in type packs
behind `UserAgentAllTypes` used above, see [Core Concepts](./core-concepts).
