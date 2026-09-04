package site.lempert.useragent

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Shared cross-target regression corpus, run identically on every MVP target
 * (AD-5). Sourced from uap-core's own `tests/test_ua.yaml`/`test_os.yaml`/
 * `test_device.yaml` fixtures at the same pinned commit as the vendored
 * `regexes.yaml` (`73e7340c3ed8055051607b296bf46ead7aa5f19e`) -- see
 * `library/NOTICE` for attribution. Each fixture asserts only the field its
 * source file actually covers: browser fixtures assert `.browser` only, OS
 * fixtures `.os` only, device fixtures `.device` only. `engine` is
 * deliberately never asserted here -- it isn't part of uap-core's own
 * fixtures, and this corpus doesn't fabricate expectations for untested
 * fields.
 */
class UapCoreFixtureCorpusTest {

    private val parse = UserAgentParser(UserAgentAllTypes)

    // Sourced from uap-core's tests/test_ua.yaml.
    private val browserFixtures: List<Pair<String, Component?>> = listOf(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/60.0.3112.78 Safari/537.36" to Component("Chrome", "60.0"),
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/60.0.3112 Safari/537.36" to Component("Chrome", "60.0"),
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko; Google Web Preview) " +
            "Chrome/27.0 .1453 Safari/537.36." to Component("Chrome", "27.0"),
        "Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.0.5) Gecko/20060728 Firefox/1.5.0.5" to
            Component("Firefox", "1.5"),
        "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.12) Gecko/20101027 Ubuntu/10.04 (lucid) " +
            "Firefox/3.6.12" to Component("Firefox", "3.6"),
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/12.1.2 Safari/605.1.15" to Component("Safari", "12.1"),
        "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-us) AppleWebKit/418.8 (KHTML, like Gecko) " +
            "Safari/419.3" to Component("Safari", null),
        "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_5; en-us) AppleWebKit/533.18.1 (KHTML, like Gecko) " +
            "Version/5.0.2 Safari/533.18.5" to Component("Safari", "5.0"),
        "Safari/6533.18.5 CFNetwork/454.9.8 Darwin/10.4.0 (i386) (MacBookPro7,1)" to
            Component("Safari", "6533.18"),
        "Safari/7536.30.1 CFNetwork/520.5.1 Darwin/11.4.2 (i386) (MacBook3,1)" to
            Component("Safari", "7536.30"),
        "Safari/9537.71 CFNetwork/673.0.2 Darwin/13.0.1 (x86_64) (MacBookPro11,1)" to
            Component("Safari", "9537.71"),
        "Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/537.41 (KHTML, like Gecko) " +
            "Large Screen WebAppManager Safari/537.41" to Component("Safari", null),
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/42.0.2311.135 Safari/537.36 Edge/12.9600" to Component("Edge", "12.9600"),
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/42.0.2311.135 Safari/537.36 Edge/12" to Component("Edge", "12"),
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/75.0.3763.0 Safari/537.36 Edg/75.0.131.0" to Component("Edge", "75.0"),
    )

    // Sourced from uap-core's tests/test_os.yaml. Component.version here is
    // major.minor.patch (three groups), per detectOs.
    private val osFixtures: List<Pair<String, Component?>> = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "BoxNotes/1.3.0 Chrome/56.0.2924.87 Electron/1.6.8 Safari/537.36" to Component("Windows", "10"),
        "Box/1.2.93;Windows/10;Intel64 Family 6 Model 158 Stepping 9, GenuineIntel/64bit" to
            Component("Windows", "10"),
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/68.0.3440.106 Safari/537.36 CitrixChromeApp" to Component("Windows", "10"),
        "Mozilla/5.0 (Windows NT 6.3) AppleWebKit/537.36 (KHTML, like Gecko) BoxNotes/1.3.0 " +
            "Chrome/56.0.2924.87 Electron/1.6.8 Safari/537.36" to Component("Windows", "8.1"),
        "iTunes/12.7.1 (Windows; Microsoft Windows 7 Ultimate Edition Service Pack 1 (Build 7601)) " +
            "AppleWebKit/7604.3005.2001.1" to Component("Windows", "7"),
        "Mozilla/5.0+(Macintosh;+Intel+Mac+OS+X+10_11_6)+AppleWebKit/537.36+(KHTML,+like+Gecko)+" +
            "Chrome/52.0.2743.116+Safari/537.36" to Component("Mac OS X", "10.11.6"),
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_0) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "BoxNotes/1.3.0 Chrome/56.0.2924.87 Electron/1.6.8 Safari/537.36" to Component("Mac OS X", "10.13.0"),
        "MacOutlook/16.12.0.180401 (Intelx64 Mac OS X Version 10.12.6 (build 16G29))" to
            Component("Mac OS X", "10.12.6"),
        "iTunes/12.0.1 (Macintosh; OS X 10.9.2) AppleWebKit/537.74.9" to Component("Mac OS X", "10.9.2"),
        "Box Sync/4.0.7848;Darwin/10.13;i386/64bit" to Component("Mac OS X", "10.13"),
        "Mozilla/5.0 (iPad; CPU iPad OS 14_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Mobile/15E148" to Component("iOS", "14.4.1"),
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Mobile/15E148 Phantom/ios/22.06.08.44" to Component("iOS", "15.5"),
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/14.3 Mobile/15E148 DuckDuckGo/7 Safari/605.1.15" to Component("iOS", "14.3"),
        "Mozilla/5.0 (iPhone; CPU iPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Mobile/15E148 Pandora/1904.1.1" to Component("iOS", "12.2"),
        "App/0 CFNetwork/1442 Darwin/24.1.0" to Component("iOS", "18.1"),
        "Mozilla/5.0 (Linux; Android 10; SM-G970F) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/75.0.3396.81 Mobile Safari/537.36" to Component("Android", "10"),
        "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
            "Chrome/87.0.4280.141 Mobile DuckDuckGo/5 Safari/537.36" to Component("Android", "11"),
        "Mozilla/5.0 (Linux; Android 9; Pixel 2 XL Build/PPP5.180610.010; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.85 Mobile Safari/537.36" to
            Component("Android", "9"),
        "Mozilla/5.0 (Linux; Android 9; motorola one power) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/72.0.3626.96 Mobile Safari/537.36" to Component("Android", "9"),
        "Mozilla/5.0 (Linux; Android 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 " +
            "Onefootball/Android/9.10.6" to Component("Android", "7.0"),
        "Mozilla/5.0 (X11; Datanyze; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/65.0.3325.181 Safari/537.36" to Component("Linux", null),
        "Mozilla/5.0 (X11; Linux i686 (x86_64); rv:2.0b4) Gecko/20100818 Firefox/4.0b4" to
            Component("Linux", null),
        "python-requests/1.2.3 CPython/2.7.3 Linux/3.5.0-23-generic" to Component("Linux", "3.5.0"),
        "ibm-cos-sdk-java/2.3.0 Linux/4.9.0-8-amd64 Java_HotSpot(TM)_64-Bit_Server_VM/9.0.4+11/9.0.4" to
            Component("Linux", "4.9.0"),
        "ELinks (0.10.6; Linux 2.6.16-hardened-r10 i686; 80x25)" to Component("Linux", "2.6.16"),
    )

    // Sourced from uap-core's tests/test_device.yaml.
    private val deviceFixtures: List<Pair<String, Device?>> = listOf(
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Mobile/15E148 Phantom/ios/22.06.08.44" to Device("Apple", "iPhone", "iPhone"),
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/14.3 Mobile/15E148 DuckDuckGo/7 Safari/605.1.15" to Device("Apple", "iPhone", "iPhone"),
        "Mozilla/5.0 (iPhone; CPU iPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Mobile/15E148 Pandora/1904.1.1" to Device("Apple", "iPhone", "iPhone"),
        "Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, like Gecko) " +
            "Mobile/15D60 Instagram 33.0.0.11.96 (iPhone9,3; iOS 11_2_5; en_AU; en-AU; scale=2.00; " +
            "gamut=wide; 750x1334)" to Device("Apple", "iPhone9,3", "iPhone"),
        "Mozilla/5.0 (iPhone5,2; iOS 7.0.3) FreeWheelAdManager/5.8.3-r10206-201309100316;" +
            "com.vevo.iphone VEVO/5987" to Device("Apple", "iPhone5,2", "iPhone"),
        "Mozilla/5.0 (iPad; CPU OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Mobile/16D57 Pandora/1902.1" to Device("Apple", "iPad", "iPad"),
        "Mozilla/5.0 (iPad; CPU OS 10_0_2 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) " +
            "Mobile/14A456 [FBAN/FBIOS;FBAV/68.0.0.49.70;FBBV/41924288;FBRV/0;FBDV/iPad4,1;FBMD/iPad;" +
            "FBSN/iOS;FBSV/10.0.2;FBSS/2;FBCR/;FBID/tablet;FBLC/en_US;FBOP/5]" to
            Device("Apple", "iPad4,1", "iPad"),
        "Mozilla/5.0 (iPad2,1; iPad; U; CPU OS 6_1_3 like Mac OS X; de_DE) com.google.GooglePlus/23341 " +
            "(KHTML, like Gecko) Mobile/K93AP (gzip)" to Device("Apple", "iPad2,1", "iPad"),
        "Mozilla/5.0 (iPad3,1; iPad; U; CPU OS 7_0_4 like Mac OS X; de_DE) com.google.GooglePlus/29676 " +
            "(KHTML, like Gecko) Mobile/J1AP (gzip)" to Device("Apple", "iPad3,1", "iPad"),
        "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) " +
            "Mobile/11A465 [FBAN/FBIOS;FBAV/8.0.0.28.18;FBBV/1665515;FBDV/iPad2,3;FBMD/iPad;" +
            "FBSN/iPhone OS;FBSV/7.0;FBSS/1; FBCR/Verizon;FBID/tablet;FBLC/de_DE;FBOP/1]" to
            Device("Apple", "iPad2,3", "iPad"),
        "Mozilla/5.0 (Linux; Android 9; Pixel 2 XL Build/PPP5.180610.010; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.85 Mobile Safari/537.36" to
            Device("Google", "Pixel 2 XL", "Pixel 2 XL"),
        "Mozilla/5.0 (Linux; Android 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 " +
            "Onefootball/Android/9.10.6" to Device("Samsung", "SM-G930F", "Samsung SM-G930F"),
        "ABC/4.3.392 version 6.2.15, build 392 (Pixel 6 Pro; google Pixel 6 Pro; Android 12) AppleWebKit" to
            Device("Google", "Pixel 6 Pro", "Pixel 6 Pro"),
        "Mozilla/5.0 (Linux; U; en-us; KFAPWA Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) " +
            "Silk/3.6 Safari/535.19 Silk-Accelerated=true" to
            Device("Amazon", "Kindle Fire HDX 8.9\" 4G", "Kindle Fire HDX 8.9\" 4G"),
        "Mozilla/5.0 (PlayStation 4 1.75) AppleWebKit/536.26 (KHTML, like Gecko)" to
            Device("Sony", "PlayStation 4", "PlayStation 4"),
    )

    @Test
    fun browserFixturesMatchUapCoreFixtures() {
        for ((userAgent, expected) in browserFixtures) {
            assertEquals(expected, parse(userAgent).browser, "UA: $userAgent")
        }
    }

    @Test
    fun osFixturesMatchUapCoreFixtures() {
        for ((userAgent, expected) in osFixtures) {
            assertEquals(expected, parse(userAgent).os, "UA: $userAgent")
        }
    }

    @Test
    fun deviceFixturesMatchUapCoreFixtures() {
        for ((userAgent, expected) in deviceFixtures) {
            assertEquals(expected, parse(userAgent).device, "UA: $userAgent")
        }
    }
}
