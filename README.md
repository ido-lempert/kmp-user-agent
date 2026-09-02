This is a Kotlin Multiplatform project targeting Android, iOS, JVM, and Web with a
shared `library` module that parses and generates User-Agent strings.

* [/library](./library/src) is the multiplatform library itself -- `UserAgentParser`,
  `UserAgentGenerator`, and the shared data model live in
  [commonMain](./library/src/commonMain/kotlin), with the shared cross-target test
  corpus in [commonTest](./library/src/commonTest/kotlin). Every production source
  set depends only on the Kotlin stdlib.

* [/androidApp](./androidApp), [/iosApp](./iosApp/iosApp), [/jvmApp](./jvmApp),
  and [/webApp](./webApp) are thin per-target sample apps -- one per MVP target
  (Android, iOS, JVM, Web) -- that each depend on `:library` and call
  `UserAgentParser.parse()`/`UserAgentGenerator.generate()` to prove the library
  works as a consumed dependency. They are harnesses, not real app experiences.

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
