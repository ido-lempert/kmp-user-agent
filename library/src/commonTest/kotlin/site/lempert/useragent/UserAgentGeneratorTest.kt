package site.lempert.useragent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserAgentGeneratorTest {

    private val parse = UserAgentParser(UserAgentAllTypes)
    private val generate = UserAgentGenerator(UserAgentAllTypes)

    @Test
    fun chromeRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = null,
            device = null,
        )

        val ua = generate(info)
        val parsed = parse(ua)

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine?.name, parsed.engine?.name)
        assertEquals(info.engine?.version, parsed.engine?.version)
    }

    @Test
    fun firefoxRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Firefox", "128.0"),
            engine = Component("Gecko", "128.0"),
            os = null,
            device = null,
        )

        val ua = generate(info)
        val parsed = parse(ua)

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
    }

    @Test
    fun safariRoundTripsWithIndependentBrowserAndEngineVersions() {
        val info = UserAgentInfo(
            browser = Component("Safari", "17.5"),
            engine = Component("WebKit", "605.1.15"),
            os = null,
            device = null,
        )

        val ua = generate(info)
        val parsed = parse(ua)

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
    }

    @Test
    fun edgeRoundTripsWithIndependentBrowserAndEngineVersions() {
        val info = UserAgentInfo(
            browser = Component("Edge", "128.0"),
            engine = Component("Blink", "127.0"),
            os = null,
            device = null,
        )

        val ua = generate(info)
        val parsed = parse(ua)

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine?.name, parsed.engine?.name)
        assertEquals(info.engine?.version, parsed.engine?.version)
    }

    @Test
    fun bothNullYieldsBaseStringWithNoException() {
        val info = UserAgentInfo(browser = null, engine = null, os = null, device = null)

        val ua = generate(info)

        assertEquals("Mozilla/5.0", ua)
    }

    @Test
    fun unrecognizedFamilyFallsThroughToBaseString() {
        val info = UserAgentInfo(
            browser = Component("SomeNicheBrowser", "1.0"),
            engine = null,
            os = null,
            device = null,
        )

        val ua = generate(info)

        assertEquals("Mozilla/5.0", ua)

        val parsed = parse(ua)
        assertNull(parsed.browser)
    }

    @Test
    fun chromeFallsBackToMatchingEngineVersionWhenBrowserVersionIsNull() {
        val info = UserAgentInfo(
            browser = Component("Chrome", null),
            engine = Component("Blink", "128.0"),
            os = null,
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(Component("Chrome", "128.0"), parsed.browser)
        assertEquals(Component("Blink", "128.0"), parsed.engine)
    }

    @Test
    fun firefoxFallsBackToMatchingEngineVersionWhenBrowserVersionIsNull() {
        val info = UserAgentInfo(
            browser = Component("Firefox", null),
            engine = Component("Gecko", "128.0"),
            os = null,
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(Component("Firefox", "128.0"), parsed.browser)
        assertEquals(Component("Gecko", "128.0"), parsed.engine)
    }

    @Test
    fun chromeNeverBorrowsAMismatchedEngineFamilysVersion() {
        // browser.version is null/blank and the supplied engine is NOT Blink --
        // must not fabricate a "Chrome" token using Gecko's version. Falls
        // through to the engine-only block instead, surfacing just the engine.
        val info = UserAgentInfo(
            browser = Component("Chrome", ""),
            engine = Component("Gecko", "91.0"),
            os = null,
            device = null,
        )

        val parsed = parse(generate(info))

        assertNull(parsed.browser)
        assertEquals(Component("Gecko", "91.0"), parsed.engine)
    }

    @Test
    fun safariFallsBackToBrowserVersionWhenEngineIsNull() {
        val info = UserAgentInfo(
            browser = Component("Safari", "17.5"),
            engine = null,
            os = null,
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(Component("Safari", "17.5"), parsed.browser)
        assertEquals(Component("WebKit", "17.5"), parsed.engine)
    }

    @Test
    fun edgeFallsBackToBrowserVersionWhenEngineIsNull() {
        val info = UserAgentInfo(
            browser = Component("Edge", "128.0"),
            engine = null,
            os = null,
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(Component("Edge", "128.0"), parsed.browser)
        assertEquals(Component("Blink", "128.0"), parsed.engine)
    }

    @Test
    fun geckoEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("Gecko", "91.0"), os = null, device = null)

        val parsed = parse(generate(info))

        assertEquals(Component("Gecko", "91.0"), parsed.engine)
    }

    @Test
    fun webKitEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("WebKit", "605.1.15"), os = null, device = null)

        val parsed = parse(generate(info))

        assertEquals(Component("WebKit", "605.1.15"), parsed.engine)
    }

    @Test
    fun tridentEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("Trident", "7.0"), os = null, device = null)

        val parsed = parse(generate(info))

        assertEquals(Component("Trident", "7.0"), parsed.engine)
    }

    @Test
    fun prestoEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("Presto", "2.12"), os = null, device = null)

        val parsed = parse(generate(info))

        assertEquals(Component("Presto", "2.12"), parsed.engine)
    }

    @Test
    fun blankVersionIsTreatedAsMissing() {
        val info = UserAgentInfo(browser = Component("Chrome", ""), engine = null, os = null, device = null)

        val ua = generate(info)

        assertEquals("Mozilla/5.0", ua)
    }

    // -----------------------------------------------------------------
    // Story 2.2: OS & device round-trips (I/O & Edge-Case Matrix)
    // -----------------------------------------------------------------

    @Test
    fun windows10OsRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = Component("Windows", "10"),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
    }

    @Test
    fun macOsRoundTripsAndDeviceRecoversIncidentally() {
        val info = UserAgentInfo(
            browser = Component("Safari", "17.5"),
            engine = Component("WebKit", "605.1.15"),
            os = Component("Mac OS X", "10.15.7"),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
        assertEquals(Device("Apple", "Mac", "Mac"), parsed.device)
    }

    @Test
    fun androidWithPixelDeviceRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "91.0"),
            engine = Component("Blink", "91.0"),
            os = Component("Android", "12"),
            device = Device("Google", "Pixel 6", "Pixel 6"),
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
        assertEquals(info.device, parsed.device)
    }

    @Test
    fun iosRoundTripsAndDeviceRecoversIncidentally() {
        // NOTE: the frozen iOS OS-token literal ("iPhone; CPU iPhone OS ...")
        // necessarily contains "iPhone", which uap-core's browser-detection
        // table matches against its iPod/iPhone/iPad-prefixed rules *ahead of*
        // the plain "Safari" rule -- so the family that round-trips here is
        // "Mobile Safari UI/WKWebView" (uap-core's real name for this exact
        // shape: an iPhone/iPad token followed by "Version/X.Y" with no
        // intervening "Mobile/xxx" token), not "Safari". This is an inherent
        // consequence of combining this story's frozen iOS OS-token format
        // with Story 2.1's unchanged Safari template -- version, engine, os,
        // and the incidental device all still round-trip correctly, which is
        // what this story's Boundaries actually govern.
        val info = UserAgentInfo(
            browser = Component("Safari", "17.5"),
            engine = Component("WebKit", "605.1.15"),
            os = Component("iOS", "17.5"),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals("Mobile Safari UI/WKWebView", parsed.browser?.name)
        assertEquals(info.browser?.version, parsed.browser?.version)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
        assertEquals(Device("Apple", "iPhone", "iPhone"), parsed.device)
    }

    @Test
    fun unsupportedWindowsVersionOmitsOsToken() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = Component("Windows", "7"),
            device = null,
        )

        val infoWithoutOs = info.copy(os = null)

        assertEquals(generate(infoWithoutOs), generate(info))

        val parsed = parse(generate(info))
        assertEquals(info.browser, parsed.browser)
        assertNull(parsed.os)
    }

    @Test
    fun nullOsAndDeviceIsIdenticalToStory21Output() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = null,
            device = null,
        )

        val ua = generate(info)

        assertEquals("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36", ua)
    }

    @Test
    fun linuxOsRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Firefox", "128.0"),
            engine = Component("Gecko", "128.0"),
            os = Component("Linux", null),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertEquals(Component("Linux", null), parsed.os)
    }

    @Test
    fun firefoxRoundTripsWithDesktopOsTokens() {
        // Firefox is safe with the three desktop OS families -- verified against
        // the real compiled browserRules table -- unlike Android/iOS below.
        for (os in listOf(Component("Windows", "10"), Component("Mac OS X", "10.15.7"), Component("Linux", null))) {
            val info = UserAgentInfo(
                browser = Component("Firefox", "128.0"),
                engine = Component("Gecko", "128.0"),
                os = os,
                device = null,
            )

            val parsed = parse(generate(info))

            assertEquals(info.browser, parsed.browser, "for os=$os")
            assertEquals(info.engine, parsed.engine, "for os=$os")
        }
    }

    @Test
    fun edgeRoundTripsWithOsToken() {
        val info = UserAgentInfo(
            browser = Component("Edge", "128.0"),
            engine = Component("Blink", "128.0"),
            os = Component("Windows", "10"),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
    }

    @Test
    fun androidWithoutDeviceRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "91.0"),
            engine = Component("Blink", "91.0"),
            os = Component("Android", "12"),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
    }

    @Test
    fun unrecognizedOsNameOmitsOsToken() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = Component("Solaris", "11"),
            device = null,
        )

        assertEquals(
            generate(info.copy(os = null)),
            generate(info),
        )
    }

    @Test
    fun iPadDeviceProducesIPadOsToken() {
        val info = UserAgentInfo(
            browser = Component("Safari", "17.5"),
            engine = Component("WebKit", "605.1.15"),
            os = Component("iOS", "17.5"),
            device = Device("Apple", "iPad", "iPad"),
        )

        val ua = generate(info)
        assertTrue(ua.contains("iPad; CPU iPad OS"), "expected an iPad token, got: $ua")

        val parsed = parse(ua)
        assertEquals(info.engine, parsed.engine)
        assertEquals(info.os, parsed.os)
    }

    @Test
    fun osTokenIsIncludedWhenBrowserIsUnrecognized() {
        // Regression test: the engine-only/base fallback previously dropped a
        // valid os token entirely whenever no recognized browser drove the
        // string -- fixed so os/device aren't silently discarded just because
        // browser wasn't populated.
        val info = UserAgentInfo(browser = null, engine = null, os = Component("Windows", "10"), device = null)

        val parsed = parse(generate(info))

        assertEquals(info.os, parsed.os)
    }

    @Test
    fun firefoxOmitsOsTokenForAndroidAndIosButStillRoundTripsBrowser() {
        // Regression test for a real collision found during review: Firefox's
        // template combined with an Android or iOS OS token causes the whole
        // string to misparse via an unrelated UserAgentParser rule (browser
        // becomes "Android", or the version is lost for iOS). generateOsToken
        // now omits the OS token for these two combinations specifically.
        for (os in listOf(Component("Android", "12"), Component("iOS", "17.5"))) {
            val info = UserAgentInfo(
                browser = Component("Firefox", "91.0"),
                engine = Component("Gecko", "91.0"),
                os = os,
                device = null,
            )

            val parsed = parse(generate(info))

            assertEquals(info.browser, parsed.browser, "for os=$os")
            assertEquals(info.engine, parsed.engine, "for os=$os")
            assertNull(parsed.os, "for os=$os")
        }
    }

    @Test
    fun safariOmitsOsTokenForAndroidButStillRoundTripsBrowser() {
        // Regression test for a real collision found during review: Safari's
        // template combined with an Android OS token causes the whole string
        // to misparse as browser "Android" via the same unrelated rule as
        // above. generateOsToken now omits the OS token for this combination.
        val info = UserAgentInfo(
            browser = Component("Safari", "17.5"),
            engine = Component("WebKit", "605.1.15"),
            os = Component("Android", "12"),
            device = null,
        )

        val parsed = parse(generate(info))

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine, parsed.engine)
        assertNull(parsed.os)
    }

    // -----------------------------------------------------------------
    // Story 4.1: composable type-pack API (I/O & Edge-Case Matrix)
    // -----------------------------------------------------------------

    @Test
    fun noPacksAlwaysReturnsTheBareBaseString() {
        // Deliberately NOT the same as UserAgentAllTypes -- see UserAgentGenerator's
        // doc comment and the spec's Spec Change Log: an implicit fallback to
        // UserAgentAllTypes here defeated JS tree-shaking for every call site,
        // not just this one, so it was dropped. Consumers who want full
        // generation must pass UserAgentAllTypes explicitly.
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = Component("Windows", "10"),
            device = null,
        )

        assertEquals("Mozilla/5.0", UserAgentGenerator()(info))
        assertNotEquals(generate(info), UserAgentGenerator()(info))
    }

    @Test
    fun subsetPackGenerateComposesInPackOrder() {
        // Only the browser+os packs are composed (no engine pack) -- the
        // browser segment alone is still enough to produce the full Chrome
        // template, matching UserAgentAllTypes's output for the same input.
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = Component("Windows", "10"),
            device = null,
        )

        val composed = UserAgentGenerator(UserAgentBrowserTypes, UserAgentOsTypes)(info)

        assertEquals(generate(info), composed)
    }

    @Test
    fun engineOnlyPackFallsBackToOsSegmentWhenEngineUnrecognized() {
        val info = UserAgentInfo(browser = null, engine = null, os = Component("Windows", "10"), device = null)

        val ua = UserAgentGenerator(UserAgentEngineTypes, UserAgentOsTypes)(info)

        assertEquals("Mozilla/5.0 (Windows 10)", ua)
    }

    @Test
    fun devicePackAloneContributesNothingToGenerate() {
        val info = UserAgentInfo(device = Device("Google", "Pixel 6", "Pixel 6"))

        val ua = UserAgentGenerator(UserAgentDeviceTypes)(info)

        assertEquals("Mozilla/5.0", ua)
    }

    @Test
    fun throwingCustomPackDegradesGracefullyAlongsideAWorkingPack() {
        // Exercises the `catch (_: Throwable) { null }` around each pack's
        // `applyToGenerate` call, previously unexercised by any test: a pack
        // that throws must not prevent a well-behaved pack composed alongside
        // it from still contributing, and must not propagate the exception.
        val throwingPack = UserAgentTypePack(
            id = "throws",
            detect = { UserAgentInfo() },
            applyToGenerate = { throw IllegalStateException("boom") },
        )
        val info = UserAgentInfo(browser = Component("Chrome", "128.0"), engine = Component("Blink", "128.0"))

        val ua = UserAgentGenerator(throwingPack, UserAgentBrowserTypes)(info)

        assertEquals(generate(info), ua)
    }
}
