package site.lempert.useragent

import site.lempert.useragent.generated.browserRules
import site.lempert.useragent.generated.deviceRules
import site.lempert.useragent.generated.osRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserAgentParserTest {

    private val parse = UserAgentParser(UserAgentAllTypes)

    @Test
    fun chromeDesktop() {
        val ua =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/128.0.6613.120 Safari/537.36"

        val info = parse(ua)

        assertEquals(Component("Chrome", "128.0"), info.browser)
        assertEquals("Blink", info.engine?.name)
        assertEquals(Component("Windows", "10"), info.os)
        assertNull(info.device)
    }

    @Test
    fun firefoxDesktop() {
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

        val info = parse(ua)

        assertEquals(Component("Firefox", "128.0"), info.browser)
        assertEquals(Component("Gecko", "128.0"), info.engine)
        assertEquals(Component("Windows", "10"), info.os)
        assertNull(info.device)
    }

    @Test
    fun safariDesktop() {
        val ua =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                "Version/17.5 Safari/605.1.15"

        val info = parse(ua)

        assertEquals(Component("Safari", "17.5"), info.browser)
        assertEquals(Component("WebKit", "605.1.15"), info.engine)
        assertEquals(Component("Mac OS X", "10.15.7"), info.os)
        // Matches the catch-all `'Mac OS'` device rule (regexes.yaml ~line 6263,
        // deliberately placed last in `device_parsers`): now that device detection
        // is implemented (this story), a desktop Mac is itself a recognized device.
        assertEquals(Device(brand = "Apple", model = "Mac", name = "Mac"), info.device)
    }

    @Test
    fun edgeDesktop() {
        val ua =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/128.0.6613.120 Safari/537.36 Edg/128.0.2739.79"

        val info = parse(ua)

        assertEquals(Component("Edge", "128.0"), info.browser)
        assertEquals("Blink", info.engine?.name)
        assertEquals(Component("Windows", "10"), info.os)
        assertNull(info.device)
    }

    @Test
    fun iosSafariMobile() {
        val ua =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

        val info = parse(ua)

        assertEquals(Component("iOS", "17.5"), info.os)
    }

    @Test
    fun androidChromeMobile() {
        val ua =
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/91.0.4472.120 Mobile Safari/537.36"

        val info = parse(ua)

        assertEquals(Component("Android", "12"), info.os)
    }

    @Test
    fun linuxDesktopWithNoOsVersion() {
        val ua = "Mozilla/5.0 (X11; Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0"

        val info = parse(ua)

        assertEquals(Component("Linux", null), info.os)
    }

    @Test
    fun osVersionReplacementTemplateSubstitutesMatchGroups() {
        // Several vendored os_parsers rules use a `$N` template in
        // os_v1_replacement (e.g. regexes.yaml's bare-token Windows fallback
        // rule), which none of the other OS tests above happen to hit -- they
        // all match rules with either a null v1Replacement (falls back to a
        // captured group directly) or a hardcoded literal with no `$`.
        val info = parse("Mozilla/4.0 (compatible; MSIE 6.0; Windows XP)")

        assertEquals(Component("Windows", "XP"), info.os)
    }

    @Test
    fun unrecognizedUserAgentYieldsAllNullFields() {
        val info = parse("Definitely Not A Real Browser 000")

        assertNull(info.browser)
        assertNull(info.engine)
        assertNull(info.os)
        assertNull(info.device)
    }

    @Test
    fun emptyStringYieldsAllNullFields() {
        val info = parse("")

        assertNull(info.browser)
        assertNull(info.engine)
        assertNull(info.os)
        assertNull(info.device)
    }

    @Test
    fun familyReplacementTemplateSubstitutesMatchGroups() {
        // Exercises uap-core's `$1`/`$2`-style replacement templates (e.g.
        // `family_replacement: 'Apple $1 App'`), which none of the browser-family
        // tests above happen to hit -- Chrome/Firefox/Safari/Edge all match rules
        // using plain capture groups or a literal (non-templated) replacement.
        val info = parse("Watch4,2")

        assertEquals(Component("Apple Watch App", "4.2"), info.browser)
    }

    @Test
    fun generatedRuleTableIsNonEmptyAndEveryPatternCompilesOnThisTarget() {
        assertTrue(browserRules.isNotEmpty())
        for (rule in browserRules) {
            // Must not throw: this is the same normalized pattern set that must
            // compile identically under JVM/Native regex and JS's mandatory
            // `u`-flag ECMAScript dialect.
            Regex(rule.pattern)
        }
    }

    @Test
    fun noGeneratedRuleMatchesAnEmptyString() {
        // Guards the "empty input -> null browser/engine" contract against a
        // future vendored-data refresh accidentally introducing an
        // all-optional pattern that matches everything, including "".
        for (rule in browserRules) {
            assertTrue(
                !Regex(rule.pattern).containsMatchIn(""),
                "Rule with pattern '${rule.pattern}' unexpectedly matches an empty string",
            )
        }
    }

    @Test
    fun generatedOsRuleTableIsNonEmptyAndEveryPatternCompilesOnThisTarget() {
        assertTrue(osRules.isNotEmpty())
        for (rule in osRules) {
            // Must not throw: this is the same normalized pattern set that must
            // compile identically under JVM/Native regex and JS's mandatory
            // `u`-flag ECMAScript dialect.
            Regex(rule.pattern)
        }
    }

    @Test
    fun noGeneratedOsRuleMatchesAnEmptyString() {
        // Guards the "empty input -> null os" contract against a future
        // vendored-data refresh accidentally introducing an all-optional
        // pattern that matches everything, including "".
        for (rule in osRules) {
            assertTrue(
                !Regex(rule.pattern).containsMatchIn(""),
                "OS rule with pattern '${rule.pattern}' unexpectedly matches an empty string",
            )
        }
    }

    @Test
    fun iphoneDevice() {
        val ua =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

        val info = parse(ua)

        // First-matching device_parsers rule for this UA is the bare `(iPhone)(?:;| Simulator;)`
        // rule (regexes.yaml ~line 5730): all three replacement fields resolve to the same
        // captured token, with no positional-group fallback involved.
        assertEquals(Device(brand = "Apple", model = "iPhone", name = "iPhone"), info.device)
    }

    @Test
    fun pixelDevice() {
        val ua =
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/91.0.4472.120 Mobile Safari/537.36"

        val info = parse(ua)

        // First-matching device_parsers rule for this UA is the Google Pixel rule
        // (regexes.yaml ~line 3160): `device_replacement`/`model_replacement` both '$2'
        // (the "Pixel 6" capture group), `brand_replacement` the literal 'Google'.
        assertEquals(Device(brand = "Google", model = "Pixel 6", name = "Pixel 6"), info.device)
    }

    @Test
    fun caseInsensitiveDeviceRuleMatchesNonCanonicalCasing() {
        // Exercises a `regex_flag: 'i'` rule (the mobile-spider-crawler rule near
        // regexes.yaml:2209..2213), whose replacement fields are all hardcoded literals
        // ('Spider'/'Spider'/'Smartphone'), so the output is identical regardless of the
        // input's casing -- only whether the pattern *matches at all* depends on
        // RegexOption.IGNORE_CASE being applied for this rule.
        val canonicallyCasedInfo = parse("iPhone test something Bot-Mobile")
        val nonCanonicallyCasedInfo = parse("iphone test something bot-mobile")

        val expected = Device(brand = "Spider", model = "Smartphone", name = "Spider")
        assertEquals(expected, canonicallyCasedInfo.device)
        assertEquals(expected, nonCanonicallyCasedInfo.device)
    }

    @Test
    fun desktopBrowserHasNoDevice() {
        val ua =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/128.0.6613.120 Safari/537.36"

        val info = parse(ua)

        assertNull(info.device)
    }

    @Test
    fun deviceNameIsNullWhenDeviceReplacementIsAbsentEvenWithNoPositionalFallback() {
        // Exercises the deliberate divergence documented on detectDevice: unlike
        // browser/OS, an absent replacement field never falls back to a positional
        // capture group. This UA matches a real vendored rule (regexes.yaml's
        // "Generic_Android" Mobile-Safari rule) that gives brand_replacement/
        // model_replacement but no device_replacement, even though the pattern
        // does capture a usable group -- proving name stays null rather than
        // silently adopting that capture.
        val ua =
            "Mozilla/5.0 (Linux; Android 5.0; SM-UNKNOWN9999) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/40.0 Mobile Safari/537.36"

        val info = parse(ua)

        assertEquals(Device(brand = "Generic_Android", model = "SM-UNKNOWN9999", name = null), info.device)
    }

    @Test
    fun characterClassEscapedHyphenIsNotTreatedAsARange() {
        // Regression test for a normalizePatternForAllTargets bug found during
        // this story's review: stripping `\-` unconditionally (correct outside a
        // character class, wrong inside one) turned the vendored BIRD rule's
        // `[ \-\.]` into `[ -\.]`, an unintended ascending range covering every
        // character from space to `.` (including e.g. `!`). The fix tracks
        // character-class context so `\-` is preserved there.
        //
        // "BIRD!X100" must NOT match via the BIRD rule's widened range -- it
        // correctly falls through to a later, unrelated `device_parsers` rule
        // (a generic case-insensitive "starts with a known feature-phone
        // prefix" catch-all that happens to include "bird" as one of dozens of
        // alternatives, regexes.yaml ~line 6229) rather than reporting a
        // Bird-brand device. "BIRD-X100"/"BIRD.X100"/"BIRD X100" (the three
        // characters the class actually intends) must still match the
        // BIRD-specific rule.
        assertEquals(
            Device(brand = "Generic", model = "Feature Phone", name = "Generic Feature Phone"),
            parse("BIRD!X100").device,
        )

        val expected = Device(brand = "Bird", model = "X100", name = "Bird X100")
        assertEquals(expected, parse("BIRD X100").device)
        assertEquals(expected, parse("BIRD-X100").device)
        assertEquals(expected, parse("BIRD.X100").device)
    }

    @Test
    fun generatedDeviceRuleTableIsNonEmptyAndEveryPatternCompilesOnThisTarget() {
        assertTrue(deviceRules.isNotEmpty())
        for (rule in deviceRules) {
            // Must not throw: same normalized pattern set that must compile
            // identically under JVM/Native regex and JS's mandatory `u`-flag
            // ECMAScript dialect, now also exercising `regex_flag`-driven options.
            val options = if (rule.regexFlag == "i") setOf(RegexOption.IGNORE_CASE) else emptySet()
            Regex(rule.pattern, options)
        }
    }

    @Test
    fun noGeneratedDeviceRuleMatchesAnEmptyString() {
        // Guards the "empty input -> null device" contract against a future
        // vendored-data refresh accidentally introducing an all-optional
        // pattern that matches everything, including "".
        for (rule in deviceRules) {
            val options = if (rule.regexFlag == "i") setOf(RegexOption.IGNORE_CASE) else emptySet()
            assertTrue(
                !Regex(rule.pattern, options).containsMatchIn(""),
                "Device rule with pattern '${rule.pattern}' unexpectedly matches an empty string",
            )
        }
    }

    @Test
    fun parseNeverThrowsOnArbitraryInput() {
        val inputs = listOf(
            "\\",
            "$1$2$3",
            "a".repeat(5000),
            " ",
        )
        for (input in inputs) {
            parse(input)
        }
    }

    // -----------------------------------------------------------------
    // Story 4.1: composable type-pack API (I/O & Edge-Case Matrix)
    // -----------------------------------------------------------------

    private val chromeDesktopUa =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/128.0.6613.120 Safari/537.36"

    @Test
    fun subsetPackOnlyPopulatesItsOwnField() {
        val browserOnly = UserAgentParser(UserAgentBrowserTypes)(chromeDesktopUa)

        assertEquals(Component("Chrome", "128.0"), browserOnly.browser)
        assertNull(browserOnly.engine)
        assertNull(browserOnly.os)
        assertNull(browserOnly.device)
        assertNull(browserOnly.bot)
        assertNull(browserOnly.aiAgent)
    }

    @Test
    fun eachBuiltInSubsetPackPopulatesOnlyItsOwnField() {
        assertEquals(Component("Windows", "10"), UserAgentParser(UserAgentOsTypes)(chromeDesktopUa).os)
        assertNull(UserAgentParser(UserAgentOsTypes)(chromeDesktopUa).browser)

        assertEquals("Blink", UserAgentParser(UserAgentEngineTypes)(chromeDesktopUa).engine?.name)
        assertNull(UserAgentParser(UserAgentEngineTypes)(chromeDesktopUa).browser)

        val safariUa =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                "Version/17.5 Safari/605.1.15"
        val deviceOnly = UserAgentParser(UserAgentDeviceTypes)(safariUa)
        assertEquals(Device(brand = "Apple", model = "Mac", name = "Mac"), deviceOnly.device)
        assertNull(deviceOnly.browser)
    }

    @Test
    fun noPacksAlwaysReturnsAnEmptyResult() {
        // Deliberately NOT the same as UserAgentAllTypes -- see UserAgentParser's
        // doc comment and the spec's Spec Change Log: an implicit fallback to
        // UserAgentAllTypes here defeated JS tree-shaking for every call site,
        // not just this one, so it was dropped in favor of an always-empty
        // result. Consumers who want everything must pass UserAgentAllTypes
        // explicitly.
        val info = UserAgentParser()(chromeDesktopUa)

        assertNull(info.browser)
        assertNull(info.engine)
        assertNull(info.os)
        assertNull(info.device)
        assertNull(info.bot)
        assertNull(info.aiAgent)
        assertEquals(emptyMap(), info.custom)

        val explicitAllTypes = UserAgentParser(UserAgentAllTypes)(chromeDesktopUa)
        assertNotEquals(explicitAllTypes, info)
    }

    @Test
    fun userAgentAllTypesMatchesTodaysParseForAKnownChromeUa() {
        val info = UserAgentParser(UserAgentAllTypes)(chromeDesktopUa)

        assertEquals(Component("Chrome", "128.0"), info.browser)
        assertEquals("Blink", info.engine?.name)
        assertEquals(Component("Windows", "10"), info.os)
        assertNull(info.device)
        assertNull(info.bot)
        assertNull(info.aiAgent)
    }

    @Test
    fun customPackContributesWithoutAnyLibrarySourceChange() {
        val myCustomPack = UserAgentTypePack(
            id = "myThing",
            detect = { userAgent ->
                if ("Chrome" in userAgent) {
                    UserAgentInfo(custom = mapOf("myThing" to Component("DetectedByCustomPack", "1.0")))
                } else {
                    UserAgentInfo()
                }
            },
        )

        val info = UserAgentParser(UserAgentBrowserTypes, myCustomPack)(chromeDesktopUa)

        assertEquals(Component("Chrome", "128.0"), info.browser)
        assertEquals(Component("DetectedByCustomPack", "1.0"), info.custom["myThing"])
        assertNull(info.os)
    }

    @Test
    fun firstPackWinsOnFieldConflict() {
        val alwaysChrome = UserAgentTypePack(
            id = "always-chrome",
            detect = { UserAgentInfo(browser = Component("AlwaysChrome", "1.0")) },
        )
        val alwaysFirefox = UserAgentTypePack(
            id = "always-firefox",
            detect = { UserAgentInfo(browser = Component("AlwaysFirefox", "2.0")) },
        )

        val info = UserAgentParser(alwaysChrome, alwaysFirefox)(chromeDesktopUa)

        assertEquals(Component("AlwaysChrome", "1.0"), info.browser)
    }

    @Test
    fun throwingCustomPackDegradesGracefullyAlongsideAWorkingPack() {
        // Exercises the `catch (_: Throwable) { null }` around each pack's
        // `detect` call, previously unexercised by any test: a pack that
        // throws must not prevent a well-behaved pack composed alongside it
        // from still contributing, and must not propagate the exception.
        val throwingPack = UserAgentTypePack(
            id = "throws",
            detect = { throw IllegalStateException("boom") },
        )

        val info = UserAgentParser(throwingPack, UserAgentBrowserTypes)(chromeDesktopUa)

        assertEquals(Component("Chrome", "128.0"), info.browser)
    }
}
