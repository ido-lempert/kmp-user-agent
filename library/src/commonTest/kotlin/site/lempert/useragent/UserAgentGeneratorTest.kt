package site.lempert.useragent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserAgentGeneratorTest {

    @Test
    fun chromeRoundTrips() {
        val info = UserAgentInfo(
            browser = Component("Chrome", "128.0"),
            engine = Component("Blink", "128.0"),
            os = null,
            device = null,
        )

        val ua = UserAgentGenerator.generate(info)
        val parsed = UserAgentParser.parse(ua)

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

        val ua = UserAgentGenerator.generate(info)
        val parsed = UserAgentParser.parse(ua)

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

        val ua = UserAgentGenerator.generate(info)
        val parsed = UserAgentParser.parse(ua)

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

        val ua = UserAgentGenerator.generate(info)
        val parsed = UserAgentParser.parse(ua)

        assertEquals(info.browser, parsed.browser)
        assertEquals(info.engine?.name, parsed.engine?.name)
        assertEquals(info.engine?.version, parsed.engine?.version)
    }

    @Test
    fun bothNullYieldsBaseStringWithNoException() {
        val info = UserAgentInfo(browser = null, engine = null, os = null, device = null)

        val ua = UserAgentGenerator.generate(info)

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

        val ua = UserAgentGenerator.generate(info)

        assertEquals("Mozilla/5.0", ua)

        val parsed = UserAgentParser.parse(ua)
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

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

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

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

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

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

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

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

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

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

        assertEquals(Component("Edge", "128.0"), parsed.browser)
        assertEquals(Component("Blink", "128.0"), parsed.engine)
    }

    @Test
    fun geckoEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("Gecko", "91.0"), os = null, device = null)

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

        assertEquals(Component("Gecko", "91.0"), parsed.engine)
    }

    @Test
    fun webKitEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("WebKit", "605.1.15"), os = null, device = null)

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

        assertEquals(Component("WebKit", "605.1.15"), parsed.engine)
    }

    @Test
    fun tridentEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("Trident", "7.0"), os = null, device = null)

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

        assertEquals(Component("Trident", "7.0"), parsed.engine)
    }

    @Test
    fun prestoEngineOnlyRoundTrips() {
        val info = UserAgentInfo(browser = null, engine = Component("Presto", "2.12"), os = null, device = null)

        val parsed = UserAgentParser.parse(UserAgentGenerator.generate(info))

        assertEquals(Component("Presto", "2.12"), parsed.engine)
    }

    @Test
    fun blankVersionIsTreatedAsMissing() {
        val info = UserAgentInfo(browser = Component("Chrome", ""), engine = null, os = null, device = null)

        val ua = UserAgentGenerator.generate(info)

        assertEquals("Mozilla/5.0", ua)
    }
}
